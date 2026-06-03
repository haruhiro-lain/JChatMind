package com.kama.mindharness.model.response;

import lombok.Builder;
import lombok.Data;

/**
 * 角色卡导入预览响应。
 */
@Data
@Builder
public class ImportCardResponse {

    /** 角色名称 */
    private String name;

    /** 角色描述 */
    private String description;

    /** 拼装好的 systemPrompt（前端可预览、可编辑） */
    private String systemPrompt;

    /** 首条消息（可选） */
    private String firstMessage;

    /** 头像文件名（PNG 角色卡上传后保存的文件名，如 a1b2c3d4.png） */
    private String avatarFileName;
}
