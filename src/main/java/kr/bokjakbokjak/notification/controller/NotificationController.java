package kr.bokjakbokjak.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import kr.bokjakbokjak.common.ApiResponse;
import kr.bokjakbokjak.common.SuccessCode;
import kr.bokjakbokjak.notification.dto.NotificationListResponse;
import kr.bokjakbokjak.notification.dto.NotificationReadResponse;
import kr.bokjakbokjak.notification.dto.UnreadCountResponse;
import kr.bokjakbokjak.notification.service.NotificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "알림")
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(summary = "알림 목록 조회", description = "최근 30일 알림을 최신순으로 반환한다.")
    @GetMapping
    public ApiResponse.Success<NotificationListResponse> getNotifications(
            @AuthenticationPrincipal Long userId,
            @RequestParam(name = "unread_only", defaultValue = "false") boolean unreadOnly,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(50) int size) {
        return ApiResponse.success(SuccessCode.NOTIFICATIONS_SUCCESS,
                notificationService.getNotifications(userId, unreadOnly, page, size));
    }

    @Operation(summary = "알림 읽음 처리", description = "이미 읽은 알림이어도 같은 결과를 반환한다.")
    @PatchMapping("/{notificationId}/read")
    public ApiResponse.Success<NotificationReadResponse> markRead(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long notificationId) {
        return ApiResponse.success(SuccessCode.NOTIFICATION_READ_SUCCESS,
                notificationService.markRead(userId, notificationId));
    }

    @Operation(summary = "안 읽은 알림 개수", description = "최근 30일 알림 중 읽지 않은 개수를 반환한다.")
    @GetMapping("/unread-count")
    public ApiResponse.Success<UnreadCountResponse> countUnread(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(SuccessCode.UNREAD_COUNT_SUCCESS, notificationService.countUnread(userId));
    }
}
