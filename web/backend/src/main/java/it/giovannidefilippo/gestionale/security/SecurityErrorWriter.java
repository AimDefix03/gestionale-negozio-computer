package it.giovannidefilippo.gestionale.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.giovannidefilippo.gestionale.common.ApiErrorCode;
import it.giovannidefilippo.gestionale.common.ApiError;
import it.giovannidefilippo.gestionale.common.OperationalMetrics;
import it.giovannidefilippo.gestionale.common.TimeProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
class SecurityErrorWriter {
    private final ObjectMapper objectMapper;
    private final OperationalMetrics operationalMetrics;
    private final TimeProvider timeProvider;

    SecurityErrorWriter(ObjectMapper objectMapper, OperationalMetrics operationalMetrics, TimeProvider timeProvider) {
        this.objectMapper = objectMapper;
        this.operationalMetrics = operationalMetrics;
        this.timeProvider = timeProvider;
    }

    void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, ApiErrorCode code, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        operationalMetrics.recordApiError(status, code);
        objectMapper.writeValue(response.getWriter(), ApiError.of(status, code, message, List.of(), request, timeProvider.instant()));
    }
}
