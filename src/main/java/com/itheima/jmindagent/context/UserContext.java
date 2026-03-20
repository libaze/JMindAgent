package com.itheima.jmindagent.context;

import lombok.Data;

/**
 * 用户上下文数据
 */
@Data
public class UserContext {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    public UserContext(Long userId, String username) {
        this.userId = userId;
        this.username = username;
    }
}
