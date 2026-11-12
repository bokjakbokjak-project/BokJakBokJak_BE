package kr.bokjakbokjak.auth.google;

/**
 * 검증을 마친 Google ID 토큰의 사용자 정보.
 *
 * @param hostedDomain 학교·회사 Google Workspace 계정의 도메인(hd). 개인 Gmail은 null
 */
public record GoogleIdentity(String subject, String email, boolean emailVerified, String hostedDomain) {
}
