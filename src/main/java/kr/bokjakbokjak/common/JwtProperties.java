package kr.bokjakbokjak.common;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank String secretBase64,
        @NotBlank String issuer,
        @NotBlank String audience,
        @NotNull Duration ttl) {
}
