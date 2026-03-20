package com.itheima.jmindagent.controller;

import com.itheima.jmindagent.context.UserContextHolder;
import com.itheima.jmindagent.entity.Result;

import com.itheima.jmindagent.entity.TUser;
import com.itheima.jmindagent.entity.dto.request.UserLoginRequest;
import com.itheima.jmindagent.entity.dto.request.UserRegisterRequest;
import com.itheima.jmindagent.entity.dto.request.UserUpdateRequest;
import com.itheima.jmindagent.entity.dto.request.VerificationCodeRequest;
import com.itheima.jmindagent.entity.dto.response.*;
import com.itheima.jmindagent.service.IEmailService;
import com.itheima.jmindagent.service.ITUserService;
import com.itheima.jmindagent.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;

/**
 * 用户管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final ITUserService userService;
    private final IEmailService emailService;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 用户注册接口
     * 接口地址：/api/user/register
     * 请求方式：POST
     * 接口描述：完成新用户注册，生成唯一用户ID
     *
     * @param request 用户注册请求参数
     * @return 统一响应结果，包含 userId 和 username
     */
    @PostMapping("/register")
    public Result register(@RequestBody UserRegisterRequest request) {
        // 1. 参数校验
        if (request == null ||
            request.getUsername() == null || request.getUsername().trim().isEmpty() ||
            request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            return Result.paramError("用户名和密码不能为空");
        }

        // 2. 调用服务层进行注册
        UserRegisterResponse response = userService.register(request);

        // 3. 返回成功响应
        return Result.success(response);
    }

    /**
     * 用户登录接口
     * 接口地址：/api/user/login
     * 请求方式：POST
     * 接口描述：用户登录验证，生成JWT令牌
     *
     * @param request 用户登录请求参数
     * @return 统一响应结果，包含 token、expireTime 和 userInfo
     */
    @PostMapping("/login")
    public Result login(@RequestBody UserLoginRequest request) {
        // 1. 参数校验
        if (request == null ||
            request.getUsername() == null || request.getUsername().trim().isEmpty() ||
            request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            return Result.paramError("用户名和密码不能为空");
        }

        // 2. 调用服务层进行登录
        UserLoginResponse response = userService.login(request);

        // 3. 返回成功响应
        return Result.success(response);
    }

    /**
     * 用户信息查询接口
     * 接口地址：/api/user/info
     * 请求方式：GET
     * 接口描述：查询当前登录用户基础信息（通过token解析用户ID）
     *
     * @return 统一响应结果，包含用户详细信息
     */
    @GetMapping("/info")
    public Result getUserInfo() {
        // 从 ThreadLocal 获取当前用户ID
        Long userId = UserContextHolder.getUserId();

        // 5. 调用服务层查询用户信息
        UserInfoResponse userInfo = userService.getUserInfoById(userId);

        // 6. 返回成功响应
        return Result.success(userInfo);
    }

    /**
     * 用户头像上传接口
     * 接口地址：/api/user/avatar/upload
     * 请求方式：POST
     * 接口描述：上传用户头像，支持常见图片格式，使用 UUID 重命名
     *
     * @param file 头像文件
     * @return 统一响应结果，包含头像访问 URL
     */
    @PostMapping("/avatar/upload")
    public Result uploadAvatar(@RequestParam("file") MultipartFile file) {
        // 1. 调用服务层上传头像
        UserAvatarUploadResponse response = userService.uploadAvatar(file);

        // 2. 返回成功响应
        return Result.success(response);
    }

    /**
     * 用户信息更新接口
     * 接口地址：/api/user/update
     * 请求方式：PUT
     * 接口描述：更新用户信息，支持修改手机号、邮箱、头像
     *
     * @param request 用户更新请求参数
     * @return 统一响应结果，包含更新后的用户信息
     */
    @PutMapping("/update")
    public Result updateUser(@RequestBody UserUpdateRequest request) {
        // 1. 从 ThreadLocal 获取当前用户ID
        Long userId = UserContextHolder.getUserId();

        // 2. 调用服务层更新用户信息
        UserInfoResponse updatedUser = userService.updateUser(userId, request);

        // 3. 返回成功响应
        return Result.success(updatedUser);
    }

    @PostMapping("/send-verification-code")
    public Result sendVerificationCode(@RequestBody VerificationCodeRequest request) {
        // 1. 参数校验
        if (request == null ||
                request.getEmail() == null ||
                request.getEmail().trim().isEmpty()) {
            return Result.paramError("邮箱地址不能为空");
        }

        // 验证邮箱格式
        String email = request.getEmail().trim().toLowerCase();
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            return Result.paramError("邮箱格式不正确");
        }

        // 设置默认用途
        String purpose = request.getPurpose() != null ? request.getPurpose().toLowerCase() : "register";

        // 验证用途合法性
        if (!isValidPurpose(purpose)) {
            return Result.paramError("不支持的验证码用途");
        }

        // 2. 根据用途检查邮箱
        if ("register".equals(purpose)) {
            // 检查邮箱是否已注册
            TUser existingUser = userService.getUserByEmail(email);
            if (existingUser != null) {
                return Result.fail(400, "该邮箱已被注册");
            }
        } else if ("forgot_password".equals(purpose) || "change_email".equals(purpose)) {
            // 找回密码或修改邮箱时，检查邮箱是否存在
            TUser existingUser = userService.getUserByEmail(email);
            if (existingUser == null) {
                return Result.fail(400, "该邮箱未注册");
            }
        }

        // 3. 检查发送频率限制（防止恶意刷邮件）
        String rateLimitKey = "verification:rate_limit:" + purpose + ":" + email;
        String sentCount = redisTemplate.opsForValue().get(rateLimitKey);
        if (sentCount != null) {
            int count = Integer.parseInt(sentCount);
            if (count >= 5) {
                // 获取剩余等待时间
                Long ttl = redisTemplate.getExpire(rateLimitKey, TimeUnit.SECONDS);
                return Result.fail(429, String.format("发送过于频繁，请 %d 分钟后再试", ttl / 60));
            }
        }

        // 4. 生成 6 位验证码
        String code = emailService.generateVerificationCode();

        // 5. 发送验证码邮件
        try {
            emailService.sendVerificationCode(email, code, purpose);
        } catch (Exception e) {
            log.error("验证码发送失败：email={}, purpose={}, error={}", email, purpose, e.getMessage());
            return Result.fail(500, "验证码发送失败：" + e.getMessage());
        }

        // 6. 记录发送次数（1 小时内最多发送 5 次）
        redisTemplate.opsForValue().increment(rateLimitKey);
        redisTemplate.expire(rateLimitKey, 1, TimeUnit.HOURS);

        // 7. 返回成功响应（不返回验证码）
        VerificationCodeResponse response = VerificationCodeResponse.builder()
                .expiresIn(300)  // 5 分钟有效期
                .resendAfter(60) // 60 秒后可重发
                .build();

        return Result.success(response);
    }

    /**
     * 验证验证码用途是否合法
     */
    private boolean isValidPurpose(String purpose) {
        return "register".equals(purpose) ||
                "forgot_password".equals(purpose) ||
                "change_email".equals(purpose);
    }
}
