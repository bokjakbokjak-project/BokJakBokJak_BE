package kr.bokjakbokjak.common;

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public final class JwtService {
    private final SecretKey key;
    private final JwtParser parser;
    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        byte[] secret = Base64.getDecoder().decode(properties.secretBase64());
        if (secret.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET_BASE64 must encode at least 32 bytes");
        }
        if (properties.ttl().toSeconds() < 1 || properties.ttl().toHours() >= 24) {
            throw new IllegalArgumentException("JWT_ACCESS_TTL must be at least 1 second and under 24 hours");
        }

        this.key = new SecretKeySpec(secret, "HmacSHA256");
        this.properties = properties;
        this.parser = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(properties.issuer())
                .requireAudience(properties.audience())
                .require("token_use", "access")
                .build();
    }

    public AccessToken issueAccessToken(long userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        Instant now = Instant.now();
        String value = Jwts.builder()
                .issuer(properties.issuer())
                .subject(Long.toString(userId))
                .audience().add(properties.audience()).and()
                .claim("token_use", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.ttl())))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        return new AccessToken(value, "Bearer", properties.ttl().toSeconds());
    }

    long readUserId(String token) {
        var signed = parser.parseSignedClaims(token);
        if (!Jwts.SIG.HS256.getId().equals(signed.getHeader().getAlgorithm())) {
            throw new IllegalArgumentException("Invalid access token algorithm");
        }
        var claims = signed.getPayload();
        if (claims.getExpiration() == null) {
            throw new IllegalArgumentException("Access token requires an expiration");
        }
        long userId = Long.parseLong(claims.getSubject());
        if (userId <= 0) {
            throw new IllegalArgumentException("Invalid token subject");
        }
        return userId;
    }

    public record AccessToken(String accessToken, String tokenType, long expiresIn) {
    }
}
