package it.giovannidefilippo.gestionale.common;

import it.giovannidefilippo.gestionale.idempotency.IdempotencyConflictException;
import it.giovannidefilippo.gestionale.system.ApiErrorMonitor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.persistence.OptimisticLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ApiErrorMonitor apiErrorMonitor;
    private final OperationalMetrics operationalMetrics;
    private final TimeProvider timeProvider;

    GlobalExceptionHandler(ApiErrorMonitor apiErrorMonitor, OperationalMetrics operationalMetrics, TimeProvider timeProvider) {
        this.apiErrorMonitor = apiErrorMonitor;
        this.operationalMetrics = operationalMetrics;
        this.timeProvider = timeProvider;
    }

    @ExceptionHandler(UnauthorizedException.class)
    ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(error(HttpStatus.UNAUTHORIZED, ApiErrorCode.AUTH_UNAUTHORIZED, exception.getMessage(), List.of(), request));
    }

    @ExceptionHandler(ForbiddenException.class)
    ResponseEntity<ApiError> handleForbidden(ForbiddenException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(error(HttpStatus.FORBIDDEN, ApiErrorCode.AUTH_FORBIDDEN, exception.getMessage(), List.of(), request));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException exception, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(error(HttpStatus.BAD_REQUEST, ApiErrorCode.REQUEST_INVALID, exception.getMessage(), List.of(), request));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ApiError> handleIllegalState(IllegalStateException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error(HttpStatus.CONFLICT, ApiErrorCode.RESOURCE_CONFLICT, exception.getMessage(), List.of(), request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<String> details = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(error(HttpStatus.UNPROCESSABLE_ENTITY, ApiErrorCode.VALIDATION_FAILED, "Dati non validi.", details, request));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleMalformedRequest(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(error(HttpStatus.BAD_REQUEST, ApiErrorCode.REQUEST_MALFORMED, "Richiesta non leggibile.", List.of(), request));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        String parameter = exception.getName();
        return ResponseEntity.badRequest()
                .body(error(HttpStatus.BAD_REQUEST, ApiErrorCode.REQUEST_INVALID, "Parametro non valido.", List.of(parameter), request));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> handleResourceNotFound(NoResourceFoundException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(error(HttpStatus.NOT_FOUND, ApiErrorCode.RESOURCE_NOT_FOUND, "Risorsa non trovata.", List.of(), request));
    }

    @ExceptionHandler({OptimisticLockingFailureException.class, OptimisticLockException.class})
    ResponseEntity<ApiError> handleOptimisticLocking(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error(HttpStatus.CONFLICT, ApiErrorCode.RESOURCE_CONFLICT, "I dati sono stati modificati da un'altra operazione. Ricarica e riprova.", List.of(), request));
    }

    @ExceptionHandler(ResourceConflictException.class)
    ResponseEntity<ApiError> handleResourceConflict(ResourceConflictException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error(HttpStatus.CONFLICT, ApiErrorCode.RESOURCE_CONFLICT, exception.getMessage(), List.of(), request));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error(HttpStatus.CONFLICT, ApiErrorCode.RESOURCE_CONFLICT, "Operazione non consentita per vincolo sui dati.", List.of(), request));
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    ResponseEntity<ApiError> handleIdempotencyConflict(IdempotencyConflictException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error(HttpStatus.CONFLICT, ApiErrorCode.IDEMPOTENCY_CONFLICT, exception.getMessage(), List.of(), request));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled API exception", exception);
        ApiError error = error(HttpStatus.INTERNAL_SERVER_ERROR, ApiErrorCode.INTERNAL_ERROR, "Errore interno del server.", List.of(), request);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }

    private ApiError error(HttpStatus status, ApiErrorCode code, String message, List<String> details, HttpServletRequest request) {
        ApiError error = ApiError.of(status, code, message, details, request, timeProvider.instant());
        operationalMetrics.recordApiError(status, code);
        if (status.is5xxServerError()) {
            apiErrorMonitor.record(error);
        }
        return error;
    }
}
