package kr.bokjakbokjak.survey.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import kr.bokjakbokjak.matching.SurveyProfileDto;
import kr.bokjakbokjak.matching.SurveyProfileQueryService;
import kr.bokjakbokjak.survey.entity.Question;
import kr.bokjakbokjak.survey.entity.QuestionKind;
import kr.bokjakbokjak.survey.entity.QuestionOption;
import kr.bokjakbokjak.survey.entity.SurveyAnswer;
import kr.bokjakbokjak.survey.entity.SurveyCategory;
import kr.bokjakbokjak.survey.repository.QuestionOptionRepository;
import kr.bokjakbokjak.survey.repository.QuestionRepository;
import kr.bokjakbokjak.survey.repository.SurveyAnswerRepository;
import kr.bokjakbokjak.survey.repository.SurveyCategoryRepository;
import kr.bokjakbokjak.survey.repository.SurveyTopicRepository;
import kr.bokjakbokjak.survey.repository.UserWeightRepository;
import kr.bokjakbokjak.user.entity.User;
import kr.bokjakbokjak.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 저장된 설문 답변을 AI 요약용 문장 형태로 조회한다. */
@Service
@Transactional(readOnly = true)
public class SurveyProfileService implements SurveyProfileQueryService {
    private static final int TOP_CATEGORY_COUNT = 3;

    private final UserRepository userRepository;
    private final UserWeightRepository weightRepository;
    private final SurveyAnswerRepository answerRepository;
    private final SurveyCategoryRepository categoryRepository;
    private final SurveyTopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;

    public SurveyProfileService(
            UserRepository userRepository,
            UserWeightRepository weightRepository,
            SurveyAnswerRepository answerRepository,
            SurveyCategoryRepository categoryRepository,
            SurveyTopicRepository topicRepository,
            QuestionRepository questionRepository,
            QuestionOptionRepository optionRepository) {
        this.userRepository = userRepository;
        this.weightRepository = weightRepository;
        this.answerRepository = answerRepository;
        this.categoryRepository = categoryRepository;
        this.topicRepository = topicRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
    }

    @Override
    public SurveyProfileDto getSurveyProfile(Long userId) {
        // 차수를 답변보다 먼저 읽는다. 도중에 설문이 수정돼도 차수가 옛 값으로 남아 다음 조회에서 다시 만들어진다
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + userId));
        if (!user.isSurveyCompleted()) {
            throw new IllegalArgumentException("설문을 완료하지 않은 사용자입니다: " + userId);
        }

        Map<String, String> categoryNames = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(SurveyCategory::getCode, SurveyCategory::getName));
        List<SurveyProfileDto.CategoryRank> topCategories = weightRepository.findAllByIdUserIdOrderByRankAsc(userId)
                .stream()
                .filter(weight -> weight.getRank() <= TOP_CATEGORY_COUNT)
                .map(weight -> new SurveyProfileDto.CategoryRank(
                        weight.getCategoryCode(), categoryNames.get(weight.getCategoryCode()), weight.getRank()))
                .toList();

        Map<String, String> optionByQuestion = answerRepository.findAllByIdUserId(userId).stream()
                .collect(Collectors.toMap(SurveyAnswer::getQuestionCode, SurveyAnswer::getOptionCode));
        Map<String, Map<QuestionKind, Question>> questionsByTopic = questionRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        Question::getTopicCode,
                        Collectors.toMap(Question::getKind, Function.identity())));
        Map<String, Map<String, String>> labelsByQuestion = optionRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        QuestionOption::getQuestionCode,
                        Collectors.toMap(QuestionOption::getCode, QuestionOption::getLabel)));

        List<SurveyProfileDto.TopicAnswer> topics = topicRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(topic -> {
                    Question habit = questionsByTopic.get(topic.getCode()).get(QuestionKind.HABIT);
                    Question tolerance = questionsByTopic.get(topic.getCode()).get(QuestionKind.TOLERANCE);
                    return new SurveyProfileDto.TopicAnswer(
                            topic.getCode(),
                            topic.getCategoryCode(),
                            categoryNames.get(topic.getCategoryCode()),
                            topic.getScenarioText(),
                            habit.getPrompt(),
                            answerLabel(habit, optionByQuestion, labelsByQuestion),
                            tolerance.getPrompt(),
                            answerLabel(tolerance, optionByQuestion, labelsByQuestion));
                })
                .toList();

        return new SurveyProfileDto(userId, user.getSurveyRevision(), topCategories, topics);
    }

    private static String answerLabel(
            Question question,
            Map<String, String> optionByQuestion,
            Map<String, Map<String, String>> labelsByQuestion) {
        return labelsByQuestion.get(question.getCode()).get(optionByQuestion.get(question.getCode()));
    }
}
