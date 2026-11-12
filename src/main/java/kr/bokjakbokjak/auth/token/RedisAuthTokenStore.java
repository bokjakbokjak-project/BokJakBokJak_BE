package kr.bokjakbokjak.auth.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** 리프레시 토큰은 원문 대신 SHA-256 해시를 키로 저장한다. */
@Component
@Profile("!test")
class RedisAuthTokenStore implements AuthTokenStore {
    private static final Logger log = LoggerFactory.getLogger(RedisAuthTokenStore.class);
    private static final String REFRESH_PREFIX = "auth:refresh:";
    private static final String BLOCKED_PREFIX = "auth:blocked:";

    private final StringRedisTemplate redis;

    RedisAuthTokenStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void saveRefreshToken(String refreshToken, long userId, Duration ttl) {
        redis.opsForValue().set(refreshKey(refreshToken), Long.toString(userId), ttl);
    }

    @Override
    public Optional<Long> consumeRefreshToken(String refreshToken) {
        return Optional.ofNullable(redis.opsForValue().getAndDelete(refreshKey(refreshToken))).map(Long::valueOf);
    }

    @Override
    public void revokeRefreshToken(String refreshToken, long userId) {
        String key = refreshKey(refreshToken);
        if (Long.toString(userId).equals(redis.opsForValue().get(key))) {
            redis.delete(key);
        }
    }

    @Override
    public void blockAccessToken(String tokenId, Duration remaining) {
        if (tokenId != null && remaining.isPositive()) {
            redis.opsForValue().set(BLOCKED_PREFIX + tokenId, "1", remaining);
        }
    }

    /** Redis 장애 때 모든 요청이 막히지 않도록 차단 여부 확인은 실패해도 통과시킨다 */
    @Override
    public boolean isAccessTokenBlocked(String tokenId) {
        try {
            return Boolean.TRUE.equals(redis.hasKey(BLOCKED_PREFIX + tokenId));
        } catch (DataAccessException exception) {
            log.warn("Access token blocklist check failed", exception);
            return false;
        }
    }

    private static String refreshKey(String refreshToken) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return REFRESH_PREFIX + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
