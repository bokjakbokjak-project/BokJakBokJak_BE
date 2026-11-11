package kr.bokjakbokjak.room.service;

import kr.bokjakbokjak.room.dto.ChatRequestResponse;
import kr.bokjakbokjak.room.repository.MatchingRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class ChatRequestService {
    private final ChatRequestValidator validator;
    private final MatchingCreator matchingCreator;
    private final MatchingRepository matchingRepository;

    ChatRequestService(ChatRequestValidator validator, MatchingCreator matchingCreator,
            MatchingRepository matchingRepository) {
        this.validator = validator;
        this.matchingCreator = matchingCreator;
        this.matchingRepository = matchingRepository;
    }

    public ChatRequestResponse request(Long requesterId, Long receiverId) {
        ChatRequestValidator.ChatPair pair = validator.validate(requesterId, receiverId);
        try {
            return ChatRequestResponse.from(matchingCreator.create(pair));
        } catch (DataIntegrityViolationException exception) {
            matchingRepository.findBetween(requesterId, receiverId).ifPresent(ChatRequestValidator::rejectExisting);
            throw exception;
        }
    }
}
