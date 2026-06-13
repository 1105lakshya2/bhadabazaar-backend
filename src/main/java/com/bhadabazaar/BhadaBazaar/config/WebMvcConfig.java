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
        // Intercept every API path; the interceptor itself picks the right bucket (auth / vendor /
        // upload / general). Matching broadly here means a new endpoint can never be silently left
        // unprotected, and keeps this list from drifting out of sync with the controllers.
        registry.addInterceptor(rateLimitingInterceptor)
                .addPathPatterns("/api/v1/**");
    }
}
