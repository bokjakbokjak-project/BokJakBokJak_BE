package kr.bokjakbokjak.user.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import kr.bokjakbokjak.common.ApiException;
import kr.bokjakbokjak.common.ErrorCode;
import kr.bokjakbokjak.matching.SurveyProfileDto;
import kr.bokjakbokjak.matching.SurveyProfileQueryService;
import kr.bokjakbokjak.recommendation.service.KoreanAge;
import kr.bokjakbokjak.survey.repository.SurveyCategoryRepository;
import kr.bokjakbokjak.user.dto.MyProfileResponse;
import kr.bokjakbokjak.user.dto.NicknameRequest;
import kr.bokjakbokjak.user.dto.NicknameResponse;
import kr.bokjakbokjak.user.dto.ProfileCategory;
import kr.bokjakbokjak.user.dto.RoommateProfileResponse;
import kr.bokjakbokjak.user.entity.User;
import kr.bokjakbokjak.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 마이페이지·후보 상세의 「상세 답변 요약」 프로필과 닉네임 변경. */
@Service
@Transactional(readOnly = true)
public class ProfileService {
    private static final int MAX_NICKNAME_LENGTH = 20;

    private final UserRepository userRepository;
    private final SurveyProfileQueryService surveyProfileQueryService;
    private final SurveyCategoryRepository categoryRepository;

    public ProfileService(
            UserRepository userRepository,
            SurveyProfileQueryService surveyProfileQueryService,
            SurveyCategoryRepository categoryRepository) {
        this.userRepository = userRepository;
        this.surveyProfileQueryService = surveyProfileQueryService;
        this.categoryRepository = categoryRepository;
    }

    public MyProfileResponse getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        if (!user.isSurveyCompleted()) {
            throw new ApiException(ErrorCode.SURVEY_REQUIRED);
        }
        SurveyProfileDto survey = surveyProfileQueryService.getSurveyProfile(userId);
        return new MyProfileResponse(
                user.getId(),
                user.getNickname(),
                KoreanAge.of(user.getBirthYear()),
                user.getDepartment(),
                topCategoryCodes(survey),
                survey.surveyRevision(),
                categories(survey));
    }

    /** 같은 학교이고 설문을 마친 활성 회원만 볼 수 있다. 조회자 자신의 답변은 넣지 않는다. */
    public RoommateProfileResponse getRoommateProfile(Long viewerId, Long candidateId) {
        if (viewerId.equals(candidateId)) {
            throw new ApiException(ErrorCode.INVALID_INPUT, Map.of("candidate_id", "본인은 조회할 수 없습니다."));
        }
        User viewer = userRepository.findById(viewerId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        User candidate = userRepository.findById(candidateId)
                .filter(user -> user.isActive() && user.isSurveyCompleted())
                .filter(user -> user.getUniversityId() != null && user.getUniversityId().equals(viewer.getUniversityId()))
                .orElseThrow(() -> new ApiException(ErrorCode.ROOMMATE_NOT_FOUND));

        SurveyProfileDto survey = surveyProfileQueryService.getSurveyProfile(candidateId);
        return new RoommateProfileResponse(
                candidate.getId(),
                candidate.getNickname(),
                KoreanAge.of(candidate.getBirthYear()),
                candidate.getDepartment(),
                topCategoryCodes(survey),
                survey.surveyRevision(),
                categories(survey));
    }

    @Transactional
    public NicknameResponse changeNickname(Long userId, NicknameRequest request) {
        String nickname = request.nickname() == null ? "" : request.nickname().strip();
        if (nickname.isEmpty() || nickname.length() > MAX_NICKNAME_LENGTH) {
            throw new ApiException(ErrorCode.INVALID_NICKNAME, Map.of("nickname", "INVALID_FORMAT"));
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        user.changeNickname(nickname);
        return new NicknameResponse(nickname);
    }

    private static List<String> topCategoryCodes(SurveyProfileDto survey) {
        return survey.topCategories().stream().map(SurveyProfileDto.CategoryRank::code).toList();
    }

    /** 카테고리 5개를 표시 순서대로, 그 안의 주제도 표시 순서대로 묶는다. 순위 선택과 무관하게 전부 담는다 */
    private List<ProfileCategory> categories(SurveyProfileDto survey) {
        Map<String, List<ProfileCategory.Topic>> topicsByCategory = survey.topics().stream()
                .collect(Collectors.groupingBy(
                        SurveyProfileDto.TopicAnswer::categoryCode,
                        LinkedHashMap::new,
                        Collectors.mapping(topic -> new ProfileCategory.Topic(
                                topic.topicCode(), topic.habitQuestion(), topic.habitAnswer(), topic.toleranceAnswer()),
                                Collectors.toList())));

        return categoryRepository.findAllByOrderByDisplayOrderAsc().stream()
                .filter(category -> topicsByCategory.containsKey(category.getCode()))
                .map(category -> new ProfileCategory(
                        category.getCode(), category.getName(), topicsByCategory.get(category.getCode())))
                .toList();
    }
}
