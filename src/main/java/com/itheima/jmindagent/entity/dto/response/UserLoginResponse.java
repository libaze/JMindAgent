package com.itheima.jmindagent.entity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户登录响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginResponse {

    /**
     * JWT令牌
     */
    private String token;

    /**
     * 过期时间戳（毫秒）
     */
    private Long expireTime;

    /**
     * 用户基础信息
     */
    private UserInfoResponse userInfo;
}
