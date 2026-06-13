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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    // Login/signup: strict, keyed by client IP.
    private final Cache<String, Bucket> authBuckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(50_000)
            .build();

    // Public browsing: keyed by client IP.
    private final Cache<String, Bucket> generalBuckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(100_000)
            .build();

    // Authenticated vendor APIs: keyed by vendor identity so one compromised token throttles only
    // that vendor, not everyone sharing an IP.
    private final Cache<String, Bucket> vendorBuckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(50_000)
            .build();

    // Image uploads are expensive (Cloudflare bill); throttle them harder, per vendor.
    private final Cache<String, Bucket> uploadBuckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(50_000)
            .build();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Don't spend tokens on CORS preflight.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        Bucket bucket = selectBucket(request, path);

        if (bucket.tryConsume(1)) {
            return true;
        }

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

    private Bucket selectBucket(HttpServletRequest request, String path) {
        if (path.startsWith("/api/v1/auth/")) {
            return authBuckets.get(resolveClientIp(request), k -> createAuthBucket());
        }
        if (path.startsWith("/api/v1/vendor/")) {
            String key = vendorKey(request);
            return isUploadPath(path)
                    ? uploadBuckets.get(key, k -> createUploadBucket())
                    : vendorBuckets.get(key, k -> createVendorBucket());
        }
        return generalBuckets.get(resolveClientIp(request), k -> createGeneralBucket());
    }

    /**
     * Resolves the real client IP. Behind Cloudflare/Render, {@code getRemoteAddr()} is the proxy,
     * so every customer would otherwise share one bucket. Prefer Cloudflare's per-request header,
     * then the first hop of X-Forwarded-For, then the socket address.
     *
     * Note: these headers are only trustworthy because the origin is reachable solely through
     * Cloudflare/Render, which set/overwrite them. If the origin is ever exposed directly, a client
     * could spoof X-Forwarded-For to dodge limits.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String cfIp = request.getHeader("CF-Connecting-IP");
        if (cfIp != null && !cfIp.isBlank()) {
            return cfIp.trim();
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > -1 ? xff.substring(0, comma) : xff).trim();
        }
        return request.getRemoteAddr();
    }

    /** Keys vendor traffic by the authenticated username, falling back to IP if unauthenticated. */
    private String vendorKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken)
                && auth.getName() != null) {
            return "vendor:" + auth.getName();
        }
        return "ip:" + resolveClientIp(request);
    }

    /** Upload endpoints: POST /vendor/items/{id}/images and POST /vendor/store/image. */
    private boolean isUploadPath(String path) {
        return path.endsWith("/images") || path.endsWith("/store/image");
    }

    private Bucket createAuthBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(5, Refill.greedy(10, Duration.ofMinutes(1))))
                .build();
    }

    private Bucket createGeneralBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1))))
                .build();
    }

    private Bucket createVendorBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(120, Refill.greedy(120, Duration.ofMinutes(1))))
                .build();
    }

    private Bucket createUploadBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(30, Refill.greedy(30, Duration.ofMinutes(1))))
                .build();
    }
}
