package com.premtsd.linkedin.platform.datasource;

/**
 * Tracks whether the current thread (i.e. the current HTTP request, cleared by
 * {@link ReadYourWritesFilter}) has performed a write. Once a write happens,
 * all later reads in the same request are pinned to the primary so the request
 * always sees its own writes, even if a replica lags.
 */
public final class DataSourceContextHolder {

    private static final ThreadLocal<Boolean> WRITE_OCCURRED = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private DataSourceContextHolder() {
    }

    public static void markWrite() {
        WRITE_OCCURRED.set(Boolean.TRUE);
    }

    public static boolean hasWritten() {
        return WRITE_OCCURRED.get();
    }

    public static void clear() {
        WRITE_OCCURRED.remove();
    }
}
