package com.premtsd.linkedin.connectionservice.auth;

import lombok.experimental.UtilityClass;

/**
 * Stores the authenticated user ID in a ThreadLocal context.
 * Should be cleared after each request (typically in a filter).
 */
@UtilityClass
public class UserContextHolder {

    private static final ThreadLocal<Long> currentUserId = new ThreadLocal<>();

    public Long getCurrentUserId() {
        return currentUserId.get();
    }

    public void setCurrentUserId(Long userId) {
        currentUserId.set(userId);
    }

    public void clear() {
        currentUserId.remove();
    }
}
