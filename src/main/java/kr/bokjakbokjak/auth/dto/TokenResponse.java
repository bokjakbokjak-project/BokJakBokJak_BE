package kr.bokjakbokjak.auth.dto;

/** expires_in, refresh_expires_in은 초 단위 */
public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken,
        long refreshExpiresIn) {
}
