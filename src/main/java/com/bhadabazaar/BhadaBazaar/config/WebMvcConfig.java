package com.bhadabazaar.BhadaBazaar.config;

import com.bhadabazaar.BhadaBazaar.security.RateLimitingInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitingInterceptor rateLimitingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitingInterceptor)
                .addPathPatterns(
                        "/api/v1/auth/**",
                        "/api/v1/cities/**",
                        "/api/v1/stores/**",
                        "/api/v1/categories",
                        "/api/v1/items/**"
                );
    }
}
