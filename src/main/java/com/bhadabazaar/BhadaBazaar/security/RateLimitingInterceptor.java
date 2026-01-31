package com.bhadabazaar.BhadaBazaar.security;

import com.bhadabazaar.BhadaBazaar.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    private final Cache<String, Bucket> authBuckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(50_000)
            .build();

    private final Cache<String, Bucket> generalBuckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(100_000)
            .build();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ip = request.getRemoteAddr();
        String path = request.getRequestURI();

        Bucket bucket = path.startsWith("/api/v1/auth/")
            ? authBuckets.get(ip, k -> createAuthBucket(k))
            : generalBuckets.get(ip, k -> createGeneralBucket(k));

        if (bucket.tryConsume(1)) {
            return true;
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            
            ErrorResponse errorResponse = new ErrorResponse(
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    "Too Many Requests",
                    "Too many requests. Please try again after some minutes."
            );
            
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            return false;
        }
    }

    private Bucket createAuthBucket(String key) {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(5, Refill.greedy(10, Duration.ofMinutes(1))))
                .build();
    }
    
    private Bucket createGeneralBucket(String key) {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1))))
                .build();
    }
}
