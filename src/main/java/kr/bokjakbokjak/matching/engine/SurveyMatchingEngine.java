package kr.bokjakbokjak.matching.engine;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import kr.bokjakbokjak.matching.MatchResultDto;
import kr.bokjakbokjak.matching.MatchingEngine;
import kr.bokjakbokjak.survey.entity.Question;
import kr.bokjakbokjak.survey.entity.QuestionKind;
import kr.bokjakbokjak.survey.entity.QuestionOption;
import kr.bokjakbokjak.survey.entity.SurveyAnswer;
import kr.bokjakbokjak.survey.entity.UserWeight;
import kr.bokjakbokjak.survey.repository.QuestionOptionRepository;
import kr.bokjakbokjak.survey.repository.QuestionRepository;
import kr.bokjakbokjak.survey.repository.SurveyAnswerRepository;
import kr.bokjakbokjak.survey.repository.SurveyTopicRepository;
import kr.bokjakbokjak.survey.repository.UserWeightRepository;
import kr.bokjakbokjak.user.entity.User;
import kr.bokjakbokjak.user.repository.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 저장된 설문 답변과 카테고리 순위로 적합도를 계산한다. */
@Service
@Profile("!matching-fixture")
@Transactional(readOnly = true)
public class SurveyMatchingEngine implements MatchingEngine {
    private final UserRepository userRepository;
    private final UserWeightRepository weightRepository;
    private final SurveyAnswerRepository answerRepository;
    private final SurveyTopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;

    public SurveyMatchingEngine(
            UserRepository userRepository,
            UserWeightRepository weightRepository,
            SurveyAnswerRepository answerRepository,
            SurveyTopicRepository topicRepository,
            QuestionRepository questionRepository,
            QuestionOptionRepository optionRepository) {
        this.userRepository = userRepository;
        this.weightRepository = weightRepository;
        this.answerRepository = answerRepository;
        this.topicRepository = topicRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
    }

    /**
     * @throws IllegalArgumentException 같은 사용자이거나, 없는 사용자이거나, 설문을 완료하지 않은 사용자
     */
    @Override
    public MatchResultDto calculate(Long viewerId, Long targetId) {
        if (Objects.equals(viewerId, targetId)) {
            throw new IllegalArgumentException("자기 자신과는 매칭 점수를 계산하지 않습니다.");
        }
        MatchScoreCalculator.Profile viewer = loadProfile(viewerId);
        MatchScoreCalculator.Profile target = loadProfile(targetId);
        return MatchScoreCalculator.calculate(loadTopics(), loadLabels(), viewer, target);
    }

    private MatchScoreCalculator.Profile loadProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + userId));
        if (!user.isSurveyCompleted()) {
            throw new IllegalArgumentException("설문을 완료하지 않은 사용자입니다: " + userId);
        }
        Map<String, Integer> rankByCategory = weightRepository.findAllByIdUserIdOrderByRankAsc(userId).stream()
                .collect(Collectors.toMap(UserWeight::getCategoryCode, UserWeight::getRank));
        Map<String, String> optionByQuestion = answerRepository.findAllByIdUserId(userId).stream()
                .collect(Collectors.toMap(SurveyAnswer::getQuestionCode, SurveyAnswer::getOptionCode));
        return new MatchScoreCalculator.Profile(userId, rankByCategory, optionByQuestion);
    }

    private List<MatchScoreCalculator.Topic> loadTopics() {
        Map<String, Map<QuestionKind, String>> questionByTopic = questionRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        Question::getTopicCode,
                        Collectors.toMap(Question::getKind, Question::getCode)));
        return topicRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(topic -> new MatchScoreCalculator.Topic(
                        topic.getCode(),
                        topic.getCategoryCode(),
                        topic.getScenarioText(),
                        questionByTopic.get(topic.getCode()).get(QuestionKind.HABIT),
                        questionByTopic.get(topic.getCode()).get(QuestionKind.TOLERANCE)))
                .toList();
    }

    private Map<String, Map<String, String>> loadLabels() {
        return optionRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        QuestionOption::getQuestionCode,
                        Collectors.toMap(QuestionOption::getCode, QuestionOption::getLabel)));
    }
}
