package com.sme.afs.web;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;

public class CorrelationIdFilter implements Filter {
    public static final String CORRELATION_ID = "correlationId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String correlationId = UUID.randomUUID().toString().replace("-", "");
        MDC.put(CORRELATION_ID, correlationId);
        try {
            ((HttpServletResponse) response).setHeader("X-Correlation-Id", correlationId);
            chain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID);
        }
    }
}