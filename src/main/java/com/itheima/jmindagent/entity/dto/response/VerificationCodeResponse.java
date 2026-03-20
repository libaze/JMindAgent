package com.itheima.jmindagent.entity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 验证码发送响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationCodeResponse {

    /**
     * 验证码有效期（秒）
     */
    private Integer expiresIn;

    /**
     * 重发间隔时间（秒）
     */
    private Integer resendAfter;
}
