package kr.bokjakbokjak.common;

public enum SuccessCode {
    COMMON2000("요청에 성공했습니다."),
    SURVEY_SCHEMA_SUCCESS("설문 문항을 조회했습니다."),
    SURVEY_SAVE_SUCCESS("설문을 저장했습니다."),
    MY_SURVEY_SUCCESS("내 설문 응답을 조회했습니다.");

    private final String message;

    SuccessCode(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
