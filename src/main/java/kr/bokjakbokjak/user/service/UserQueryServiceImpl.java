package kr.bokjakbokjak.user.service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import kr.bokjakbokjak.dormitory.repository.DormitoryRoomTypeRepository;
import kr.bokjakbokjak.matching.CandidateDto;
import kr.bokjakbokjak.matching.UserQueryService;
import kr.bokjakbokjak.matching.UserSummaryDto;
import kr.bokjakbokjak.survey.entity.UserWeight;
import kr.bokjakbokjak.survey.repository.UserWeightRepository;
import kr.bokjakbokjak.user.entity.User;
import kr.bokjakbokjak.user.entity.UserStatus;
import kr.bokjakbokjak.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 추천 후보와 사용자 요약을 실제 회원 데이터로 조회한다. */
@Service
@Profile("!matching-fixture")
@Transactional(readOnly = true)
public class UserQueryServiceImpl implements UserQueryService {
    private static final int TOP_CATEGORY_COUNT = 3;

    private final UserRepository userRepository;
    private final UserWeightRepository weightRepository;
    private final DormitoryRoomTypeRepository roomTypeRepository;
    private final CandidateScope candidateScope;

    public UserQueryServiceImpl(
            UserRepository userRepository,
            UserWeightRepository weightRepository,
            DormitoryRoomTypeRepository roomTypeRepository,
            @Value("${app.matching.candidate-scope:ROOM_TYPE}") CandidateScope candidateScope) {
        this.userRepository = userRepository;
        this.weightRepository = weightRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.candidateScope = candidateScope;
    }

    /**
     * 같은 학교·같은 성별·같은 기숙사(설정에 따라 방 타입까지)에서 온보딩과 설문을 마친 활성 회원.
     * 조회하는 사람이 비활성이거나 프로필·설문이 비어 있으면 빈 목록을 돌려준다.
     *
     * @throws IllegalArgumentException 존재하지 않는 사용자
     */
    @Override
    public List<CandidateDto> findCandidates(Long viewerId) {
        User viewer = userRepository.findById(viewerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + viewerId));
        if (!viewer.isActive() || !viewer.hasDormitoryProfile() || !viewer.isSurveyCompleted()) {
            return List.of();
        }

        List<Long> roomTypeIds = candidateScope == CandidateScope.DORMITORY
                ? roomTypeRepository.findIdsInSameDormitory(viewer.getRoomTypeId())
                : List.of(viewer.getRoomTypeId());
        List<User> candidates = userRepository.findCandidates(
                viewerId, viewer.getUniversityId(), viewer.getGender(), roomTypeIds, UserStatus.ACTIVE);

        Map<Long, List<String>> topCategories = topCategoriesByUser(candidates.stream().map(User::getId).toList());
        return candidates.stream()
                .map(user -> new CandidateDto(
                        user.getId(),
                        user.getNickname(),
                        user.getDepartment(),
                        user.getEntryYear(),
                        user.getBirthYear(),
                        topCategories.getOrDefault(user.getId(), List.of())))
                .toList();
    }

    /** 요청한 순서대로, 중복 없이 돌려준다. 탈퇴 회원도 포함하고 없는 ID는 생략한다. */
    @Override
    public List<UserSummaryDto> getUserSummaries(List<Long> userIds) {
        List<Long> ids = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, User> usersById = userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, List<String>> topCategories = topCategoriesByUser(usersById.keySet());

        return ids.stream()
                .map(usersById::get)
                .filter(Objects::nonNull)
                .map(user -> new UserSummaryDto(
                        user.getId(),
                        user.getNickname(),
                        user.getDepartment(),
                        user.getBirthYear(),
                        user.getStatus().name(),
                        user.getSurveyRevision(),
                        topCategories.getOrDefault(user.getId(), List.of())))
                .toList();
    }

    private Map<Long, List<String>> topCategoriesByUser(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return weightRepository.findAllByIdUserIdInOrderByRankAsc(userIds).stream()
                .filter(weight -> weight.getRank() <= TOP_CATEGORY_COUNT)
                .collect(Collectors.groupingBy(
                        UserWeight::getUserId,
                        LinkedHashMap::new,
                        Collectors.mapping(UserWeight::getCategoryCode, Collectors.toList())));
    }
}
