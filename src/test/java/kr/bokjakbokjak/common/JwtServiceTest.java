package kr.bokjakbokjak.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
    private final byte[] secret = new byte[32];
    private final JwtService service = serviceFor(secret, "bokjakbokjak-api");

    @Test
    void requiresValidSignatureIssuerAudienceAndExpiry() {
        assertEquals(42, service.readUserId(service.issueAccessToken(42).accessToken()));
        byte[] differentSecret = secret.clone();
        differentSecret[0] = 1;
        assertThrows(JwtException.class,
                () -> service.readUserId(serviceFor(differentSecret, "bokjakbokjak-api")
                        .issueAccessToken(42).accessToken()));
        assertThrows(JwtException.class,
                () -> service.readUserId(serviceFor(secret, "other-api").issueAccessToken(42).accessToken()));
        assertThrows(JwtException.class, () -> service.readUserId(token("other-issuer", Instant.now().plusSeconds(30))));
        assertThrows(JwtException.class, () -> service.readUserId(token("bokjakbokjak", Instant.now().minusSeconds(60))));
        assertThrows(IllegalArgumentException.class, () -> service.readUserId(token("bokjakbokjak", null)));
    }

    private static JwtService serviceFor(byte[] key, String audience) {
        return new JwtService(new JwtProperties(Base64.getEncoder().encodeToString(key),
                "bokjakbokjak", audience, Duration.ofMinutes(30)));
    }

    private String token(String issuer, Instant expiry) {
        var builder = Jwts.builder().subject("42").issuer(issuer)
                .audience().add("bokjakbokjak-api").and().claim("token_use", "access");
        if (expiry != null) {
            builder.expiration(Date.from(expiry));
        }
        return builder.signWith(new SecretKeySpec(secret, "HmacSHA256"), Jwts.SIG.HS256).compact();
    }
}
