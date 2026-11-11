package kr.bokjakbokjak.common;

import java.util.Map;

public final class ApiResponse {
    private ApiResponse() {
    }

    public record Success<T>(boolean success, String code, String message, T data) {
    }

    public record Failure(boolean success, String code, String message, Map<String, String> errors) {
    }

    public static <T> Success<T> success(T data) {
        return success(SuccessCode.COMMON2000.name(), SuccessCode.COMMON2000.message(), data);
    }

    public static <T> Success<T> success(SuccessCode code, T data) {
        return success(code.name(), code.message(), data);
    }

    public static <T> Success<T> success(String code, String message, T data) {
        return new Success<>(true, code, message, data);
    }

    public static Failure failure(String code, String message, Map<String, String> errors) {
        return new Failure(false, code, message, Map.copyOf(errors));
    }

    public static Failure failure(ErrorCode code, Map<String, String> errors) {
        return failure(code.name(), code.message(), errors);
    }
}
