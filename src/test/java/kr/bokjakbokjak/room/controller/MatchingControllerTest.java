package kr.bokjakbokjak.room.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;
import kr.bokjakbokjak.common.JwtService;
import kr.bokjakbokjak.notification.repository.NotificationRepository;
import kr.bokjakbokjak.room.entity.Matching;
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
class MatchingControllerTest {
    private static final long BOOKMARK = 7L;
    private static final long MINT = 12L;

    @Autowired MockMvc mvc;
    @Autowired JwtService jwtService;
    @Autowired FakeUserQueryService users;
    @Autowired MatchingRepository matchingRepository;
    @Autowired ChatMessageRepository chatMessageRepository;
    @Autowired NotificationRepository notificationRepository;

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
        notificationRepository.deleteAll();
        matchingRepository.deleteAll();
        users.clear();
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(patch("/api/matchings/" + matchingId + "/end")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/matchings/" + matchingId + "/confirm")).andExpect(status().isUnauthorized());
    }

    @Test
    void endsMatching() throws Exception {
        patchAs(BOOKMARK, "/end")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MATCH_END_SUCCESS"))
                .andExpect(jsonPath("$.data.matching_id").value(matchingId))
                .andExpect(jsonPath("$.data.status").value("ENDED"))
                .andExpect(jsonPath("$.data.end_reason").value("USER_ENDED"))
                .andExpect(jsonPath("$.data.ended_at").exists());

        patchAs(BOOKMARK, "/end")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_MATCHING_STATUS"));
    }

    @Test
    void confirmsMatching() throws Exception {
        patchAs(BOOKMARK, "/confirm")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MATCH_CONFIRM_SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("CHATTING"))
                .andExpect(jsonPath("$.data.my_confirmed").value(true))
                .andExpect(jsonPath("$.data.partner_confirmed").value(false));

        patchAs(MINT, "/confirm")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matching_id").value(matchingId))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.my_confirmed").value(true))
                .andExpect(jsonPath("$.data.partner_confirmed").value(true));
    }

    private ResultActions patchAs(long userId, String action) throws Exception {
        return mvc.perform(patch("/api/matchings/" + matchingId + action)
                .header("Authorization", "Bearer " + jwtService.issueAccessToken(userId).accessToken()));
    }
}
