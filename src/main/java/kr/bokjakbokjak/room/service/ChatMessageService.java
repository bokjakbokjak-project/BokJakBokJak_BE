package kr.bokjakbokjak.room.service;

import java.util.List;
import kr.bokjakbokjak.common.ApiException;
import kr.bokjakbokjak.common.ErrorCode;
import kr.bokjakbokjak.room.dto.MessageResponse;
import kr.bokjakbokjak.room.dto.NewMessagesResponse;
import kr.bokjakbokjak.room.dto.PreviousMessagesResponse;
import kr.bokjakbokjak.room.entity.Agenda;
import kr.bokjakbokjak.room.entity.AgendaStatus;
import kr.bokjakbokjak.room.entity.ChatMessage;
import kr.bokjakbokjak.room.entity.Matching;
import kr.bokjakbokjak.room.repository.AgendaRepository;
import kr.bokjakbokjak.room.repository.ChatMessageRepository;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatMessageService {
    private final MatchingReader matchingReader;
    private final ChatMessageRepository chatMessageRepository;
    private final AgendaRepository agendaRepository;

    public ChatMessageService(MatchingReader matchingReader, ChatMessageRepository chatMessageRepository,
            AgendaRepository agendaRepository) {
        this.matchingReader = matchingReader;
        this.chatMessageRepository = chatMessageRepository;
        this.agendaRepository = agendaRepository;
    }

    @Transactional(readOnly = true)
    public NewMessagesResponse getNewMessages(Long userId, Long matchingId, long afterId, int size) {
        matchingReader.getParticipating(matchingId, userId);
        List<ChatMessage> messages = chatMessageRepository
                .findAllByMatchingIdAndIdGreaterThanOrderByIdAsc(matchingId, afterId, Limit.of(size));
        return new NewMessagesResponse(toResponses(messages));
    }

    @Transactional(readOnly = true)
    public PreviousMessagesResponse getPreviousMessages(Long userId, Long matchingId, Long beforeId, int size) {
        matchingReader.getParticipating(matchingId, userId);
        Limit limit = Limit.of(size + 1);
        List<ChatMessage> newestFirst = beforeId == null
                ? chatMessageRepository.findAllByMatchingIdOrderByIdDesc(matchingId, limit)
                : chatMessageRepository.findAllByMatchingIdAndIdLessThanOrderByIdDesc(matchingId, beforeId, limit);
        boolean hasMore = newestFirst.size() > size;
        List<ChatMessage> page = newestFirst.subList(0, Math.min(size, newestFirst.size())).reversed();
        return new PreviousMessagesResponse(toResponses(page), hasMore);
    }

    @Transactional
    public MessageResponse send(Long userId, Long matchingId, String content) {
        Matching matching = matchingReader.getParticipating(matchingId, userId);
        if (!matching.isChatting()) {
            throw new ApiException(ErrorCode.INVALID_MATCHING_STATUS.status(), ErrorCode.INVALID_MATCHING_STATUS.name(),
                    "조율 중인 채팅방에만 메시지를 보낼 수 있습니다.");
        }
        Long agendaId = agendaRepository.findFirstByMatchingIdAndStatus(matchingId, AgendaStatus.DISCUSSING)
                .map(Agenda::getId)
                .orElse(null);
        ChatMessage saved = chatMessageRepository.save(ChatMessage.user(matchingId, userId, content, agendaId));
        return MessageResponse.from(saved);
    }

    private List<MessageResponse> toResponses(List<ChatMessage> messages) {
        return messages.stream().map(MessageResponse::from).toList();
    }
}
