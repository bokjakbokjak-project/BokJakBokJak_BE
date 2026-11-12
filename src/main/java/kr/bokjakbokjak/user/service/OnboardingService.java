package kr.bokjakbokjak.user.service;

import java.time.Instant;
import java.time.Year;
import java.time.ZoneId;
import java.util.Map;
import kr.bokjakbokjak.auth.dto.NextStep;
import kr.bokjakbokjak.common.ApiException;
import kr.bokjakbokjak.common.ErrorCode;
import kr.bokjakbokjak.dormitory.entity.DormitoryRoomType;
import kr.bokjakbokjak.dormitory.repository.DormitoryRoomTypeRepository;
import kr.bokjakbokjak.survey.entity.UserWeight;
import kr.bokjakbokjak.survey.repository.UserWeightRepository;
import kr.bokjakbokjak.survey.service.CategoryPriorityValidator;
import kr.bokjakbokjak.user.dto.OnboardingRequest;
import kr.bokjakbokjak.user.dto.OnboardingResponse;
import kr.bokjakbokjak.user.entity.User;
import kr.bokjakbokjak.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingService {
    private static final int MAX_NICKNAME_LENGTH = 20;
    private static final int MIN_BIRTH_YEAR = 1950;
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final UserRepository userRepository;
    private final UserWeightRepository weightRepository;
    private final DormitoryRoomTypeRepository roomTypeRepository;
    private final CategoryPriorityValidator priorityValidator;

    public OnboardingService(
            UserRepository userRepository,
            UserWeightRepository weightRepository,
            DormitoryRoomTypeRepository roomTypeRepository,
            CategoryPriorityValidator priorityValidator) {
        this.userRepository = userRepository;
        this.weightRepository = weightRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.priorityValidator = priorityValidator;
    }

    /** 회원 정보 갱신과 카테고리 순위 교체를 한 트랜잭션에서 처리한다. 첫 설문 제출 후에는 409. */
    @Transactional
    public OnboardingResponse save(Long userId, OnboardingRequest request) {
        Map<String, Integer> priorities = priorityValidator.validate(request.priorities());
        String nickname = validNickname(request.nickname());
        int birthYear = validBirthYear(request.birthYear());

        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        if (user.isSurveyCompleted()) {
            throw new ApiException(ErrorCode.ONBOARDING_ALREADY_SUBMITTED);
        }
        DormitoryRoomType roomType = roomTypeRepository.findById(request.roomTypeId())
                .filter(type -> type.getUniversityId().equals(user.getUniversityId()))
                .orElseThrow(() -> new ApiException(ErrorCode.ROOM_TYPE_NOT_FOUND));
        if (roomType.getCapacity() != DormitoryRoomType.SUPPORTED_CAPACITY) {
            throw new ApiException(ErrorCode.UNSUPPORTED_ROOM_CAPACITY);
        }

        user.completeOnboarding(
                request.department().strip(),
                request.studentNumber(),
                nickname,
                request.gender(),
                birthYear,
                roomType.getId(),
                Instant.now());

        weightRepository.deleteAll(weightRepository.findAllByIdUserIdOrderByRankAsc(userId));
        weightRepository.flush();
        weightRepository.saveAll(priorities.entrySet().stream()
                .map(entry -> new UserWeight(userId, entry.getKey(), entry.getValue()))
                .toList());

        return new OnboardingResponse(true, false, NextStep.of(user));
    }

    private static String validNickname(String nickname) {
        String stripped = nickname.strip();
        if (stripped.isEmpty() || stripped.length() > MAX_NICKNAME_LENGTH) {
            throw invalid("nickname", "이름은 공백을 빼고 1~20자로 입력해주세요.");
        }
        return stripped;
    }

    private static int validBirthYear(int birthYear) {
        if (birthYear < MIN_BIRTH_YEAR || birthYear > Year.now(SEOUL).getValue()) {
            throw invalid("birth_year", "출생년도를 확인해주세요.");
        }
        return birthYear;
    }

    private static ApiException invalid(String field, String message) {
        return new ApiException(ErrorCode.INVALID_INPUT, Map.of(field, message));
    }
}
