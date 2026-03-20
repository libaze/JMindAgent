package com.itheima.jmindagent.service.impl;

import com.itheima.jmindagent.service.IEmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 邮件服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements IEmailService {

    private final JavaMailSender mailSender;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${mail.from}")
    private String fromEmail;
    @Value("${mail.code.expire-time}")
    private Long codeExpireTime;
    private static final String EMAIL_TEMPLATE_PATH = "static/verification_code_email.html";

    private static final Random RANDOM = new Random();

    @Override
    public void sendVerificationCode(String to, String code, String purpose) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("【JMindAgent】验证码邮件");

            String content = buildEmailContent(code, purpose);
            helper.setText(content, true);

            mailSender.send(message);

            // 将验证码存储到 Redis，设置过期时间
            String redisKey = buildRedisKey(to, purpose);
            redisTemplate.opsForValue().set(redisKey, code, codeExpireTime, TimeUnit.MILLISECONDS);

            log.info("验证码邮件发送成功：to={}, purpose={}", to, purpose);

        } catch (MessagingException e) {
            log.error("验证码邮件发送失败：to={}, error={}", to, e.getMessage());
            throw new RuntimeException("邮件发送失败，请稍后重试", e);
        }
    }

    @Override
    public String generateVerificationCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(RANDOM.nextInt(10));
        }
        return code.toString();
    }

    @Override
    public boolean verifyCode(String email, String code, String purpose) {
        String redisKey = buildRedisKey(email, purpose);
        String storedCode = redisTemplate.opsForValue().get(redisKey);

        if (storedCode == null) {
            log.warn("验证码已过期或不存在：email={}, purpose={}", email, purpose);
            return false;
        }

        if (!storedCode.equals(code)) {
            log.warn("验证码不匹配：email={}, input={}, expected={}", email, code, storedCode);
            return false;
        }

        // 验证成功后删除验证码
        redisTemplate.delete(redisKey);
        log.info("验证码验证成功：email={}, purpose={}", email, purpose);
        return true;
    }

    /**
     * 构建邮件内容
     */
    /**
     * 构建邮件内容
     */
    private String buildEmailContent(String code, String purpose) {
        try {
            // 从 classpath 读取 HTML 模板文件
            ClassPathResource resource = new ClassPathResource(EMAIL_TEMPLATE_PATH);
            String template = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

            // 替换模板中的占位符
            String purposeText = getPurposeText(purpose);
            return template.replace("{{purpose}}", purposeText)
                    .replace("{{code}}", code);

        } catch (Exception e) {
            log.error("读取邮件模板失败：path={}, error={}", EMAIL_TEMPLATE_PATH, e.getMessage());
            // 如果读取模板失败，返回简单的文本内容
            return buildSimpleEmailContent(code, purpose);
        }
    }
    private String buildSimpleEmailContent(String code, String purpose) {
        String purposeText = getPurposeText(purpose);

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .code-box { background: white; border: 2px dashed #667eea; padding: 20px; text-align: center; margin: 20px 0; border-radius: 8px; }
                    .code { font-size: 32px; font-weight: bold; color: #667eea; letter-spacing: 5px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>JMindAgent 验证码</h2>
                    <p>您正在进行%s操作</p>
                    <div class="code-box">
                        <div class="code">%s</div>
                    </div>
                    <p>验证码有效期为 5 分钟</p>
                </div>
            </body>
            </html>
            """.formatted(purposeText, code);
    }
    /**
     * 获取验证码用途文本
     */
    private String getPurposeText(String purpose) {
        return switch (purpose.toLowerCase()) {
            case "register" -> "用户注册";
            case "forgot_password" -> "找回密码";
            case "change_email" -> "修改邮箱";
            default -> "身份验证";
        };
    }

    /**
     * 构建 Redis 键
     */
    private String buildRedisKey(String email, String purpose) {
        return "verification:code:" + purpose + ":" + email;
    }
}
