package kr.bokjakbokjak.support;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import kr.bokjakbokjak.auth.token.AuthTokenStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** 테스트에는 Redis가 없어서 메모리에 저장한다. 만료 시간은 다루지 않는다. */
@Component
@Profile("test")
public class InMemoryAuthTokenStore implements AuthTokenStore {
    private final Map<String, Long> refreshTokens = new ConcurrentHashMap<>();
    private final Set<String> blockedTokenIds = ConcurrentHashMap.newKeySet();

    @Override
    public void saveRefreshToken(String refreshToken, long userId, Duration ttl) {
        refreshTokens.put(refreshToken, userId);
    }

    @Override
    public Optional<Long> consumeRefreshToken(String refreshToken) {
        return Optional.ofNullable(refreshTokens.remove(refreshToken));
    }

    @Override
    public void revokeRefreshToken(String refreshToken, long userId) {
        refreshTokens.remove(refreshToken, userId);
    }

    @Override
    public void blockAccessToken(String tokenId, Duration remaining) {
        if (tokenId != null && remaining.isPositive()) {
            blockedTokenIds.add(tokenId);
        }
    }

    @Override
    public boolean isAccessTokenBlocked(String tokenId) {
        return blockedTokenIds.contains(tokenId);
    }
}
