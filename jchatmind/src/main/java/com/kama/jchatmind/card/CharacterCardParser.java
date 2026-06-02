package com.kama.jchatmind.card;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SillyTavern / Chub Character Card V2 解析器。
 * 从 PNG 文件的 tEXt 块中提取 "chara" 键的 Base64 编码 JSON。
 * 同时兼容纯 JSON 格式的角色卡文件。
 *
 * <p>参考规格：
 * <a href="https://github.com/malfoyslastname/character-card-spec-v2">Character Card V2 Spec</a>
 */
@Slf4j
public class CharacterCardParser {

    private static final String CHARA_KEY = "chara";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 解析角色卡字节数组，自动识别 PNG / JSON 格式。
     *
     * @param data 文件字节数组
     * @return 结构化的角色卡数据
     */
    public static CardData parse(byte[] data) throws IOException {
        // PNG 格式：从 tEXt 块提取
        if (isPng(data)) {
            return parsePng(data);
        }
        // JSON 格式：直接反序列化
        return parseJson(data);
    }

    // ==================== PNG 解析 ====================

    private static boolean isPng(byte[] data) {
        return data.length > 8
                && data[0] == (byte) 0x89
                && data[1] == (byte) 0x50
                && data[2] == (byte) 0x4E
                && data[3] == (byte) 0x47;
    }

    @SuppressWarnings("unchecked")
    private static CardData parsePng(byte[] data) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ImageInputStream iis = ImageIO.createImageInputStream(bis);

        try {
            // 方法 A: 走 ImageIO 元数据（跨平台兼容性好）
            if (iis != null) {
                String charaBase64 = extractViaImageIO(iis);
                if (charaBase64 != null) {
                    return parseCharaJson(charaBase64);
                }
            }

            // 方法 B: 回退原始字节扫描（ImageIO 在某些 JDK 上不支持 png 插件的 tEXt 读取）
            String charaBase64 = extractViaRawScan(data);
            if (charaBase64 != null) {
                return parseCharaJson(charaBase64);
            }

            throw new IOException("PNG 文件中未找到角色卡元数据（缺少 tEXt:chara 块）");
        } finally {
            if (iis != null) {
                iis.close();
            }
            bis.close();
        }
    }

    /**
     * 方法 A：通过 javax.imageio 标准 API 读取 PNG 文本块。
     */
    private static String extractViaImageIO(ImageInputStream iis) throws IOException {
        ImageReader reader = getPngReader();
        if (reader == null) return null;

        try {
            reader.setInput(iis);
            IIOMetadata metadata = reader.getImageMetadata(0);
            if (metadata == null) return null;

            // PNG 文本节点路径：javax_imageio_png_1.0 → tEXt → tEXtEntry
            IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(
                    metadata.getNativeMetadataFormatName());
            IIOMetadataNode textNode = getChildByName(root, "tEXt");
            if (textNode == null) return null;

            List<IIOMetadataNode> entries = getChildrenByName(textNode, "tEXtEntry");
            for (IIOMetadataNode entry : entries) {
                String keyword = entry.getAttribute("keyword");
                if (CHARA_KEY.equals(keyword)) {
                    return entry.getAttribute("value");
                }
            }
        } finally {
            reader.dispose();
        }
        return null;
    }

    /**
     * 方法 B：原始字节扫描 tEXt 块。
     * PNG 块结构：[4字节长度(大端)] [4字节类型] [数据] [4字节CRC]
     */
    private static String extractViaRawScan(byte[] data) {
        int offset = 8; // 跳过 PNG 签名
        while (offset + 8 <= data.length) {
            int chunkLen = readIntBigEndian(data, offset);
            String chunkType = new String(data, offset + 4, 4);

            if ("tEXt".equals(chunkType)) {
                int dataStart = offset + 8;
                int dataEnd = dataStart + chunkLen;
                if (dataEnd <= data.length) {
                    // tEXt 格式：keyword\0value
                    String raw = new String(data, dataStart, chunkLen, java.nio.charset.StandardCharsets.ISO_8859_1);
                    int nullIdx = raw.indexOf('\0');
                    if (nullIdx > 0 && CHARA_KEY.equals(raw.substring(0, nullIdx))) {
                        return raw.substring(nullIdx + 1);
                    }
                }
            }

            offset += 12 + chunkLen; // 4(len) + 4(type) + data + 4(CRC)
        }
        return null;
    }

    // ==================== JSON 解析 ====================

    @SuppressWarnings("unchecked")
    private static CardData parseJson(byte[] data) throws IOException {
        Map<String, Object> json = objectMapper.readValue(data, Map.class);
        return mapToCardData(json);
    }

    @SuppressWarnings("unchecked")
    private static CardData parseCharaJson(String base64) throws IOException {
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new IOException("角色卡 Base64 解码失败: " + e.getMessage(), e);
        }
        Map<String, Object> json;

        // 尝试 UTF-8，失败回退 ISO-8859-1
        try {
            json = objectMapper.readValue(new String(decoded, java.nio.charset.StandardCharsets.UTF_8), Map.class);
        } catch (Exception e) {
            json = objectMapper.readValue(new String(decoded, java.nio.charset.StandardCharsets.ISO_8859_1), Map.class);
        }

        // Character Card V2 格式：{ "data": { ... }, "spec": "..." }
        if (json.containsKey("data") && json.get("data") instanceof Map) {
            return mapToCardData((Map<String, Object>) json.get("data"));
        }
        // 旧格式：直接就是角色数据
        return mapToCardData(json);
    }

    // ==================== 数据映射 ====================

    @SuppressWarnings("unchecked")
    private static CardData mapToCardData(Map<String, Object> map) {
        return CardData.builder()
                .name(getString(map, "name"))
                .description(getString(map, "description"))
                .personality(getString(map, "personality"))
                .scenario(getString(map, "scenario"))
                .firstMessage(getString(map, "first_mes"))
                .mesExample(getString(map, "mes_example"))
                .creatorNotes(getString(map, "creator_notes"))
                .systemPrompt(getString(map, "system_prompt"))
                .build();
    }

    // ==================== SystemPrompt 拼装 ====================

    /**
     * 将角色卡数据拼装为 Agent 的 systemPrompt。
     */
    public static String buildSystemPrompt(CardData card) {
        StringBuilder sb = new StringBuilder();

        if (card.getDescription() != null && !card.getDescription().isBlank()) {
            sb.append(card.getDescription()).append("\n");
        }

        if (card.getPersonality() != null && !card.getPersonality().isBlank()) {
            sb.append("\n## 性格特征\n").append(card.getPersonality()).append("\n");
        }

        if (card.getScenario() != null && !card.getScenario().isBlank()) {
            sb.append("\n## 当前场景\n").append(card.getScenario()).append("\n");
        }

        if (card.getMesExample() != null && !card.getMesExample().isBlank()) {
            // 清理 SillyTavern 格式的 <START> 标记
            String cleaned = card.getMesExample()
                    .replace("<START>", "")
                    .replace("{{user}}", "用户")
                    .replace("{{char}}", card.getName() != null ? card.getName() : "角色")
                    .trim();
            sb.append("\n## 对话示例\n").append(cleaned).append("\n");
        }

        if (card.getCreatorNotes() != null && !card.getCreatorNotes().isBlank()) {
            sb.append("\n## 补充说明\n").append(card.getCreatorNotes()).append("\n");
        }

        if (card.getSystemPrompt() != null && !card.getSystemPrompt().isBlank()) {
            sb.append("\n## 系统提示\n").append(card.getSystemPrompt()).append("\n");
        }

        // 角色扮演核心约束
        sb.append("\n---\n");
        sb.append("请以「").append(card.getName() != null ? card.getName() : "角色").append("」的身份进行对话。");
        sb.append("保持角色设定，不要跳出角色。使用中文回复。");

        return sb.toString();
    }

    // ==================== 工具方法 ====================

    private static String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val instanceof String ? (String) val : null;
    }

    private static ImageReader getPngReader() {
        var readers = ImageIO.getImageReadersByFormatName("png");
        return readers.hasNext() ? readers.next() : null;
    }

    private static IIOMetadataNode getChildByName(IIOMetadataNode parent, String name) {
        for (int i = 0; i < parent.getLength(); i++) {
            if (parent.item(i) instanceof IIOMetadataNode node
                    && name.equals(node.getNodeName())) {
                return node;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<IIOMetadataNode> getChildrenByName(IIOMetadataNode parent, String name) {
        List<IIOMetadataNode> result = new java.util.ArrayList<>();
        for (int i = 0; i < parent.getLength(); i++) {
            if (parent.item(i) instanceof IIOMetadataNode node
                    && name.equals(node.getNodeName())) {
                result.add(node);
            }
        }
        return result;
    }

    private static int readIntBigEndian(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 24)
                | ((data[offset + 1] & 0xFF) << 16)
                | ((data[offset + 2] & 0xFF) << 8)
                | (data[offset + 3] & 0xFF);
    }
}
