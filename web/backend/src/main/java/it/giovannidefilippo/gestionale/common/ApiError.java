package it.giovannidefilippo.gestionale.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        List<String> details,
        String path,
        String requestId
) {
    public static ApiError of(HttpStatus status, ApiErrorCode code, String message, List<String> details, HttpServletRequest request, Instant timestamp) {
        return new ApiError(
                timestamp,
                status.value(),
                code.name(),
                message,
                details,
                request.getRequestURI(),
                CorrelationId.from(request)
        );
    }
}
