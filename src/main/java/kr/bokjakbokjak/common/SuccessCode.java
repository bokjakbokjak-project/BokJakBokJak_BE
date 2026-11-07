package kr.bokjakbokjak.common;

public enum SuccessCode {
    COMMON2000("요청에 성공했습니다.");

    private final String message;

    SuccessCode(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
