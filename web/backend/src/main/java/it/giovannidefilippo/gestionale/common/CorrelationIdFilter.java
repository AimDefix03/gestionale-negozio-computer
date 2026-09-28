package it.giovannidefilippo.gestionale.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        long startedAt = System.nanoTime();

        request.setAttribute(CorrelationId.ATTRIBUTE_NAME, requestId);
        response.setHeader(CorrelationId.HEADER_NAME, requestId);
        response.setHeader("Access-Control-Expose-Headers", CorrelationId.HEADER_NAME);
        MDC.put(CorrelationId.ATTRIBUTE_NAME, requestId);
        MDC.put("http_method", request.getMethod());
        MDC.put("http_path", request.getRequestURI());

        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
            MDC.put("http_status", Integer.toString(response.getStatus()));
            MDC.put("duration_ms", Long.toString(elapsedMs));
            try {
                log.info("HTTP request completed");
            } finally {
                MDC.remove("duration_ms");
                MDC.remove("http_status");
                MDC.remove("http_path");
                MDC.remove("http_method");
                MDC.remove(CorrelationId.ATTRIBUTE_NAME);
            }
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(CorrelationId.HEADER_NAME);
        return CorrelationId.isValid(requestId) ? requestId.trim() : UUID.randomUUID().toString();
    }
}
