package kr.bokjakbokjak.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Google ID 토큰을 입력해주세요.")
        String idToken) {
}
