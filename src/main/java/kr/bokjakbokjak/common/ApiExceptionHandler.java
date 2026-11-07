package kr.bokjakbokjak.common;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
    private static final String INVALID_INPUT = "요청 값이 올바르지 않습니다.";

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
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.failure("FORBIDDEN", "이 요청에 접근할 수 없습니다.", Map.of()));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            String name = PropertyNamingStrategies.SNAKE_CASE.nameForField(null, null, error.getField());
            errors.putIfAbsent(name, error.getDefaultMessage() == null ? INVALID_INPUT : error.getDefaultMessage());
        }
        return handleExceptionInternal(exception, ApiResponse.failure("INVALID_INPUT", INVALID_INPUT, errors),
                headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(exception, ApiResponse.failure("INVALID_INPUT", INVALID_INPUT, Map.of()),
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
            case 400 -> ApiResponse.failure("INVALID_INPUT", INVALID_INPUT, Map.of());
            case 401 -> ApiResponse.failure("UNAUTHORIZED", "로그인이 필요합니다.", Map.of());
            case 403 -> ApiResponse.failure("FORBIDDEN", "이 요청에 접근할 수 없습니다.", Map.of());
            case 404 -> ApiResponse.failure("RESOURCE_NOT_FOUND", "요청한 정보를 찾을 수 없습니다.", Map.of());
            case 405 -> ApiResponse.failure("METHOD_NOT_ALLOWED", "지원하지 않는 요청 방식입니다.", Map.of());
            case 415 -> ApiResponse.failure("UNSUPPORTED_MEDIA_TYPE", "지원하지 않는 데이터 형식입니다.", Map.of());
            default -> status.is5xxServerError()
                    ? ApiResponse.failure("INTERNAL_ERROR", "요청을 처리할 수 없습니다.", Map.of())
                    : ApiResponse.failure("HTTP_ERROR", "요청을 처리할 수 없습니다.", Map.of());
        };
        return super.handleExceptionInternal(exception, failure, headers, status, request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse.Failure> unexpected(Exception exception) {
        log.error("Unhandled API exception type={}", exception.getClass().getName());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("INTERNAL_ERROR", "요청을 처리할 수 없습니다.", Map.of()));
    }
}
