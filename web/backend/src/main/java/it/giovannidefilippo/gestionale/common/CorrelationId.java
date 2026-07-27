package it.giovannidefilippo.gestionale.common;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

public final class CorrelationId {
    public static final String HEADER_NAME = "X-Request-Id";
    public static final String ATTRIBUTE_NAME = "requestId";

    private CorrelationId() {
    }

    public static String from(HttpServletRequest request) {
        Object attribute = request.getAttribute(ATTRIBUTE_NAME);
        if (attribute instanceof String requestId && !requestId.isBlank()) {
            return requestId;
        }
        String requestId = request.getHeader(HEADER_NAME);
        return isValid(requestId) ? requestId.trim() : UUID.randomUUID().toString();
    }

    public static boolean isValid(String requestId) {
        return requestId != null
                && !requestId.isBlank()
                && requestId.length() <= 80
                && requestId.matches("[A-Za-z0-9._:-]+");
    }
}
