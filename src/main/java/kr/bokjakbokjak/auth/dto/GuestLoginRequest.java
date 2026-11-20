package kr.bokjakbokjak.auth.dto;

/** 체험 로그인. 형식 검증은 서비스에서 하고 INVALID_NICKNAME으로 응답한다 */
public record GuestLoginRequest(String nickname) {
}
