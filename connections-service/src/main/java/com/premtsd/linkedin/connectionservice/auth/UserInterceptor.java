package com.premtsd.linkedin.connectionservice.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Intercepts every HTTP request to extract the authenticated user ID from the "X-User-Id" header,
 * and stores it in a ThreadLocal context for use throughout the request lifecycle.
 */
@Slf4j
@Component
public class UserInterceptor implements HandlerInterceptor {

    private static final String USER_ID_HEADER = "X-User-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userIdHeader = request.getHeader(USER_ID_HEADER);

        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                Long userId = Long.parseLong(userIdHeader);
                UserContextHolder.setCurrentUserId(userId);
                log.debug("User ID {} set in UserContextHolder for request: {}", userId, request.getRequestURI());
            } catch (NumberFormatException e) {
                log.warn("Invalid X-User-Id header format: '{}'", userIdHeader);
            }
        } else {
            log.debug("No X-User-Id header found for request: {}", request.getRequestURI());
        }

        return true; // Continue processing
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContextHolder.clear();
        log.debug("Cleared UserContextHolder after request: {}", request.getRequestURI());
    }
}
