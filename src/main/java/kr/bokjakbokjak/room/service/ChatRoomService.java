package kr.bokjakbokjak.room.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import kr.bokjakbokjak.matching.UserSummaryDto;
import kr.bokjakbokjak.room.dto.ChatRoomListResponse;
import kr.bokjakbokjak.room.dto.ChatRoomResponse;
import kr.bokjakbokjak.room.entity.ChatMessage;
import kr.bokjakbokjak.room.entity.Matching;
import kr.bokjakbokjak.room.entity.MatchingStatus;
import kr.bokjakbokjak.room.repository.AgendaRepository;
import kr.bokjakbokjak.room.repository.ChatMessageRepository;
import kr.bokjakbokjak.room.repository.MatchingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatRoomService {
    private static final List<MatchingStatus> VISIBLE_STATUSES = List.of(MatchingStatus.CHATTING,
            MatchingStatus.CONFIRMED);

    private final MatchingRepository matchingRepository;
    private final MatchingReader matchingReader;
    private final AgendaRepository agendaRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatPartnerReader partnerReader;

    ChatRoomService(MatchingRepository matchingRepository, MatchingReader matchingReader,
            AgendaRepository agendaRepository, ChatMessageRepository chatMessageRepository,
            ChatPartnerReader partnerReader) {
        this.matchingRepository = matchingRepository;
        this.matchingReader = matchingReader;
        this.agendaRepository = agendaRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.partnerReader = partnerReader;
    }

    @Transactional(readOnly = true)
    public ChatRoomListResponse getMyRooms(Long userId) {
        List<Matching> matchings = matchingRepository.findAllByParticipantAndStatusIn(userId, VISIBLE_STATUSES);
        if (matchings.isEmpty()) {
            return new ChatRoomListResponse(List.of());
        }
        Map<Long, UserSummaryDto> partners = partnerReader.readAll(
                matchings.stream().map(matching -> matching.partnerOf(userId)).toList());
        Map<Long, ChatMessage> lastMessages = chatMessageRepository
                .findLatestByMatchingIdIn(matchings.stream().map(Matching::getId).toList()).stream()
                .collect(Collectors.toMap(ChatMessage::getMatchingId, Function.identity()));

        List<ChatRoomListResponse.Room> rooms = matchings.stream()
                .sorted(Comparator.comparing((Matching matching) -> lastActivityAt(matching, lastMessages))
                        .thenComparing(Matching::getId)
                        .reversed())
                .map(matching -> toRoom(matching, partners.get(matching.partnerOf(userId)),
                        lastMessages.get(matching.getId())))
                .toList();
        return new ChatRoomListResponse(rooms);
    }

    @Transactional(readOnly = true)
    public ChatRoomResponse getRoom(Long userId, Long matchingId) {
        Matching matching = matchingReader.getParticipating(matchingId, userId);
        Long partnerId = matching.partnerOf(userId);
        List<ChatRoomResponse.AgendaItem> agendas = agendaRepository.findAllByMatchingIdOrderBySeqAsc(matchingId)
                .stream()
                .map(ChatRoomResponse.AgendaItem::from)
                .toList();
        return new ChatRoomResponse(
                matching.getId(),
                matching.getStatus(),
                matching.getEndReason(),
                ChatRoomResponse.Partner.from(partnerReader.read(partnerId)),
                agendas,
                matching.isConfirmedBy(userId),
                matching.isConfirmedBy(partnerId));
    }

    private static Instant lastActivityAt(Matching matching, Map<Long, ChatMessage> lastMessages) {
        ChatMessage last = lastMessages.get(matching.getId());
        return last == null ? matching.getCreatedAt() : last.getCreatedAt();
    }

    private static ChatRoomListResponse.Room toRoom(Matching matching, UserSummaryDto partner, ChatMessage last) {
        return new ChatRoomListResponse.Room(
                matching.getId(),
                new ChatRoomListResponse.Partner(partner.userId(), partner.nickname(), partner.topCategories()),
                matching.getStatus(),
                last == null ? null : last.getContent(),
                last == null ? null : last.getCreatedAt());
    }
}
