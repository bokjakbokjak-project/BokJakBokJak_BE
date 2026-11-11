package kr.bokjakbokjak.room.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;
import kr.bokjakbokjak.common.JwtService;
import kr.bokjakbokjak.notification.repository.NotificationRepository;
import kr.bokjakbokjak.room.repository.MatchingRepository;
import kr.bokjakbokjak.support.FakeUserQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatRequestControllerTest {
    private static final long BOOKMARK = 7L;
    private static final long MINT = 12L;

    @Autowired MockMvc mvc;
    @Autowired JwtService jwtService;
    @Autowired FakeUserQueryService users;
    @Autowired MatchingRepository matchingRepository;
    @Autowired NotificationRepository notificationRepository;

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    @BeforeEach
    void setUp() {
        users.clear();
        users.addUser(BOOKMARK, "책갈피", 1);
        users.addUser(MINT, "민트초코", 1);
        users.connect(BOOKMARK, MINT);
    }

    @AfterEach
    void cleanUp() {
        notificationRepository.deleteAll();
        matchingRepository.deleteAll();
        users.clear();
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(post("/api/matchings").contentType(MediaType.APPLICATION_JSON).content("{\"receiver_id\":12}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void startsChatting() throws Exception {
        request(BOOKMARK, "{\"receiver_id\":12}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CHAT_REQUEST_SUCCESS"))
                .andExpect(jsonPath("$.data.matching_id").isNumber())
                .andExpect(jsonPath("$.data.status").value("CHATTING"))
                .andExpect(jsonPath("$.data.created_at").exists());
    }

    @Test
    void rejectsMissingReceiver() throws Exception {
        request(BOOKMARK, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors.receiver_id").value("대화를 신청할 사용자를 입력해 주세요."));
    }

    @Test
    void alreadyChattingIncludesExistingRoomId() throws Exception {
        request(MINT, "{\"receiver_id\":7}").andExpect(status().isOk());
        Long matchingId = matchingRepository.findAll().getFirst().getId();

        request(BOOKMARK, "{\"receiver_id\":12}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_CHATTING"))
                .andExpect(jsonPath("$.errors.matching_id").value(String.valueOf(matchingId)));
    }

    private ResultActions request(long userId, String body) throws Exception {
        return mvc.perform(post("/api/matchings")
                .header("Authorization", "Bearer " + jwtService.issueAccessToken(userId).accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
