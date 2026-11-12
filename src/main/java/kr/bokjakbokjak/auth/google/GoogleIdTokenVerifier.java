package kr.bokjakbokjak.auth.google;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import kr.bokjakbokjak.common.ApiException;
import kr.bokjakbokjak.common.ErrorCode;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/** Google ID 토큰의 서명·발급자(iss)·대상(aud)·만료(exp)를 검증한다. */
public class GoogleIdTokenVerifier {
    public static final String GOOGLE_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> GOOGLE_ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");

    private final JwtDecoder decoder;

    public GoogleIdTokenVerifier(NimbusJwtDecoder decoder, String clientId) {
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                claim("iss", jwt -> GOOGLE_ISSUERS.contains(String.valueOf(jwt.getClaims().get("iss")))),
                claim("aud", jwt -> clientId != null && !clientId.isBlank()
                        && audience(jwt).contains(clientId))));
        this.decoder = decoder;
    }

    public GoogleIdentity verify(String idToken) {
        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (JwtException exception) {
            throw new ApiException(ErrorCode.INVALID_GOOGLE_ID_TOKEN);
        }
        return new GoogleIdentity(
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")),
                jwt.getClaimAsString("hd"));
    }

    private static List<String> audience(Jwt jwt) {
        List<String> audience = jwt.getAudience();
        return audience == null ? List.of() : audience;
    }

    private static OAuth2TokenValidator<Jwt> claim(String name, Predicate<Jwt> test) {
        OAuth2Error error = new OAuth2Error("invalid_token", "Invalid Google ID token claim: " + name, null);
        return jwt -> test.test(jwt) ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(error);
    }
}
