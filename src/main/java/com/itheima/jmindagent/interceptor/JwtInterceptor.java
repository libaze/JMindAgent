package com.itheima.jmindagent.interceptor;

import com.itheima.jmindagent.context.UserContext;
import com.itheima.jmindagent.context.UserContextHolder;
import com.itheima.jmindagent.entity.Result;
import com.itheima.jmindagent.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 认证拦截器
 * 负责解析 JWT令牌并将用户信息存储到 ThreadLocal 中
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 从请求头获取 token
        String token = request.getHeader("Authorization");

        // 处理 token 格式（去除 Bearer 前缀）
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 验证 token 有效性
        if (token == null || token.trim().isEmpty() || !jwtUtil.validateToken(token)) {
            log.warn("JWT 验证失败：token={}", token != null ? "已提供" : "未提供");

            // 设置响应类型和编码
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            // 写入错误响应
            Result result = Result.unauthorized("未授权/登录过期");
            response.getWriter().write(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(result));

            return false;
        }

        try {
            // 从 token 中解析用户信息
            Long userId = jwtUtil.getUserIdFromToken(token);
            String username = jwtUtil.getUsernameFromToken(token);

            if (userId == null || username == null) {
                log.error("Token 中缺少必要字段：userId={}, username={}", userId, username);
                throw new RuntimeException("无效的 token");
            }

            // 将用户信息存储到 ThreadLocal
            UserContext userContext = new UserContext(userId, username);
            UserContextHolder.set(userContext);

            log.debug("JWT 验证成功：userId={}, username={}", userId, username);

            return true;

        } catch (Exception e) {
            log.error("解析 JWT 失败：{}", e.getMessage());

            // 设置响应类型和编码
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            // 写入错误响应
            Result result = Result.unauthorized("无效的 token");
            response.getWriter().write(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(result));

            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求完成后清理 ThreadLocal，防止内存泄漏
        UserContextHolder.clear();
        log.debug("用户上下文已清理");
    }
}
