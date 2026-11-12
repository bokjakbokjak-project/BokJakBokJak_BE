package kr.bokjakbokjak.auth.token;

import java.time.Duration;
import java.util.Optional;

/** 리프레시 토큰과 로그아웃한 액세스 토큰 차단 목록 저장소. 운영은 Redis를 쓴다. */
public interface AuthTokenStore {

    void saveRefreshToken(String refreshToken, long userId, Duration ttl);

    /** 리프레시 토큰을 꺼내면서 바로 지운다. 한 번 쓴 토큰은 다시 쓸 수 없다 */
    Optional<Long> consumeRefreshToken(String refreshToken);

    /** 본인 리프레시 토큰이면 지운다 */
    void revokeRefreshToken(String refreshToken, long userId);

    /** 남은 만료 시간 동안 액세스 토큰을 차단한다 */
    void blockAccessToken(String tokenId, Duration remaining);

    boolean isAccessTokenBlocked(String tokenId);
}
