package kr.bokjakbokjak.auth.dto;

/** 테스트 계정 초기화로 지운 건수 */
public record DevResetResponse(
        int deletedMatchings,
        int deletedMessages,
        int deletedAgendas,
        int deletedNotifications) {
}
