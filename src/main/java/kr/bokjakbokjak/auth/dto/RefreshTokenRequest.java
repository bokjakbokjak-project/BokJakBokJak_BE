package kr.bokjakbokjak.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** 토큰 재발급과 로그아웃에서 쓴다. */
public record RefreshTokenRequest(
        @NotBlank(message = "리프레시 토큰을 입력해주세요.")
        String refreshToken) {
}
