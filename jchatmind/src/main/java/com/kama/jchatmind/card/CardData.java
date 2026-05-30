package com.kama.jchatmind.card;

import lombok.Builder;
import lombok.Data;

/**
 * 角色卡解析结果数据结构。
 * 字段名遵循 Character Card V2 规范。
 */
@Data
@Builder
public class CardData {

    /** 角色名称 */
    private String name;

    /** 角色描述 / 简介 */
    private String description;

    /** 性格特征 */
    private String personality;

    /** 场景描述 */
    private String scenario;

    /** 首条消息（角色第一次说的话） */
    private String firstMessage;

    /** 对话示例（SillyTavern 格式，含 <START> 分隔符） */
    private String mesExample;

    /** 创作者备注 */
    private String creatorNotes;

    /** 自定义系统提示词（覆盖默认拼装） */
    private String systemPrompt;
}
