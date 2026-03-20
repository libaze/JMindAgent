package com.itheima.jmindagent.service.impl;

import com.itheima.jmindagent.entity.TUser;
import com.itheima.jmindagent.entity.dto.request.UserLoginRequest;
import com.itheima.jmindagent.entity.dto.request.UserRegisterRequest;
import com.itheima.jmindagent.entity.dto.request.UserUpdateRequest;
import com.itheima.jmindagent.entity.dto.response.UserAvatarUploadResponse;
import com.itheima.jmindagent.entity.dto.response.UserInfoResponse;
import com.itheima.jmindagent.entity.dto.response.UserLoginResponse;
import com.itheima.jmindagent.entity.dto.response.UserRegisterResponse;
import com.itheima.jmindagent.mapper.TUserMapper;
import com.itheima.jmindagent.service.IEmailService;
import com.itheima.jmindagent.service.ITUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.jmindagent.utils.JwtUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author
 * @since 2026-03-16
 */
@Service
public class TUserServiceImpl extends ServiceImpl<TUserMapper, TUser> implements ITUserService {

    private final JwtUtil jwtUtil;
    private final IEmailService emailService;

    public TUserServiceImpl(JwtUtil jwtUtil, IEmailService emailService) {
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserRegisterResponse register(UserRegisterRequest request) {
        // 1. 基础参数校验
        if (request == null ||
                request.getUsername() == null || request.getUsername().trim().isEmpty() ||
                request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new RuntimeException("用户名和密码不能为空");
        }

        // 2. 校验用户名是否已存在
        LambdaQueryWrapper<TUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TUser::getUsername, request.getUsername());
        TUser existingUser = this.getOne(queryWrapper);

        if (existingUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 3. 邮箱
        // 3.1 校验邮箱格式
        String email = request.getEmail().trim().toLowerCase();
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new RuntimeException("邮箱格式不正确");
        }

        // 3.2 检查邮箱是否已被注册
        TUser emailUser = this.getUserByEmail(email);
        if (emailUser != null) {
            throw new RuntimeException("该邮箱已被注册");
        }

        // 3.3 验证码不能为空
        if (request.getVerificationCode() == null || request.getVerificationCode().trim().isEmpty()) {
            throw new RuntimeException("邮箱验证码不能为空");
        }

        // 3.4 调用邮件服务验证验证码
        boolean isValid = emailService.verifyCode(email, request.getVerificationCode().trim(), "register");
        if (!isValid) {
            throw new RuntimeException("邮箱验证码错误或已过期");
        }


        // 4. 构建用户对象
        TUser user = new TUser();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());

        // 5. 设置可选参数
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail() != null ? request.getEmail().trim().toLowerCase() : null);
        user.setAvatar("/avatars/80838818716b475a873fcb36a0d205dc.png");
        // 6. 设置时间戳
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        // 7. 保存到数据库
        this.save(user);

        // 8. 构建响应对象
        return UserRegisterResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .build();
    }

    @Override
    public UserLoginResponse login(UserLoginRequest request) {
        // 1. 根据用户名查询用户
        LambdaQueryWrapper<TUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TUser::getUsername, request.getUsername());
        TUser user = this.getOne(queryWrapper);

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 2. 验证密码
        if (!request.getPassword().equals(user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        // 3. 生成JWT令牌
        String token = jwtUtil.generateToken(user.getUserId(), user.getUsername());

        // 4. 获取过期时间戳
        Long expireTime = jwtUtil.getExpireTime();

        // 5. 构建用户信息对象
        UserInfoResponse userInfo = UserInfoResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .phone(user.getPhone())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .build();

        // 6. 构建登录响应对象
        return UserLoginResponse.builder()
                .token(token)
                .expireTime(expireTime)
                .userInfo(userInfo)
                .build();
    }


    @Override
    public UserInfoResponse getUserInfoById(Long userId) {
        // 1. 根据用户ID 查询用户
        TUser user = this.getById(userId);

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 2. 构建用户信息响应对象
        return UserInfoResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .phone(user.getPhone())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .createTime(user.getCreateTime())
                .build();
    }

    @Override
    public UserAvatarUploadResponse uploadAvatar(MultipartFile file) {
        // 1. 参数校验
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }

        // 2. 校验文件类型（仅允许图片）
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("仅支持上传图片文件");
        }

        // 3. 生成唯一文件名（UUID + 原文件扩展名）
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String newFilename = UUID.randomUUID().toString().replace("-", "") + extension;

        // 4. 确定存储路径（resources/static/avatars/）
        String uploadDir = "src/main/resources/static/avatars";
        try {
            Path dirPath = Paths.get(uploadDir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            // 5. 保存文件
            Path filePath = dirPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 6. 构建访问 URL（静态资源映射路径：/avatars/文件名）
            String accessUrl = "/avatars/" + newFilename;

            // 7. 构建响应对象
            return UserAvatarUploadResponse.builder()
                    .avatarUrl(accessUrl)
                    .originalFilename(originalFilename)
                    .fileSize(file.getSize())
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("头像上传失败：" + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserInfoResponse updateUser(Long userId, UserUpdateRequest request) {
        // 1. 查询用户
        TUser user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 2. 选择性更新字段（只更新非空字段）
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }

        // 3. 更新时间
        user.setUpdateTime(LocalDateTime.now());

        // 4. 保存到数据库
        this.updateById(user);

        // 5. 构建响应对象
        return UserInfoResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .phone(user.getPhone())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .createTime(user.getCreateTime())
                .build();
    }

    @Override
    public TUser getUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        LambdaQueryWrapper<TUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TUser::getEmail, email.trim());
        return this.getOne(queryWrapper);
    }
}
