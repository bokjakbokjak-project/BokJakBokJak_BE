package kr.bokjakbokjak.survey.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import kr.bokjakbokjak.survey.dto.SurveyCatalogResponse;
import kr.bokjakbokjak.survey.entity.Question;
import kr.bokjakbokjak.survey.entity.QuestionOption;
import kr.bokjakbokjak.survey.repository.QuestionOptionRepository;
import kr.bokjakbokjak.survey.repository.QuestionRepository;
import kr.bokjakbokjak.survey.repository.SurveyCategoryRepository;
import kr.bokjakbokjak.survey.repository.SurveyTopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SurveyCatalogService {
    private final SurveyCategoryRepository categoryRepository;
    private final SurveyTopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;

    public SurveyCatalogService(
            SurveyCategoryRepository categoryRepository,
            SurveyTopicRepository topicRepository,
            QuestionRepository questionRepository,
            QuestionOptionRepository optionRepository) {
        this.categoryRepository = categoryRepository;
        this.topicRepository = topicRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
    }

    public SurveyCatalogResponse getCurrentSurvey() {
        Map<String, List<SurveyCatalogResponse.Option>> optionsByQuestion = optionRepository
                .findAllByOrderByQuestionCodeAscDisplayOrderAsc().stream()
                .collect(Collectors.groupingBy(
                        QuestionOption::getQuestionCode,
                        Collectors.mapping(
                                option -> new SurveyCatalogResponse.Option(option.getCode(), option.getLabel()),
                                Collectors.toList())));

        Map<String, List<SurveyCatalogResponse.QuestionItem>> questionsByTopic = questionRepository
                .findAllByOrderByQuestionNumberAsc().stream()
                .collect(Collectors.groupingBy(
                        Question::getTopicCode,
                        Collectors.mapping(
                                question -> new SurveyCatalogResponse.QuestionItem(
                                        question.getCode(),
                                        question.getQuestionNumber(),
                                        question.getKind(),
                                        question.getPrompt(),
                                        optionsByQuestion.getOrDefault(question.getCode(), List.of())),
                                Collectors.toList())));

        List<SurveyCatalogResponse.Category> categories = categoryRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(category -> new SurveyCatalogResponse.Category(
                        category.getCode(), category.getName(), category.getDisplayOrder()))
                .toList();

        List<SurveyCatalogResponse.Topic> topics = topicRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(topic -> new SurveyCatalogResponse.Topic(
                        topic.getCode(),
                        topic.getCategoryCode(),
                        topic.getDisplayOrder(),
                        topic.getScenarioText(),
                        questionsByTopic.getOrDefault(topic.getCode(), List.of())))
                .toList();

        return new SurveyCatalogResponse(categories, topics);
    }
}
