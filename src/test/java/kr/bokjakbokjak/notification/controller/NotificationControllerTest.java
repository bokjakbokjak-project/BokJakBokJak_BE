package kr.bokjakbokjak.notification.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;
import kr.bokjakbokjak.common.JwtService;
import kr.bokjakbokjak.notification.entity.Notification;
import kr.bokjakbokjak.notification.entity.NotificationType;
import kr.bokjakbokjak.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NotificationControllerTest {
    private static final long RECEIVER = 7L;
    private static final long OTHER = 12L;

    @Autowired MockMvc mvc;
    @Autowired JwtService jwtService;
    @Autowired NotificationRepository notificationRepository;

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/notifications")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/notifications/unread-count")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/notifications/1/read")).andExpect(status().isUnauthorized());
    }

    @Test
    void listsNotificationsInSnakeCase() throws Exception {
        Long id = save(RECEIVER).getId();

        mvc.perform(get("/api/notifications").header("Authorization", bearer(RECEIVER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("NOTIFICATIONS_SUCCESS"))
                .andExpect(jsonPath("$.data.notifications[0].notification_id").value(id))
                .andExpect(jsonPath("$.data.notifications[0].type").value("CHAT_REQUESTED"))
                .andExpect(jsonPath("$.data.notifications[0].content").value("민트초코님과의 매칭이 시작되었어요"))
                .andExpect(jsonPath("$.data.notifications[0].matching_id").value(45))
                .andExpect(jsonPath("$.data.notifications[0].is_read").value(false))
                .andExpect(jsonPath("$.data.notifications[0].created_at").exists())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.has_next").value(false));
    }

    @Test
    void unreadOnlyFiltersReadNotifications() throws Exception {
        Long read = save(RECEIVER).getId();
        Long unread = save(RECEIVER).getId();
        mvc.perform(patch("/api/notifications/{id}/read", read).header("Authorization", bearer(RECEIVER)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/notifications").param("unread_only", "true").header("Authorization", bearer(RECEIVER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notifications.length()").value(1))
                .andExpect(jsonPath("$.data.notifications[0].notification_id").value(unread));
    }

    @Test
    void rejectsInvalidPaging() throws Exception {
        mvc.perform(get("/api/notifications").param("size", "0").header("Authorization", bearer(RECEIVER)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        mvc.perform(get("/api/notifications").param("page", "-1").header("Authorization", bearer(RECEIVER)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void marksOwnNotificationRead() throws Exception {
        Long id = save(RECEIVER).getId();

        mvc.perform(patch("/api/notifications/{id}/read", id).header("Authorization", bearer(RECEIVER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_READ_SUCCESS"))
                .andExpect(jsonPath("$.data.notification_id").value(id))
                .andExpect(jsonPath("$.data.is_read").value(true));
    }

    @Test
    void cannotReadOthersOrMissingNotification() throws Exception {
        Long id = save(RECEIVER).getId();

        mvc.perform(patch("/api/notifications/{id}/read", id).header("Authorization", bearer(OTHER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_FORBIDDEN"));
        mvc.perform(patch("/api/notifications/{id}/read", Long.MAX_VALUE).header("Authorization", bearer(RECEIVER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"));
    }

    @Test
    void countsUnreadNotifications() throws Exception {
        save(RECEIVER);
        save(RECEIVER);
        save(OTHER);

        mvc.perform(get("/api/notifications/unread-count").header("Authorization", bearer(RECEIVER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("UNREAD_COUNT_SUCCESS"))
                .andExpect(jsonPath("$.data.count").value(2));
    }

    private Notification save(Long receiverId) {
        return notificationRepository.saveAndFlush(Notification.create(
                receiverId, 45L, NotificationType.CHAT_REQUESTED, "민트초코님과의 매칭이 시작되었어요"));
    }

    private String bearer(long userId) {
        return "Bearer " + jwtService.issueAccessToken(userId).accessToken();
    }
}
