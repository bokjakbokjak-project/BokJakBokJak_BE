package kr.bokjakbokjak.room.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank(message = "메시지는 1자 이상 500자 이하로 입력해 주세요.")
        @Size(max = 500, message = "메시지는 1자 이상 500자 이하로 입력해 주세요.")
        String content) {
}
