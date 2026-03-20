package com.itheima.jmindagent.entity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会话列表项响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionItemResponse {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 会话名称
     */
    private String sessionName;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 最后一条消息内容
     */
    private String lastContent;
}
