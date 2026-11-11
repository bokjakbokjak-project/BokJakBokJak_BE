package kr.bokjakbokjak.room.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import kr.bokjakbokjak.common.ApiResponse;
import kr.bokjakbokjak.common.SuccessCode;
import kr.bokjakbokjak.room.dto.MessageResponse;
import kr.bokjakbokjak.room.dto.NewMessagesResponse;
import kr.bokjakbokjak.room.dto.PreviousMessagesResponse;
import kr.bokjakbokjak.room.dto.SendMessageRequest;
import kr.bokjakbokjak.room.service.ChatMessageService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "조율 채팅")
@RestController
@RequestMapping("/api/matchings/{matchingId}/messages")
public class ChatMessageController {
    private final ChatMessageService chatMessageService;

    public ChatMessageController(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    @Operation(summary = "새 메시지 조회 (폴링)", description = "after_id보다 큰 메시지를 오래된 순으로 반환한다.")
    @GetMapping(params = "after_id")
    public ApiResponse.Success<NewMessagesResponse> getNewMessages(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long matchingId,
            @RequestParam("after_id") @Min(0) long afterId,
            @RequestParam(name = "size", defaultValue = "50") @Min(1) @Max(100) int size) {
        return ApiResponse.success(SuccessCode.NEW_MESSAGES_SUCCESS,
                chatMessageService.getNewMessages(userId, matchingId, afterId, size));
    }

    @Operation(summary = "이전 메시지 조회", description = "before_id보다 작은 메시지를 오래된 순으로 반환한다. before_id가 없으면 가장 최근부터.")
    @GetMapping(params = "!after_id")
    public ApiResponse.Success<PreviousMessagesResponse> getPreviousMessages(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long matchingId,
            @RequestParam(name = "before_id", required = false) @Min(1) Long beforeId,
            @RequestParam(name = "size", defaultValue = "30") @Min(1) @Max(100) int size) {
        return ApiResponse.success(SuccessCode.PREVIOUS_MESSAGES_SUCCESS,
                chatMessageService.getPreviousMessages(userId, matchingId, beforeId, size));
    }

    @Operation(summary = "메시지 보내기", description = "대화 중인 채팅방에만 보낼 수 있다.")
    @PostMapping
    public ApiResponse.Success<MessageResponse> send(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long matchingId,
            @Valid @RequestBody SendMessageRequest request) {
        return ApiResponse.success(SuccessCode.MESSAGE_SEND_SUCCESS,
                chatMessageService.send(userId, matchingId, request.content()));
    }
}
