package com.itheima.jmindagent.service;

import com.itheima.jmindagent.entity.TUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.jmindagent.entity.dto.request.UserLoginRequest;
import com.itheima.jmindagent.entity.dto.request.UserRegisterRequest;
import com.itheima.jmindagent.entity.dto.request.UserUpdateRequest;
import com.itheima.jmindagent.entity.dto.response.UserAvatarUploadResponse;
import com.itheima.jmindagent.entity.dto.response.UserInfoResponse;
import com.itheima.jmindagent.entity.dto.response.UserLoginResponse;
import com.itheima.jmindagent.entity.dto.response.UserRegisterResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author
 * @since 2026-03-16
 */
public interface ITUserService extends IService<TUser> {

    /**
     * 用户注册
     * @param request 用户注册请求参数
     * @return 用户注册响应，包含 userId 和 username
     */
    UserRegisterResponse register(UserRegisterRequest request);

    /**
     * 用户登录
     * @param request 用户登录请求参数
     * @return 用户登录响应，包含 token、expireTime 和 userInfo
     */
    UserLoginResponse login(UserLoginRequest request);

    /**
     * 根据用户ID 查询用户信息
     * @param userId 用户ID
     * @return 用户详细信息
     */
    UserInfoResponse getUserInfoById(Long userId);

    /**
     * 上传用户头像
     * @param file 头像文件
     * @return 头像上传响应，包含访问 URL
     */
    UserAvatarUploadResponse uploadAvatar(MultipartFile file);

    /**
     * 更新用户信息
     * @param userId 用户ID
     * @param request 更新请求参数
     * @return 更新后的用户信息
     */
    UserInfoResponse updateUser(Long userId, UserUpdateRequest request);

    /**
     * 根据邮箱查询用户
     * @param email 邮箱地址
     * @return 用户对象，如果不存在则返回 null
     */
    TUser getUserByEmail(String email);
}
