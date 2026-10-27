package com.clausify.config;

import com.clausify.common.RateLimiter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // ObjectProvider because @WebMvcTest slices load this class but not RateLimiter; they simply run unlimited.
    private final ObjectProvider<RateLimiter> rateLimiter;
    private final ObjectProvider<RateLimitProperties> limits;

    public WebConfig(ObjectProvider<RateLimiter> rateLimiter, ObjectProvider<RateLimitProperties> limits) {
        this.rateLimiter = rateLimiter;
        this.limits = limits;
    }

    /** Only the endpoints that are abusable or start paid work; everything else is unlimited. */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        RateLimiter limiter = rateLimiter.getIfAvailable();
        RateLimitProperties properties = limits.getIfAvailable();
        if (limiter == null || properties == null) {
            return;
        }
        registry.addInterceptor(new RateLimitInterceptor(limiter, properties))
                .addPathPatterns("/api/auth/login", "/api/auth/register", "/api/contracts", "/api/contracts/*/retry");
    }
}
