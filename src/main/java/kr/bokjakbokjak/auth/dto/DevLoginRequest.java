package kr.bokjakbokjak.auth.dto;

import jakarta.validation.constraints.Positive;

/** user_id와 nickname 중 하나만 보낸다. 닉네임은 환경마다 ID가 달라도 같은 페르소나로 로그인할 때 쓴다. */
public record DevLoginRequest(
        @Positive(message = "user_id는 1 이상이어야 합니다.")
        Long userId,
        String nickname) {
}
