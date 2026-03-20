package com.itheima.jmindagent.entity.dto.request;

import lombok.Data;

/**
 * 用户登录请求 DTO
 */
@Data
public class UserLoginRequest {

    /**
     * 用户名（必填）
     */
    private String username;

    /**
     * 密码（必填）
     */
    private String password;
}
