package com.itheima.jmindagent.entity.dto.request;

import lombok.Data;

/**
 * 用户注册请求 DTO
 */
@Data
public class UserRegisterRequest {

    /**
     * 用户名（必填）
     */
    private String username;

    /**
     * 密码（必填）
     */
    private String password;

    /**
     * 手机号（必填）
     */
    private String phone;

    /**
     * 邮箱（必填）
     */
    private String email;

    /**
     * 邮箱验证码（必填）
     */
    private String verificationCode;
}
