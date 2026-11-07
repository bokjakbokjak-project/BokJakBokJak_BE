package kr.bokjakbokjak.common;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.databind.PropertyNamingStrategies;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiResponse.Failure> business(ApiException exception) {
        var response = ResponseEntity.status(exception.status());
        if (exception.retryAfterSeconds() != null) {
            response.header(HttpHeaders.RETRY_AFTER, exception.retryAfterSeconds().toString());
        }
        return response.body(ApiResponse.failure(exception.code(), exception.getMessage(), exception.errors()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiResponse.Failure> forbidden() {
        return ResponseEntity.status(ErrorCode.FORBIDDEN.status())
                .body(ApiResponse.failure(ErrorCode.FORBIDDEN, Map.of()));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            String name = PropertyNamingStrategies.SNAKE_CASE.nameForField(null, null, error.getField());
            errors.putIfAbsent(name, error.getDefaultMessage() == null
                    ? ErrorCode.INVALID_INPUT.message() : error.getDefaultMessage());
        }
        return handleExceptionInternal(exception, ApiResponse.failure(ErrorCode.INVALID_INPUT, errors),
                headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(exception, ApiResponse.failure(ErrorCode.INVALID_INPUT, Map.of()),
                headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleErrorResponseException(
            ErrorResponseException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(exception, null, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (body instanceof ApiResponse.Failure failure) {
            return super.handleExceptionInternal(exception, failure, headers, status, request);
        }
        ApiResponse.Failure failure = switch (status.value()) {
            case 400 -> ApiResponse.failure(ErrorCode.INVALID_INPUT, Map.of());
            case 401 -> ApiResponse.failure(ErrorCode.UNAUTHORIZED, Map.of());
            case 403 -> ApiResponse.failure(ErrorCode.FORBIDDEN, Map.of());
            case 404 -> ApiResponse.failure(ErrorCode.RESOURCE_NOT_FOUND, Map.of());
            case 405 -> ApiResponse.failure(ErrorCode.METHOD_NOT_ALLOWED, Map.of());
            case 415 -> ApiResponse.failure(ErrorCode.UNSUPPORTED_MEDIA_TYPE, Map.of());
            default -> status.is5xxServerError()
                    ? ApiResponse.failure(ErrorCode.INTERNAL_ERROR, Map.of())
                    : ApiResponse.failure(ErrorCode.HTTP_ERROR, Map.of());
        };
        return super.handleExceptionInternal(exception, failure, headers, status, request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse.Failure> unexpected(Exception exception) {
        log.error("Unhandled API exception type={}", exception.getClass().getName());
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.status())
                .body(ApiResponse.failure(ErrorCode.INTERNAL_ERROR, Map.of()));
    }
}
