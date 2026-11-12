package kr.bokjakbokjak.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import java.util.List;
import kr.bokjakbokjak.matching.CandidateDto;
import kr.bokjakbokjak.matching.UserQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = "app.matching.candidate-scope=DORMITORY")
@ActiveProfiles("test")
class UserQueryServiceDormitoryScopeTest {
    @Autowired UserQueryService userQueryService;
    @Autowired JdbcClient jdbc;

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    @Test
    void dormitoryScopeIgnoresRoomTypeButKeepsDormitory() {
        long bookmark = jdbc.sql("select id from app_user where test_user = TRUE and nickname = ?")
                .param("책갈피")
                .query(Long.class)
                .single();

        List<String> nicknames = userQueryService.findCandidates(bookmark).stream()
                .map(CandidateDto::nickname)
                .toList();

        assertThat(nicknames).contains("옆방타입", "깔끔이").doesNotContain("옆동주민", "민트초코");
    }
}
