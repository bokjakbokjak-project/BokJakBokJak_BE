package kr.bokjakbokjak.survey.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import kr.bokjakbokjak.common.ApiException;
import kr.bokjakbokjak.common.ErrorCode;
import kr.bokjakbokjak.survey.dto.SurveySubmitRequest;
import kr.bokjakbokjak.survey.entity.SurveyCategory;
import kr.bokjakbokjak.survey.repository.SurveyCategoryRepository;
import org.springframework.stereotype.Component;

/** 카테고리 순위 검증. 온보딩과 설문 제출이 같은 규칙을 쓴다. */
@Component
public class CategoryPriorityValidator {
    private static final int MIN_PRIORITIES = 3;
    private static final int MAX_PRIORITIES = 5;
    private static final String PRIORITIES_MESSAGE = "서로 다른 카테고리를 3~5개 선택하고 1순위부터 연속으로 지정해주세요.";

    private final SurveyCategoryRepository categoryRepository;

    public CategoryPriorityValidator(SurveyCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /** 서로 다른 카테고리 3~5개, 1순위부터 연속인지 확인하고 카테고리별 순위를 돌려준다. */
    public Map<String, Integer> validate(List<SurveySubmitRequest.Priority> priorities) {
        Set<String> categoryCodes = categoryRepository.findAll().stream()
                .map(SurveyCategory::getCode)
                .collect(Collectors.toSet());

        if (priorities.size() < MIN_PRIORITIES || priorities.size() > MAX_PRIORITIES) {
            throw invalid();
        }
        Map<String, Integer> rankByCategory = new HashMap<>();
        Set<Integer> ranks = new HashSet<>();
        for (SurveySubmitRequest.Priority priority : priorities) {
            if (priority == null || priority.categoryCode() == null || priority.rank() == null
                    || !categoryCodes.contains(priority.categoryCode())
                    || priority.rank() < 1 || priority.rank() > priorities.size()
                    || !ranks.add(priority.rank())
                    || rankByCategory.putIfAbsent(priority.categoryCode(), priority.rank()) != null) {
                throw invalid();
            }
        }
        return rankByCategory;
    }

    private static ApiException invalid() {
        return new ApiException(ErrorCode.INVALID_INPUT, Map.of("priorities", PRIORITIES_MESSAGE));
    }
}
