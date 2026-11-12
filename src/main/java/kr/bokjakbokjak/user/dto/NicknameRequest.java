package kr.bokjakbokjak.user.dto;

/** 형식 검증은 서비스에서 하고 INVALID_NICKNAME으로 응답한다. */
public record NicknameRequest(String nickname) {
}
