package com.itheima.jmindagent.entity.dto.request;

import lombok.Data;

/**
 * 会话创建请求 DTO
 */
@Data
public class ChatSessionCreateRequest {

    /**
     * 会话名称（选填）
     */
    private String sessionName;
}
