package kr.bokjakbokjak.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요하거나 인증이 만료되었습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "이 요청에 접근할 수 없습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 정보를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식입니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 데이터 형식입니다."),
    SURVEY_REVISION_CONFLICT(HttpStatus.CONFLICT, "최신 설문을 조회해주세요."),
    ONBOARDING_REQUIRED(HttpStatus.CONFLICT, "온보딩을 먼저 완료해주세요."),
    INVALID_GOOGLE_ID_TOKEN(HttpStatus.UNAUTHORIZED, "Google 로그인을 다시 시도해주세요."),
    UNSUPPORTED_SCHOOL_ACCOUNT(HttpStatus.FORBIDDEN, "지원하는 학교 Google 계정으로 로그인해주세요."),
    ACCOUNT_UNAVAILABLE(HttpStatus.FORBIDDEN, "현재 사용할 수 없는 계정입니다."),
    ACCOUNT_LINK_REQUIRED(HttpStatus.CONFLICT, "기존 계정과 연결을 확인해주세요."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "다시 로그인해주세요."),
    INVALID_DEV_LOGIN_KEY(HttpStatus.UNAUTHORIZED, "테스트 로그인 키가 올바르지 않습니다."),
    NOT_TEST_USER(HttpStatus.FORBIDDEN, "테스트 유저만 로그인할 수 있습니다."),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "잠시 후 다시 시도해주세요."),
    HTTP_ERROR(HttpStatus.BAD_REQUEST, "요청을 처리할 수 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "요청을 처리할 수 없습니다."),

    CANNOT_REQUEST_SELF(HttpStatus.BAD_REQUEST, "자기 자신에게는 대화를 신청할 수 없습니다."),
    TARGET_NOT_ELIGIBLE(HttpStatus.FORBIDDEN, "대화를 신청할 수 없는 사용자입니다."),
    TARGET_WITHDRAWN(HttpStatus.NOT_FOUND, "탈퇴했거나 존재하지 않는 사용자입니다."),
    ALREADY_CHATTING(HttpStatus.CONFLICT, "이미 대화 중인 상대입니다."),
    MATCHING_ENDED_PAIR(HttpStatus.CONFLICT, "매칭을 중지한 상대에게는 다시 신청할 수 없습니다."),
    ALREADY_MATCHED(HttpStatus.CONFLICT, "이미 최종 매칭이 완료된 사용자입니다."),
    MATCHING_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 매칭입니다."),
    MATCHING_FORBIDDEN(HttpStatus.FORBIDDEN, "참여하지 않은 매칭입니다."),
    INVALID_MATCHING_STATUS(HttpStatus.CONFLICT, "현재 매칭 상태에서는 할 수 없는 요청입니다."),
    AGENDA_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 안건입니다."),
    AGENDA_NOT_STARTED(HttpStatus.CONFLICT, "아직 진행 중인 안건이 아닙니다."),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 알림입니다."),
    NOTIFICATION_FORBIDDEN(HttpStatus.FORBIDDEN, "내 알림이 아닙니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }
}
