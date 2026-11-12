package kr.bokjakbokjak.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import kr.bokjakbokjak.auth.config.AuthProperties;
import kr.bokjakbokjak.auth.dto.LoginResponse;
import kr.bokjakbokjak.auth.dto.TokenResponse;
import kr.bokjakbokjak.auth.google.GoogleIdTokenVerifier;
import kr.bokjakbokjak.auth.google.GoogleIdentity;
import kr.bokjakbokjak.auth.token.AuthTokenStore;
import kr.bokjakbokjak.common.ApiException;
import kr.bokjakbokjak.common.ErrorCode;
import kr.bokjakbokjak.common.JwtService;
import kr.bokjakbokjak.university.repository.UniversityDomainRepository;
import kr.bokjakbokjak.user.entity.User;
import kr.bokjakbokjak.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final GoogleIdTokenVerifier googleVerifier;
    private final UniversityDomainRepository domainRepository;
    private final UserRepository userRepository;
    private final UserRegistrar userRegistrar;
    private final JwtService jwtService;
    private final AuthTokenStore tokenStore;
    private final AuthProperties properties;

    public AuthService(
            GoogleIdTokenVerifier googleVerifier,
            UniversityDomainRepository domainRepository,
            UserRepository userRepository,
            UserRegistrar userRegistrar,
            JwtService jwtService,
            AuthTokenStore tokenStore,
            AuthProperties properties) {
        this.googleVerifier = googleVerifier;
        this.domainRepository = domainRepository;
        this.userRepository = userRepository;
        this.userRegistrar = userRegistrar;
        this.jwtService = jwtService;
        this.tokenStore = tokenStore;
        this.properties = properties;
    }

    /** Google 학교 계정으로 로그인한다. 처음이면 회원을 만든다. */
    public LoginResponse login(String idToken) {
        GoogleIdentity identity = googleVerifier.verify(idToken);
        Long universityId = resolveUniversity(identity);
        User user = userRepository.findByGoogleSub(identity.subject())
                .orElseGet(() -> register(universityId, identity));
        return issue(requireActive(user));
    }

    /** 리프레시 토큰으로 두 토큰을 모두 새로 발급한다. 쓴 리프레시 토큰은 바로 폐기된다. */
    public TokenResponse refresh(String refreshToken) {
        long userId = tokenStore.consumeRefreshToken(refreshToken)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REFRESH_TOKEN));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REFRESH_TOKEN));
        return issueTokens(requireActive(user));
    }

    /** 현재 액세스 토큰을 남은 시간 동안 차단하고 리프레시 토큰을 지운다. */
    public void logout(JwtService.AccessClaims accessClaims, String refreshToken) {
        tokenStore.blockAccessToken(accessClaims.tokenId(), Duration.between(Instant.now(), accessClaims.expiresAt()));
        tokenStore.revokeRefreshToken(refreshToken, accessClaims.userId());
    }

    /** 개발·시연용. 키가 맞고 테스트 유저일 때만 토큰을 준다. */
    public LoginResponse devLogin(String key, Long userId) {
        if (!properties.devLoginEnabled()) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (key == null || !MessageDigest.isEqual(
                key.getBytes(StandardCharsets.UTF_8), properties.devLoginKey().getBytes(StandardCharsets.UTF_8))) {
            throw new ApiException(ErrorCode.INVALID_DEV_LOGIN_KEY);
        }
        User user = userRepository.findById(userId)
                .filter(User::isTestUser)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_TEST_USER));
        return issue(requireActive(user));
    }

    /** hd가 있어야 학교·회사 Google Workspace 계정이다. hd나 이메일 도메인 중 하나가 등록된 학교면 통과 */
    private Long resolveUniversity(GoogleIdentity identity) {
        if (!identity.emailVerified() || identity.hostedDomain() == null || identity.email() == null) {
            throw new ApiException(ErrorCode.UNSUPPORTED_SCHOOL_ACCOUNT);
        }
        String hostedDomain = identity.hostedDomain().toLowerCase(Locale.ROOT);
        String emailDomain = identity.email().substring(identity.email().indexOf('@') + 1).toLowerCase(Locale.ROOT);
        return domainRepository.findActiveUniversityId(hostedDomain)
                .or(() -> domainRepository.findActiveUniversityId(emailDomain))
                .orElseThrow(() -> {
                    log.info("Unsupported school account: hd={}, emailDomain={}", hostedDomain, emailDomain);
                    return new ApiException(ErrorCode.UNSUPPORTED_SCHOOL_ACCOUNT);
                });
    }

    private User register(Long universityId, GoogleIdentity identity) {
        if (userRepository.existsByEmail(identity.email())) {
            throw new ApiException(ErrorCode.ACCOUNT_LINK_REQUIRED);
        }
        try {
            return userRegistrar.register(universityId, identity.subject(), identity.email());
        } catch (DataIntegrityViolationException exception) {
            // 같은 계정의 첫 로그인이 동시에 들어오면 먼저 만든 회원을 쓴다
            return userRepository.findByGoogleSub(identity.subject()).orElseThrow(() -> exception);
        }
    }

    private static User requireActive(User user) {
        if (!user.isActive()) {
            throw new ApiException(ErrorCode.ACCOUNT_UNAVAILABLE);
        }
        return user;
    }

    private LoginResponse issue(User user) {
        return LoginResponse.of(user, issueTokens(user));
    }

    private TokenResponse issueTokens(User user) {
        JwtService.AccessToken access = jwtService.issueAccessToken(user.getId());
        String refreshToken = newRefreshToken();
        tokenStore.saveRefreshToken(refreshToken, user.getId(), properties.refreshTtl());
        return new TokenResponse(
                access.accessToken(),
                access.tokenType(),
                access.expiresIn(),
                refreshToken,
                properties.refreshTtl().toSeconds());
    }

    private static String newRefreshToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
