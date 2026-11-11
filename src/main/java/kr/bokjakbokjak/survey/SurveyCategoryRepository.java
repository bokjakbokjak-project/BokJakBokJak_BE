package kr.bokjakbokjak.survey;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyCategoryRepository extends JpaRepository<SurveyCategory, String> {
    List<SurveyCategory> findAllByOrderByDisplayOrderAsc();
}
