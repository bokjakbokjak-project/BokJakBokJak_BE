package kr.bokjakbokjak.common;

import java.util.Map;
import org.springframework.http.HttpStatus;

public final class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final Map<String, String> errors;
    private final Long retryAfterSeconds;

    public ApiException(ErrorCode code) {
        this(code, Map.of());
    }

    public ApiException(ErrorCode code, Map<String, String> errors) {
        this(code.status(), code.name(), code.message(), errors);
    }

    public ApiException(HttpStatus status, String code, String message) {
        this(status, code, message, Map.of());
    }

    public ApiException(HttpStatus status, String code, String message, Map<String, String> errors) {
        this(status, code, message, errors, null);
    }

    private ApiException(HttpStatus status, String code, String message, Map<String, String> errors,
            Long retryAfterSeconds) {
        super(message);
        this.status = status;
        this.code = code;
        this.errors = Map.copyOf(errors);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public static ApiException rateLimited(long retryAfterSeconds) {
        return new ApiException(ErrorCode.RATE_LIMITED.status(), ErrorCode.RATE_LIMITED.name(),
                ErrorCode.RATE_LIMITED.message(),
                Map.of(), Math.max(1, retryAfterSeconds));
    }

    Long retryAfterSeconds() {
        return retryAfterSeconds;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public Map<String, String> errors() {
        return errors;
    }
}
