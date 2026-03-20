package com.itheima.jmindagent.config;

import com.itheima.jmindagent.interceptor.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 允许的来源域名，*表示允许所有来源
                // 生产环境建议指定具体域名，如："http://localhost:3000"
                .allowedOriginPatterns("*")
                // 允许的 HTTP 方法
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                // 允许携带凭证（cookies、认证信息等）
                .allowCredentials(true)
                // 允许的请求头
                .allowedHeaders("*")
                // 暴露的响应头（让前端可以获取到这些头信息）
                .exposedHeaders("Authorization", "Content-Type")
                // 预检请求的缓存时间（秒），避免每次请求都发送 OPTIONS
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .addResourceLocations("classpath:/public/")
                .addResourceLocations("classpath:/resources/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                // 拦截所有请求
                .addPathPatterns("/api/**")
                // 排除登录和注册接口，不需要 JWT 认证
                .excludePathPatterns(
                        "/api/user/login",
                        "/api/user/register",
                        "/api/user/logout",
                        "/api/user/send-verification-code"
                );
    }
}
