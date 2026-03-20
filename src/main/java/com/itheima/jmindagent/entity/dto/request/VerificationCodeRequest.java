package com.itheima.jmindagent.entity.dto.request;

import lombok.Data;

/**
 * 发送邮箱验证码请求 DTO
 */
@Data
public class VerificationCodeRequest {

    /**
     * 邮箱地址（必填）
     */
    private String email;

    /**
     * 验证码用途：register - 注册，forgot_password - 找回密码
     */
    private String purpose;
}
