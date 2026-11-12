package kr.bokjakbokjak.room.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.bokjakbokjak.common.ApiResponse;
import kr.bokjakbokjak.common.SuccessCode;
import kr.bokjakbokjak.room.dto.ChatRequestRequest;
import kr.bokjakbokjak.room.dto.ChatRequestResponse;
import kr.bokjakbokjak.room.service.ChatRequestService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "대화 신청")
@RestController
@RequestMapping("/api/matchings")
public class ChatRequestController {
    private final ChatRequestService chatRequestService;

    public ChatRequestController(ChatRequestService chatRequestService) {
        this.chatRequestService = chatRequestService;
    }

    @Operation(summary = "대화 신청", description = "수락 단계 없이 바로 채팅방을 만든다. 안건은 응답 후 준비된다.")
    @PostMapping
    public ApiResponse.Success<ChatRequestResponse> request(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ChatRequestRequest request) {
        return ApiResponse.success(SuccessCode.CHAT_REQUEST_SUCCESS,
                chatRequestService.request(userId, request.receiverId()));
    }
}
