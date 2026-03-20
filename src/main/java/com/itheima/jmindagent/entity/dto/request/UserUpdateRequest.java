package com.itheima.jmindagent.entity.dto.request;

import lombok.Data;

/**
 * 用户信息更新请求 DTO
 */
@Data
public class UserUpdateRequest {

    /**
     * 手机号（选填）
     */
    private String phone;

    /**
     * 邮箱（选填）
     */
    private String email;

    /**
     * 头像 URL（选填）
     */
    private String avatar;
}
