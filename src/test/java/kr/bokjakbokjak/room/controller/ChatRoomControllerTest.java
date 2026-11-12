package kr.bokjakbokjak.room.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;
import kr.bokjakbokjak.common.JwtService;
import kr.bokjakbokjak.room.entity.Agenda;
import kr.bokjakbokjak.room.entity.AgendaStatus;
import kr.bokjakbokjak.room.entity.ChatMessage;
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
class ChatRoomControllerTest {
    private static final long BOOKMARK = 7L;
    private static final long MINT = 12L;
    private static final long OUTSIDER = 99L;

    @Autowired MockMvc mvc;
    @Autowired JwtService jwtService;
    @Autowired FakeUserQueryService users;
    @Autowired MatchingRepository matchingRepository;
    @Autowired AgendaRepository agendaRepository;
    @Autowired ChatMessageRepository chatMessageRepository;

    private Long matchingId;

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
        mvc.perform(get("/api/matchings/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/matchings/" + matchingId)).andExpect(status().isUnauthorized());
    }

    @Test
    void listsMyRooms() throws Exception {
        chatMessageRepository.save(ChatMessage.user(matchingId, MINT, "안녕하세요", null));

        getAs(BOOKMARK, "/api/matchings/me")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CHAT_ROOMS_SUCCESS"))
                .andExpect(jsonPath("$.data.rooms[0].matching_id").value(matchingId))
                .andExpect(jsonPath("$.data.rooms[0].partner.user_id").value(MINT))
                .andExpect(jsonPath("$.data.rooms[0].partner.nickname").value("민트초코"))
                .andExpect(jsonPath("$.data.rooms[0].partner.top_categories").isArray())
                .andExpect(jsonPath("$.data.rooms[0].status").value("CHATTING"))
                .andExpect(jsonPath("$.data.rooms[0].last_message").value("안녕하세요"))
                .andExpect(jsonPath("$.data.rooms[0].last_message_at").exists());
    }

    @Test
    void showsRoom() throws Exception {
        agendaRepository.save(Agenda.create(matchingId, 1, "BEDTIME", "소등", "몇 시에 불을 끌까요?", AgendaStatus.DISCUSSING));

        getAs(MINT, "/api/matchings/" + matchingId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CHAT_ROOM_SUCCESS"))
                .andExpect(jsonPath("$.data.matching_id").value(matchingId))
                .andExpect(jsonPath("$.data.status").value("CHATTING"))
                .andExpect(jsonPath("$.data.end_reason").doesNotExist())
                .andExpect(jsonPath("$.data.partner.user_id").value(BOOKMARK))
                .andExpect(jsonPath("$.data.partner.department").value("컴퓨터공학과"))
                .andExpect(jsonPath("$.data.agendas[0].agenda_id").isNumber())
                .andExpect(jsonPath("$.data.agendas[0].seq").value(1))
                .andExpect(jsonPath("$.data.agendas[0].title").value("소등"))
                .andExpect(jsonPath("$.data.agendas[0].status").value("DISCUSSING"))
                .andExpect(jsonPath("$.data.my_confirmed").value(false))
                .andExpect(jsonPath("$.data.partner_confirmed").value(false));
    }

    @Test
    void outsiderIsForbidden() throws Exception {
        getAs(OUTSIDER, "/api/matchings/" + matchingId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MATCHING_FORBIDDEN"));
    }

    @Test
    void missingRoomIsNotFound() throws Exception {
        getAs(BOOKMARK, "/api/matchings/" + Long.MAX_VALUE)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MATCHING_NOT_FOUND"));
    }

    private ResultActions getAs(long userId, String path) throws Exception {
        return mvc.perform(get(path)
                .header("Authorization", "Bearer " + jwtService.issueAccessToken(userId).accessToken()));
    }
}
