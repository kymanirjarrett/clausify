package com.clausify.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Called by Spring Security when a request needs authentication but has none (or an invalid token).
 * Security filters run before Spring MVC, so @RestControllerAdvice cannot see these exceptions on its own;
 * handing them to MVC's exception resolver makes GlobalExceptionHandler write the same ProblemDetail JSON
 * as every other error.
 */
@Component
public class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final HandlerExceptionResolver resolver;

    public ProblemDetailAuthenticationEntryPoint(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) {
        // RFC 6750: tell the client which auth scheme to use.
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        resolver.resolveException(request, response, null, ex);
    }
}
