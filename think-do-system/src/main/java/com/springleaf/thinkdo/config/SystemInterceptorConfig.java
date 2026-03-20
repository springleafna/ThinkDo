package com.springleaf.thinkdo.config;

import com.springleaf.thinkdo.interceptor.UserContextInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * System模块的Web配置
 * 主要用于注册用户上下文拦截器
 */
@Configuration
@RequiredArgsConstructor
public class SystemInterceptorConfig implements WebMvcConfigurer {

    private final UserContextInterceptor userContextInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册用户上下文拦截器，拦截所有请求
        registry.addInterceptor(userContextInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/error",
                        "/swagger-resources/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/doc.html" // knife4j文档
                );
    }
}
