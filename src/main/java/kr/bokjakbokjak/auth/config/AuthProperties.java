package kr.bokjakbokjak.auth.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * @param googleClientId Google OAuth 웹 클라이언트 ID. 비어 있으면 Google 로그인이 모두 실패한다
 * @param refreshTtl 리프레시 토큰 유효 기간
 * @param devLoginKey 테스트 로그인 키. 비어 있으면 테스트 로그인 API가 꺼진다
 * @param guestLoginEnabled 이름만으로 체험 계정을 만드는 체험 로그인을 열지 (심사·데모용)
 */
@Validated
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        String googleClientId,
        @NotNull Duration refreshTtl,
        String devLoginKey,
        @DefaultValue("true") boolean guestLoginEnabled) {

    public boolean devLoginEnabled() {
        return devLoginKey != null && !devLoginKey.isBlank();
    }
}
