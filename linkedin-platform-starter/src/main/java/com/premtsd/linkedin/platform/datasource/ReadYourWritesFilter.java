package com.premtsd.linkedin.platform.datasource;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Resets the per-request write marker so a pooled server thread never carries
 * a stale "already wrote" flag from a previous request into a new one.
 */

public class ReadYourWritesFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        DataSourceContextHolder.clear();
        try {
            filterChain.doFilter(request, response);
        } finally {
            DataSourceContextHolder.clear();
        }
    }
}
