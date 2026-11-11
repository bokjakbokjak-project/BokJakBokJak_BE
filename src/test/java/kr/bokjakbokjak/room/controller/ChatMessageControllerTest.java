package kr.bokjakbokjak.room.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;
import kr.bokjakbokjak.common.JwtService;
import kr.bokjakbokjak.room.entity.ChatMessage;
import kr.bokjakbokjak.room.entity.Matching;
import kr.bokjakbokjak.room.repository.ChatMessageRepository;
import kr.bokjakbokjak.room.repository.MatchingRepository;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ChatMessageControllerTest {
    private static final long REQUESTER = 7L;
    private static final long RECEIVER = 12L;
    private static final long OUTSIDER = 99L;

    @Autowired MockMvc mvc;
    @Autowired JwtService jwtService;
    @Autowired MatchingRepository matchingRepository;
    @Autowired ChatMessageRepository chatMessageRepository;

    private Long matchingId;

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    @BeforeEach
    void setUp() {
        matchingId = matchingRepository.saveAndFlush(Matching.start(REQUESTER, RECEIVER, 1, 1)).getId();
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/matchings/{id}/messages", matchingId)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/matchings/{id}/messages", matchingId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"hi\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void afterIdRoutesToPolling() throws Exception {
        Long first = save("첫 메시지");
        Long second = save("두 번째 메시지");

        mvc.perform(get("/api/matchings/{id}/messages", matchingId)
                        .param("after_id", String.valueOf(first))
                        .header("Authorization", bearer(RECEIVER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("NEW_MESSAGES_SUCCESS"))
                .andExpect(jsonPath("$.data.messages.length()").value(1))
                .andExpect(jsonPath("$.data.messages[0].message_id").value(second))
                .andExpect(jsonPath("$.data.messages[0].type").value("USER"))
                .andExpect(jsonPath("$.data.messages[0].sender_id").value(REQUESTER))
                .andExpect(jsonPath("$.data.messages[0].content").value("두 번째 메시지"))
                .andExpect(jsonPath("$.data.messages[0].created_at").exists());
    }

    @Test
    void withoutAfterIdRoutesToPreviousMessages() throws Exception {
        Long first = save("첫 메시지");
        Long second = save("두 번째 메시지");

        mvc.perform(get("/api/matchings/{id}/messages", matchingId).header("Authorization", bearer(REQUESTER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PREVIOUS_MESSAGES_SUCCESS"))
                .andExpect(jsonPath("$.data.messages[0].message_id").value(first))
                .andExpect(jsonPath("$.data.messages[1].message_id").value(second))
                .andExpect(jsonPath("$.data.has_more").value(false));

        mvc.perform(get("/api/matchings/{id}/messages", matchingId)
                        .param("before_id", String.valueOf(second))
                        .header("Authorization", bearer(REQUESTER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages.length()").value(1))
                .andExpect(jsonPath("$.data.messages[0].message_id").value(first));
    }

    @Test
    void sendsMessage() throws Exception {
        mvc.perform(post("/api/matchings/{id}/messages", matchingId)
                        .header("Authorization", bearer(REQUESTER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"저는 다음 날 아침에 한꺼번에 씻는 편이에요!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MESSAGE_SEND_SUCCESS"))
                .andExpect(jsonPath("$.data.message_id").isNumber())
                .andExpect(jsonPath("$.data.type").value("USER"))
                .andExpect(jsonPath("$.data.sender_id").value(REQUESTER))
                .andExpect(jsonPath("$.data.content").value("저는 다음 날 아침에 한꺼번에 씻는 편이에요!"))
                .andExpect(jsonPath("$.data.agenda_id").isEmpty());
    }

    @Test
    void rejectsBlankOrTooLongContent() throws Exception {
        for (String content : new String[] {"", "   ", "가".repeat(501)}) {
            mvc.perform(post("/api/matchings/{id}/messages", matchingId)
                            .header("Authorization", bearer(REQUESTER))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\":\"" + content + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                    .andExpect(jsonPath("$.errors.content").value("메시지는 1자 이상 500자 이하로 입력해 주세요."));
        }
    }

    @Test
    void rejectsInvalidSize() throws Exception {
        mvc.perform(get("/api/matchings/{id}/messages", matchingId)
                        .param("size", "0")
                        .header("Authorization", bearer(REQUESTER)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        mvc.perform(get("/api/matchings/{id}/messages", matchingId)
                        .param("after_id", "0").param("size", "101")
                        .header("Authorization", bearer(REQUESTER)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void outsiderAndMissingRoomAreRejected() throws Exception {
        mvc.perform(get("/api/matchings/{id}/messages", matchingId).header("Authorization", bearer(OUTSIDER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MATCHING_FORBIDDEN"));
        mvc.perform(post("/api/matchings/{id}/messages", Long.MAX_VALUE)
                        .header("Authorization", bearer(REQUESTER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"안녕하세요\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MATCHING_NOT_FOUND"));
    }

    private Long save(String content) {
        return chatMessageRepository.saveAndFlush(ChatMessage.user(matchingId, REQUESTER, content, null)).getId();
    }

    private String bearer(long userId) {
        return "Bearer " + jwtService.issueAccessToken(userId).accessToken();
    }
}
