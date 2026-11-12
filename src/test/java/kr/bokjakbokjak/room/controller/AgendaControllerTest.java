package kr.bokjakbokjak.room.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;
import kr.bokjakbokjak.common.JwtService;
import kr.bokjakbokjak.room.entity.Agenda;
import kr.bokjakbokjak.room.entity.AgendaStatus;
import kr.bokjakbokjak.room.entity.Matching;
import kr.bokjakbokjak.room.repository.AgendaRepository;
import kr.bokjakbokjak.room.repository.ChatMessageRepository;
import kr.bokjakbokjak.room.repository.MatchingRepository;
import kr.bokjakbokjak.support.FakeUserQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AgendaControllerTest {
    private static final long BOOKMARK = 7L;
    private static final long MINT = 12L;

    @Autowired MockMvc mvc;
    @Autowired JwtService jwtService;
    @Autowired FakeUserQueryService users;
    @Autowired MatchingRepository matchingRepository;
    @Autowired AgendaRepository agendaRepository;
    @Autowired ChatMessageRepository chatMessageRepository;

    private Long matchingId;
    private Long firstId;
    private Long secondId;

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    @BeforeEach
    void setUp() {
        users.clear();
        users.addUser(BOOKMARK, "책갈피", 1);
        users.addUser(MINT, "민트초코", 1);
        matchingId = matchingRepository.save(Matching.start(BOOKMARK, MINT, 1, 1)).getId();
        firstId = agendaRepository.save(Agenda.create(matchingId, 1, "BEDTIME", "평일 소등 시간",
                "평일에는 몇 시에 방 불을 끌까요?", AgendaStatus.DISCUSSING)).getId();
        secondId = agendaRepository.save(Agenda.create(matchingId, 2, "DISHES", "식기 정리",
                "사용한 컵이나 그릇은 언제까지 정리할까요?", AgendaStatus.WAITING)).getId();
    }

    @AfterEach
    void cleanUp() {
        chatMessageRepository.deleteAll();
        agendaRepository.deleteAll();
        matchingRepository.deleteAll();
        users.clear();
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(patch(path(firstId))).andExpect(status().isUnauthorized());
    }

    @Test
    void decidesAgenda() throws Exception {
        decide(BOOKMARK, firstId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("AGENDA_DECIDE_SUCCESS"))
                .andExpect(jsonPath("$.data.agenda_id").value(firstId))
                .andExpect(jsonPath("$.data.status").value("DECIDED"))
                .andExpect(jsonPath("$.data.decided_by").value(BOOKMARK))
                .andExpect(jsonPath("$.data.next_agenda_id").value(secondId))
                .andExpect(jsonPath("$.data.all_decided").value(false));
    }

    @Test
    void waitingAgendaIsConflict() throws Exception {
        decide(BOOKMARK, secondId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AGENDA_NOT_STARTED"));
    }

    @Test
    void missingAgendaIsNotFound() throws Exception {
        decide(BOOKMARK, Long.MAX_VALUE)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("AGENDA_NOT_FOUND"));
    }

    private ResultActions decide(long userId, Long agendaId) throws Exception {
        return mvc.perform(patch(path(agendaId))
                .header("Authorization", "Bearer " + jwtService.issueAccessToken(userId).accessToken()));
    }

    private String path(Long agendaId) {
        return "/api/matchings/" + matchingId + "/agendas/" + agendaId + "/decide";
    }
}
