package kr.bokjakbokjak.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Base64;
import java.util.List;
import kr.bokjakbokjak.matching.CandidateDto;
import kr.bokjakbokjak.matching.MatchItemDto;
import kr.bokjakbokjak.matching.MatchLabel;
import kr.bokjakbokjak.matching.MatchResultDto;
import kr.bokjakbokjak.matching.MatchingEngine;
import kr.bokjakbokjak.matching.UserQueryService;
import kr.bokjakbokjak.matching.UserSummaryDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Flyway로 넣은 페르소나 테스트 유저(V6) 기준으로 후보 조회를 확인한다. */
@SpringBootTest
@ActiveProfiles("test")
class UserQueryServiceImplTest {
    @Autowired UserQueryService userQueryService;
    @Autowired MatchingEngine matchingEngine;
    @Autowired JdbcClient jdbc;

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    @Test
    void realServiceIsRegisteredByDefault() {
        assertThat(userQueryService).isInstanceOf(UserQueryServiceImpl.class);
    }

    @Test
    void candidatesShareSchoolGenderAndRoomType() {
        List<String> nicknames = candidateNicknames(persona("책갈피"));

        assertThat(nicknames).contains(
                "깔끔이", "털털이", "아침햇살", "새벽감성", "연기한줄", "드르렁", "무난이", "갸우뚱", "금연중");
        assertThat(nicknames).doesNotContain("책갈피", "옆동주민", "옆방타입", "민트초코");
    }

    @Test
    void candidateCarriesProfileAndTopCategories() {
        CandidateDto candidate = userQueryService.findCandidates(persona("책갈피")).stream()
                .filter(dto -> dto.nickname().equals("깔끔이"))
                .findFirst()
                .orElseThrow();

        assertThat(candidate.userId()).isEqualTo(persona("깔끔이"));
        assertThat(candidate.department()).isEqualTo("화학과");
        assertThat(candidate.entryYear()).isEqualTo(2022);
        assertThat(candidate.birthYear()).isEqualTo(2003);
        assertThat(candidate.topCategories()).containsExactly("CLEANING", "PERSONAL_HYGIENE", "SLEEP_SCHEDULE");
    }

    @Test
    void smokerStaysAsCandidateWithLowerScore() {
        long bookmark = persona("책갈피");
        assertThat(candidateNicknames(bookmark)).contains("연기한줄");

        MatchResultDto smoker = matchingEngine.calculate(bookmark, persona("연기한줄"));
        MatchResultDto sameAnswers = matchingEngine.calculate(bookmark, persona("옆동주민"));
        MatchItemDto smoking = smoker.items().stream()
                .filter(item -> item.topicCode().equals("SMOKING"))
                .findFirst()
                .orElseThrow();

        assertThat(smoking.label()).isEqualTo(MatchLabel.NEED_ADJUST);
        assertThat(smoker.mutualScore()).isLessThan(sameAnswers.mutualScore());
    }

    @Test
    void excludesWithdrawnAndSuspendedUsersFromCandidates() {
        long bookmark = persona("책갈피");
        long withdrawn = copyOf(bookmark, "탈퇴한후보", "WITHDRAWN");
        long suspended = copyOf(bookmark, "정지된후보", "SUSPENDED");
        long active = copyOf(bookmark, "활성후보", "ACTIVE");

        List<Long> ids = userQueryService.findCandidates(bookmark).stream().map(CandidateDto::userId).toList();

        assertThat(ids).contains(active).doesNotContain(withdrawn, suspended);
    }

    @Test
    void summariesKeepRequestOrderAndIncludeWithdrawnUsers() {
        long bookmark = persona("책갈피");
        long withdrawn = copyOf(bookmark, "탈퇴한요약", "WITHDRAWN");

        List<UserSummaryDto> summaries = userQueryService.getUserSummaries(
                List.of(withdrawn, bookmark, -1L, withdrawn));

        assertThat(summaries).extracting(UserSummaryDto::userId).containsExactly(withdrawn, bookmark);
        assertThat(summaries.get(0).status()).isEqualTo("WITHDRAWN");
        UserSummaryDto summary = summaries.get(1);
        assertThat(summary.nickname()).isEqualTo("책갈피");
        assertThat(summary.department()).isEqualTo("산업공학과");
        assertThat(summary.birthYear()).isEqualTo(2004);
        assertThat(summary.status()).isEqualTo("ACTIVE");
        assertThat(summary.surveyRevision()).isEqualTo(1);
        assertThat(summary.topCategories()).containsExactly("CLEANING", "SLEEP_SCHEDULE", "SMOKING");
    }

    @Test
    void viewerWithoutProfileGetsNoCandidates() {
        var keyHolder = new GeneratedKeyHolder();
        jdbc.sql("""
                        insert into app_user (onboarding_completed_at, survey_revision, created_at, updated_at)
                        values (CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)""")
                .update(keyHolder);

        assertThat(userQueryService.findCandidates(keyHolder.getKeyAs(Long.class))).isEmpty();
        assertThrows(IllegalArgumentException.class, () -> userQueryService.findCandidates(-1L));
    }

    private List<String> candidateNicknames(long viewerId) {
        return userQueryService.findCandidates(viewerId).stream().map(CandidateDto::nickname).toList();
    }

    private long persona(String nickname) {
        return jdbc.sql("select id from app_user where test_user = TRUE and nickname = ?")
                .param(nickname)
                .query(Long.class)
                .single();
    }

    /** 원본과 같은 학교·성별·방 타입에 상태만 다른 회원을 만든다. */
    private long copyOf(long sourceId, String nickname, String status) {
        jdbc.sql("""
                        insert into app_user (university_id, nickname, gender, birth_year, department, student_number,
                                              room_type_id, status, onboarding_completed_at, survey_revision,
                                              created_at, updated_at)
                        select university_id, ?, gender, birth_year, department, student_number,
                               room_type_id, ?, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                        from app_user where id = ?""")
                .params(nickname, status, sourceId)
                .update();
        return jdbc.sql("select max(id) from app_user where nickname = ?")
                .param(nickname)
                .query(Long.class)
                .single();
    }
}
