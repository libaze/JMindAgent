package com.itheima.jmindagent.entity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户头像上传响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAvatarUploadResponse {

    /**
     * 头像访问 URL
     */
    private String avatarUrl;

    /**
     * 文件原始名称
     */
    private String originalFilename;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;
}
