package com.itheima.jmindagent.entity.dto.request;

import lombok.Data;

/**
 * 历史记录查询请求 DTO
 */
@Data
public class ChatHistoryRequest {

    /**
     * 会话ID（必填）
     */
    private String sessionId;
}
