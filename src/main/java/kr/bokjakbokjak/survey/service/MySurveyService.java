package kr.bokjakbokjak.survey.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import kr.bokjakbokjak.common.ApiException;
import kr.bokjakbokjak.common.ErrorCode;
import kr.bokjakbokjak.common.SurveyRevisionChangedEvent;
import kr.bokjakbokjak.survey.dto.MySurveyResponse;
import kr.bokjakbokjak.survey.dto.SurveySubmitRequest;
import kr.bokjakbokjak.survey.dto.SurveySubmitResponse;
import kr.bokjakbokjak.survey.entity.Question;
import kr.bokjakbokjak.survey.entity.QuestionOption;
import kr.bokjakbokjak.survey.entity.SurveyAnswer;
import kr.bokjakbokjak.survey.entity.UserWeight;
import kr.bokjakbokjak.survey.repository.QuestionOptionRepository;
import kr.bokjakbokjak.survey.repository.QuestionRepository;
import kr.bokjakbokjak.survey.repository.SurveyAnswerRepository;
import kr.bokjakbokjak.survey.repository.UserWeightRepository;
import kr.bokjakbokjak.user.entity.User;
import kr.bokjakbokjak.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MySurveyService {
    private static final String ANSWERS_MESSAGE = "필수 문항을 모두 제출해주세요.";
    private static final String OPTION_MESSAGE = "문항에 없는 선택지입니다.";

    private final UserRepository userRepository;
    private final UserWeightRepository weightRepository;
    private final SurveyAnswerRepository answerRepository;
    private final CategoryPriorityValidator priorityValidator;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public MySurveyService(
            UserRepository userRepository,
            UserWeightRepository weightRepository,
            SurveyAnswerRepository answerRepository,
            CategoryPriorityValidator priorityValidator,
            QuestionRepository questionRepository,
            QuestionOptionRepository optionRepository,
            ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.weightRepository = weightRepository;
        this.answerRepository = answerRepository;
        this.priorityValidator = priorityValidator;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public MySurveyResponse getMySurvey(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        if (!user.isSurveyCompleted()) {
            return new MySurveyResponse(0, false, null, List.of(), List.of());
        }

        List<SurveySubmitRequest.Priority> priorities = weightRepository.findAllByIdUserIdOrderByRankAsc(userId)
                .stream()
                .map(weight -> new SurveySubmitRequest.Priority(weight.getCategoryCode(), weight.getRank()))
                .toList();

        Map<String, String> optionByQuestion = answerRepository.findAllByIdUserId(userId).stream()
                .collect(Collectors.toMap(SurveyAnswer::getQuestionCode, SurveyAnswer::getOptionCode));
        List<SurveySubmitRequest.Answer> answers = questionRepository.findAllByOrderByQuestionNumberAsc().stream()
                .filter(question -> optionByQuestion.containsKey(question.getCode()))
                .map(question -> new SurveySubmitRequest.Answer(
                        question.getCode(), optionByQuestion.get(question.getCode())))
                .toList();

        return new MySurveyResponse(
                user.getSurveyRevision(), true, user.getSurveyCompletedAt(), priorities, answers);
    }

    @Transactional
    public SurveySubmitResponse submit(Long userId, SurveySubmitRequest request) {
        Map<String, Integer> priorities = priorityValidator.validate(request.priorities());
        Map<String, String> answers = validateAnswers(request.answers());

        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        if (!user.isOnboardingCompleted()) {
            throw new ApiException(ErrorCode.ONBOARDING_REQUIRED);
        }

        List<UserWeight> currentWeights = weightRepository.findAllByIdUserIdOrderByRankAsc(userId);
        List<SurveyAnswer> currentAnswers = answerRepository.findAllByIdUserId(userId);

        // 같은 요청을 다시 보낸 경우: 저장된 내용과 같으면 차수를 그대로 돌려준다
        if (user.isSurveyCompleted() && sameContent(currentWeights, currentAnswers, priorities, answers)) {
            return toResponse(user);
        }
        if (request.expectedRevision() != user.getSurveyRevision()) {
            throw new ApiException(ErrorCode.SURVEY_REVISION_CONFLICT);
        }

        weightRepository.deleteAll(currentWeights);
        answerRepository.deleteAll(currentAnswers);
        weightRepository.flush();
        answerRepository.flush();

        weightRepository.saveAll(priorities.entrySet().stream()
                .map(entry -> new UserWeight(userId, entry.getKey(), entry.getValue()))
                .toList());
        answerRepository.saveAll(answers.entrySet().stream()
                .map(entry -> new SurveyAnswer(userId, entry.getKey(), entry.getValue()))
                .toList());

        int revision = user.completeSurvey(Instant.now());
        eventPublisher.publishEvent(new SurveyRevisionChangedEvent(userId, revision));
        return toResponse(user);
    }

    private Map<String, String> validateAnswers(List<SurveySubmitRequest.Answer> answers) {
        Set<String> questionCodes = questionRepository.findAll().stream()
                .map(Question::getCode)
                .collect(Collectors.toSet());
        Map<String, Set<String>> optionsByQuestion = optionRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        QuestionOption::getQuestionCode,
                        Collectors.mapping(QuestionOption::getCode, Collectors.toSet())));

        Map<String, String> optionByQuestion = new HashMap<>();
        for (SurveySubmitRequest.Answer answer : answers) {
            if (answer == null || answer.questionCode() == null || answer.optionCode() == null
                    || !questionCodes.contains(answer.questionCode())
                    || optionByQuestion.putIfAbsent(answer.questionCode(), answer.optionCode()) != null) {
                throw invalid("answers", ANSWERS_MESSAGE);
            }
            if (!optionsByQuestion.getOrDefault(answer.questionCode(), Set.of()).contains(answer.optionCode())) {
                throw invalid("answers", OPTION_MESSAGE);
            }
        }
        if (!optionByQuestion.keySet().equals(questionCodes)) {
            throw invalid("answers", ANSWERS_MESSAGE);
        }
        return optionByQuestion;
    }

    private static boolean sameContent(
            List<UserWeight> currentWeights,
            List<SurveyAnswer> currentAnswers,
            Map<String, Integer> priorities,
            Map<String, String> answers) {
        Map<String, Integer> savedPriorities = currentWeights.stream()
                .collect(Collectors.toMap(UserWeight::getCategoryCode, UserWeight::getRank));
        Map<String, String> savedAnswers = currentAnswers.stream()
                .collect(Collectors.toMap(SurveyAnswer::getQuestionCode, SurveyAnswer::getOptionCode));
        return savedPriorities.equals(priorities) && savedAnswers.equals(answers);
    }

    private static SurveySubmitResponse toResponse(User user) {
        return new SurveySubmitResponse(user.getSurveyRevision(), true, user.getSurveyCompletedAt(), "HOME");
    }

    private static ApiException invalid(String field, String message) {
        return new ApiException(ErrorCode.INVALID_INPUT, Map.of(field, message));
    }
}
