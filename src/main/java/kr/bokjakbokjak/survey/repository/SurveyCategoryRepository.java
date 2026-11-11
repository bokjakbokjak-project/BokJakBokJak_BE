package kr.bokjakbokjak.survey.repository;

import java.util.List;
import kr.bokjakbokjak.survey.entity.SurveyCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyCategoryRepository extends JpaRepository<SurveyCategory, String> {
    List<SurveyCategory> findAllByOrderByDisplayOrderAsc();
}
