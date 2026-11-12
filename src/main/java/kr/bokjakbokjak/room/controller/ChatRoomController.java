package kr.bokjakbokjak.room.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.bokjakbokjak.common.ApiResponse;
import kr.bokjakbokjak.common.SuccessCode;
import kr.bokjakbokjak.room.dto.ChatRoomListResponse;
import kr.bokjakbokjak.room.dto.ChatRoomResponse;
import kr.bokjakbokjak.room.service.ChatRoomService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "채팅방")
@RestController
@RequestMapping("/api/matchings")
public class ChatRoomController {
    private final ChatRoomService chatRoomService;

    public ChatRoomController(ChatRoomService chatRoomService) {
        this.chatRoomService = chatRoomService;
    }

    @Operation(summary = "채팅방 목록 조회", description = "대화 중이거나 확정된 채팅방을 최근 활동 순으로 반환한다.")
    @GetMapping("/me")
    public ApiResponse.Success<ChatRoomListResponse> getMyRooms(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(SuccessCode.CHAT_ROOMS_SUCCESS, chatRoomService.getMyRooms(userId));
    }

    @Operation(summary = "채팅방 정보 조회", description = "상대 정보, 안건, 확정 상태를 반환한다. 안건이 준비 중이면 빈 배열이다.")
    @GetMapping("/{matchingId}")
    public ApiResponse.Success<ChatRoomResponse> getRoom(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long matchingId) {
        return ApiResponse.success(SuccessCode.CHAT_ROOM_SUCCESS, chatRoomService.getRoom(userId, matchingId));
    }
}
