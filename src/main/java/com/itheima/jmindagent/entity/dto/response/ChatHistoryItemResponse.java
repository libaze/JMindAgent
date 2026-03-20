package com.itheima.jmindagent.entity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 历史消息项响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryItemResponse {

    /**
     * 消息 ID
     */
    private Long msgId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 发送方类型：1-用户，2-AI
     */
    private Integer senderType;

    /**
     * 发送方描述：user 或 ai
     */
    private String senderDesc;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型：1-普通，2-RAG，3-工具调用
     */
    private Integer msgType;

    /**
     * 消息类型描述
     */
    private String msgTypeDesc;

    /**
     * 发送时间
     */
    private LocalDateTime createTime;
}
