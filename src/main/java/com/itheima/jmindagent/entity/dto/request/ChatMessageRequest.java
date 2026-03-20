package com.itheima.jmindagent.entity.dto.request;

import lombok.Data;

/**
 * 聊天消息请求 DTO
 */
@Data
public class ChatMessageRequest {

    /**
     * 会话ID（必填）
     */
    private String sessionId;

    /**
     * 用户提问内容（必填）
     */
    private String content;

    /**
     * 自定义Agent ID（选填，存在时使用 Agent 模式，否则降级为普通对话）
     */
    private String agentId;

    /**
     * 知识库 ID（选填，所有模式都支持知识库检索）
     */
    private String kbId;
}
