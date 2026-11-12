package kr.bokjakbokjak.auth.config;

import kr.bokjakbokjak.auth.google.GoogleIdTokenVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
class GoogleAuthConfig {

    /** Google 공개키는 첫 로그인 때 받아오고 캐시한다 */
    @Bean
    GoogleIdTokenVerifier googleIdTokenVerifier(AuthProperties properties) {
        return new GoogleIdTokenVerifier(
                NimbusJwtDecoder.withJwkSetUri(GoogleIdTokenVerifier.GOOGLE_JWK_SET_URI).build(),
                properties.googleClientId());
    }
}
