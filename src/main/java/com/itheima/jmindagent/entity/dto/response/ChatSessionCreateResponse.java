package com.itheima.jmindagent.entity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会话创建响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionCreateResponse {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 会话名称
     */
    private String sessionName;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
