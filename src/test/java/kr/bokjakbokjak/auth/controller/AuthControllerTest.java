package kr.bokjakbokjak.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import kr.bokjakbokjak.auth.google.GoogleIdTokenVerifier;
import kr.bokjakbokjak.notification.entity.Notification;
import kr.bokjakbokjak.notification.entity.NotificationType;
import kr.bokjakbokjak.notification.repository.NotificationRepository;
import kr.bokjakbokjak.room.entity.Agenda;
import kr.bokjakbokjak.room.entity.AgendaStatus;
import kr.bokjakbokjak.room.entity.ChatMessage;
import kr.bokjakbokjak.room.entity.Matching;
import kr.bokjakbokjak.room.repository.AgendaRepository;
import kr.bokjakbokjak.room.repository.ChatMessageRepository;
import kr.bokjakbokjak.room.repository.MatchingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(properties = {
        "app.auth.google-client-id=" + AuthControllerTest.CLIENT_ID,
        "app.auth.dev-login-key=" + AuthControllerTest.DEV_KEY})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {
    static final String CLIENT_ID = "test-client.apps.googleusercontent.com";
    static final String DEV_KEY = "test-dev-key";
    private static final RSAKey GOOGLE_KEY = generateKey();

    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    @Autowired MatchingRepository matchingRepository;
    @Autowired AgendaRepository agendaRepository;
    @Autowired ChatMessageRepository chatMessageRepository;
    @Autowired NotificationRepository notificationRepository;

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    /** 실제 Google 공개키 대신 테스트 키로 서명을 검증한다. 발급자·대상·만료 검증은 운영과 같다 */
    @TestConfiguration
    static class TestGoogleKeys {
        @Bean
        @Primary
        GoogleIdTokenVerifier testGoogleIdTokenVerifier() throws JOSEException {
            return new GoogleIdTokenVerifier(
                    NimbusJwtDecoder.withPublicKey(GOOGLE_KEY.toRSAPublicKey()).build(), CLIENT_ID);
        }
    }

    @Test
    void firstLoginCreatesSchoolMemberAndSameAccountReturnsSameMember() throws Exception {
        String sub = newSub();
        String body = login(idToken(sub, sub + "@dgu.ac.kr", "dgu.ac.kr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("LOGIN_SUCCESS"))
                .andExpect(jsonPath("$.data.token_type").value("Bearer"))
                .andExpect(jsonPath("$.data.expires_in").value(1800))
                .andExpect(jsonPath("$.data.refresh_expires_in").value(1209600))
                .andExpect(jsonPath("$.data.onboarding_completed").value(false))
                .andExpect(jsonPath("$.data.survey_completed").value(false))
                .andExpect(jsonPath("$.data.next_step").value("ONBOARDING"))
                .andReturn().getResponse().getContentAsString();

        long userId = ((Number) JsonPath.read(body, "$.data.user_id")).longValue();
        long universityId = ((Number) JsonPath.read(body, "$.data.university_id")).longValue();
        assertThat(universityId).isEqualTo(universityIdOf("DONGGUK"));

        String again = login(idToken(sub, sub + "@dgu.ac.kr", "dgu.ac.kr"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(((Number) JsonPath.read(again, "$.data.user_id")).longValue()).isEqualTo(userId);

        String accessToken = JsonPath.read(body, "$.data.access_token");
        mvc.perform(get("/api/me/survey").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void schoolIsResolvedByEmailDomainWhenHostedDomainIsAnAlias() throws Exception {
        String sub = newSub();
        login(idToken(sub, sub + "@sm.ac.kr", "sookmyung-alias.example"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.university_id").value((int) universityIdOf("SOOKMYUNG")));
    }

    @Test
    void addedSchoolsAreResolvedByGoogleWorkspaceDomain() throws Exception {
        String sub = newSub();
        login(idToken(sub, sub + "@g.skku.edu", "g.skku.edu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.university_id").value((int) universityIdOf("SKKU")));
        String other = newSub();
        login(idToken(other, other + "@yonsei.ac.kr", "yonsei.ac.kr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.university_id").value((int) universityIdOf("YONSEI")));
    }

    @Test
    void rejectsPersonalGmailUnknownSchoolAndUnverifiedEmail() throws Exception {
        login(idToken(newSub(), "someone@gmail.com", null))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_SCHOOL_ACCOUNT"));
        login(idToken(newSub(), "someone@unknown.ac.kr", "unknown.ac.kr"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_SCHOOL_ACCOUNT"));
        String sub = newSub();
        login(sign(claims(sub, sub + "@dgu.ac.kr", "dgu.ac.kr").claim("email_verified", false).build()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_SCHOOL_ACCOUNT"));
    }

    @Test
    void rejectsWrongAudienceIssuerExpiryAndForgedToken() throws Exception {
        String sub = newSub();
        String email = sub + "@dgu.ac.kr";
        login(sign(claims(sub, email, "dgu.ac.kr").audience("other-client").build()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_GOOGLE_ID_TOKEN"));
        login(sign(claims(sub, email, "dgu.ac.kr").issuer("https://evil.example").build()))
                .andExpect(status().isUnauthorized());
        login(sign(claims(sub, email, "dgu.ac.kr")
                .expirationTime(Date.from(Instant.now().minusSeconds(300))).build()))
                .andExpect(status().isUnauthorized());
        login("not-a-jwt")
                .andExpect(status().isUnauthorized());
        login("")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.id_token").exists());
    }

    @Test
    void rejectsSuspendedMemberAndEmailOwnedByAnotherAccount() throws Exception {
        String sub = newSub();
        long userId = loginUserId(sub);
        jdbc.sql("update app_user set status = 'SUSPENDED' where id = ?").param(userId).update();
        login(idToken(sub, sub + "@dgu.ac.kr", "dgu.ac.kr"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_UNAVAILABLE"));

        String other = newSub();
        login(idToken(other, sub + "@dgu.ac.kr", "dgu.ac.kr"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LINK_REQUIRED"));
    }

    @Test
    void refreshRotatesTokensAndUsedRefreshTokenCannotBeReused() throws Exception {
        String body = login(idToken(newSub(), null, "dgu.ac.kr")).andReturn().getResponse().getContentAsString();
        String refreshToken = JsonPath.read(body, "$.data.refresh_token");

        String refreshed = refresh(refreshToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TOKEN_REFRESH_SUCCESS"))
                .andExpect(jsonPath("$.data.access_token").isString())
                .andReturn().getResponse().getContentAsString();
        assertThat((String) JsonPath.read(refreshed, "$.data.refresh_token")).isNotEqualTo(refreshToken);

        refresh(refreshToken)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
        refresh(JsonPath.read(refreshed, "$.data.refresh_token"))
                .andExpect(status().isOk());
    }

    @Test
    void logoutBlocksAccessTokenAndRevokesRefreshToken() throws Exception {
        String body = login(idToken(newSub(), null, "dgu.ac.kr")).andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(body, "$.data.access_token");
        String refreshToken = JsonPath.read(body, "$.data.refresh_token");

        mvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refresh_token\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("LOGOUT_SUCCESS"));

        mvc.perform(get("/api/me/survey").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
        refresh(refreshToken).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refresh_token\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void devLoginIssuesTokensOnlyForTestUsersWithTheKey() throws Exception {
        long bookmark = jdbc.sql("select id from app_user where test_user = TRUE and nickname = ?")
                .param("책갈피").query(Long.class).single();

        devLogin(DEV_KEY, bookmark)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user_id").value((int) bookmark))
                .andExpect(jsonPath("$.data.next_step").value("HOME"));
        devLogin("wrong-key", bookmark)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_DEV_LOGIN_KEY"));
        devLogin(null, bookmark)
                .andExpect(status().isUnauthorized());
        devLogin(DEV_KEY, loginUserId(newSub()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_TEST_USER"));
    }

    @Test
    void devLoginFindsTestUserByNickname() throws Exception {
        long bookmark = personaId("책갈피");

        devLogin(DEV_KEY, "{\"nickname\":\" 책갈피 \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user_id").value((int) bookmark));
        devLogin(DEV_KEY, "{\"nickname\":\"없는닉네임\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_TEST_USER"));
        devLogin(DEV_KEY, "{\"user_id\":" + bookmark + ",\"nickname\":\"책갈피\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        devLogin(DEV_KEY, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void devLoginRejectsDuplicatedTestNickname() throws Exception {
        long mint = personaId("민트초코");
        jdbc.sql("update app_user set nickname = ? where id = ?").params("책갈피", mint).update();
        try {
            devLogin(DEV_KEY, "{\"nickname\":\"책갈피\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        } finally {
            jdbc.sql("update app_user set nickname = ? where id = ?").params("민트초코", mint).update();
        }
    }

    @Test
    void browserCanSendDevLoginKeyHeader() throws Exception {
        mvc.perform(options("/api/auth/dev-login")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type,x-dev-login-key"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    @Test
    void devResetDeletesOnlyMatchingsBetweenTestUsers() throws Exception {
        long bookmark = personaId("책갈피");
        long mint = personaId("민트초코");
        long realUser = loginUserId(newSub());
        Long testPair = seedMatching(bookmark, mint);
        Long withRealUser = seedMatching(realUser, bookmark);
        try {
            devReset("wrong-key").andExpect(status().isUnauthorized());

            devReset(DEV_KEY)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.deleted_matchings").value(1))
                    .andExpect(jsonPath("$.data.deleted_messages").value(1))
                    .andExpect(jsonPath("$.data.deleted_agendas").value(1))
                    .andExpect(jsonPath("$.data.deleted_notifications").value(1));

            assertThat(matchingRepository.existsById(testPair)).isFalse();
            assertThat(matchingRepository.existsById(withRealUser)).isTrue();
            assertThat(countByMatching("chat_message", withRealUser)).isEqualTo(1);
            assertThat(countByMatching("agenda", withRealUser)).isEqualTo(1);
            assertThat(countByMatching("notification", withRealUser)).isEqualTo(1);
        } finally {
            for (String table : List.of("chat_message", "agenda", "notification", "matching")) {
                jdbc.sql("delete from " + table + " where matching_id = ?").param(withRealUser).update();
            }
        }
    }

    private Long seedMatching(long requesterId, long receiverId) {
        Long matchingId = matchingRepository.save(Matching.start(requesterId, receiverId, 1, 1)).getId();
        Long agendaId = agendaRepository.save(
                Agenda.create(matchingId, 1, "BEDTIME", "취침 시간", "몇 시에 잘까요?", AgendaStatus.DISCUSSING)).getId();
        chatMessageRepository.save(ChatMessage.system(matchingId, "대화가 시작됐어요.", agendaId));
        notificationRepository.save(
                Notification.create(receiverId, matchingId, NotificationType.CHAT_REQUESTED, "대화 신청이 왔어요."));
        return matchingId;
    }

    private long countByMatching(String table, Long matchingId) {
        return jdbc.sql("select count(*) from " + table + " where matching_id = ?")
                .param(matchingId).query(Long.class).single();
    }

    private ResultActions devReset(String key) throws Exception {
        return mvc.perform(post("/api/auth/dev-reset").header("X-Dev-Login-Key", key));
    }

    private long personaId(String nickname) {
        return jdbc.sql("select id from app_user where test_user = TRUE and nickname = ?")
                .param(nickname).query(Long.class).single();
    }

    private long loginUserId(String sub) throws Exception {
        String body = login(idToken(sub, sub + "@dgu.ac.kr", "dgu.ac.kr"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.user_id")).longValue();
    }

    private ResultActions login(String idToken) throws Exception {
        return mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id_token\":\"" + idToken + "\"}"));
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refresh_token\":\"" + refreshToken + "\"}"));
    }

    private ResultActions devLogin(String key, long userId) throws Exception {
        return devLogin(key, "{\"user_id\":" + userId + "}");
    }

    private ResultActions devLogin(String key, String body) throws Exception {
        var request = post("/api/auth/dev-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        if (key != null) {
            request.header("X-Dev-Login-Key", key);
        }
        return mvc.perform(request);
    }

    private long universityIdOf(String code) {
        return jdbc.sql("select id from university where code = ?").param(code).query(Long.class).single();
    }

    private static String newSub() {
        return "google-" + UUID.randomUUID();
    }

    /** email이 null이면 sub로 학교 이메일을 만든다 */
    private static String idToken(String sub, String email, String hostedDomain) {
        String address = email != null ? email : sub + "@" + hostedDomain;
        return sign(claims(sub, address, hostedDomain).build());
    }

    private static JWTClaimsSet.Builder claims(String sub, String email, String hostedDomain) {
        var builder = new JWTClaimsSet.Builder()
                .issuer("https://accounts.google.com")
                .audience(CLIENT_ID)
                .subject(sub)
                .claim("email", email)
                .claim("email_verified", true)
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plusSeconds(600)));
        if (hostedDomain != null) {
            builder.claim("hd", hostedDomain);
        }
        return builder;
    }

    private static String sign(JWTClaimsSet claims) {
        try {
            var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(GOOGLE_KEY.getKeyID()).build(),
                    claims);
            jwt.sign(new RSASSASigner(GOOGLE_KEY));
            return jwt.serialize();
        } catch (JOSEException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static RSAKey generateKey() {
        try {
            return new RSAKeyGenerator(2048).keyID("test-google-key").generate();
        } catch (JOSEException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
