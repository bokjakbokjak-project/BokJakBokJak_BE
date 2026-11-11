package kr.bokjakbokjak.room.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ChatRequestRequest(
        @NotNull(message = "대화를 신청할 사용자를 입력해 주세요.")
        @Positive(message = "대화를 신청할 사용자를 입력해 주세요.")
        Long receiverId) {
}
