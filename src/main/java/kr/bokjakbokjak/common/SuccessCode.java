package kr.bokjakbokjak.common;

public enum SuccessCode {
    COMMON2000("요청에 성공했습니다."),
    SURVEY_SCHEMA_SUCCESS("설문 문항을 조회했습니다."),
    SURVEY_SAVE_SUCCESS("설문을 저장했습니다."),
    MY_SURVEY_SUCCESS("내 설문 응답을 조회했습니다."),
    LOGIN_SUCCESS("로그인했습니다."),
    TOKEN_REFRESH_SUCCESS("토큰을 재발급했습니다."),
    LOGOUT_SUCCESS("로그아웃했습니다."),

    RECOMMENDATION_LIST_SUCCESS("추천 리스트를 조회했습니다."),
    RECOMMENDATION_REFRESH_SUCCESS("추천 리스트를 새로고침했습니다."),
    MATCH_ANALYSIS_SUCCESS("AI 매칭 분석을 조회했습니다."),
    LIFESTYLE_SUMMARY_SUCCESS("생활습관 요약을 조회했습니다."),
    CHAT_REQUEST_SUCCESS("대화를 신청했습니다."),
    CHAT_ROOMS_SUCCESS("채팅방 목록을 조회했습니다."),
    CHAT_ROOM_SUCCESS("채팅방 정보를 조회했습니다."),
    NEW_MESSAGES_SUCCESS("새 메시지를 조회했습니다."),
    PREVIOUS_MESSAGES_SUCCESS("이전 메시지를 조회했습니다."),
    MESSAGE_SEND_SUCCESS("메시지를 보냈습니다."),
    AGENDA_DECIDE_SUCCESS("안건을 확정했습니다."),
    MATCH_END_SUCCESS("매칭을 중지했습니다."),
    MATCH_CONFIRM_SUCCESS("최종 매칭을 선택했습니다."),
    NOTIFICATIONS_SUCCESS("알림 목록을 조회했습니다."),
    NOTIFICATION_READ_SUCCESS("알림을 읽음 처리했습니다."),
    UNREAD_COUNT_SUCCESS("안 읽은 알림 개수를 조회했습니다.");

    private final String message;

    SuccessCode(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
