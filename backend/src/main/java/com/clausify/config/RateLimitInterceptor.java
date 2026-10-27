package com.clausify.config;

import com.clausify.common.CurrentUser;
import com.clausify.common.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Applies the per-IP and per-user limits from {@link RateLimitProperties} before the controller runs (#80).
 * Created and registered for specific paths in {@link WebConfig}; a 429 is turned into ProblemDetail by GlobalExceptionHandler.
 * <p>
 * The client IP is {@code request.getRemoteAddr()}. In prod, {@code server.forward-headers-strategy: framework}
 * sets it from X-Forwarded-For, which Vercel overwrites with the real client IP. A caller that skips Vercel and
 * hits Render directly can forge that header, so per-IP limits slow casual abuse only; the per-user and
 * per-email limits are the ones that cannot be bypassed (ADR 0009).
 */
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiter rateLimiter;
    private final RateLimitProperties limits;

    public RateLimitInterceptor(RateLimiter rateLimiter, RateLimitProperties limits) {
        this.rateLimiter = rateLimiter;
        this.limits = limits;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!"POST".equals(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        if (path.equals("/api/auth/login")) {
            rateLimiter.acquire("login-ip:" + request.getRemoteAddr(), limits.login());
        } else if (path.equals("/api/auth/register")) {
            rateLimiter.acquire("register-ip:" + request.getRemoteAddr(), limits.register());
        } else if (path.startsWith("/api/contracts")) {
            // Upload and retry both start an AI analysis, so they share one per-user budget.
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                rateLimiter.acquire("analysis-user:" + CurrentUser.id(jwt), limits.analysis());
            }
        }
        return true;
    }
}
