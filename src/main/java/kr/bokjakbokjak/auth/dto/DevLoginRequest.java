package kr.bokjakbokjak.auth.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DevLoginRequest(
        @NotNull(message = "user_id를 입력해주세요.")
        @Positive(message = "user_id는 1 이상이어야 합니다.")
        Long userId) {
}
