package kr.bokjakbokjak.survey.repository;

import java.util.List;
import kr.bokjakbokjak.survey.entity.SurveyTopic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyTopicRepository extends JpaRepository<SurveyTopic, String> {
    List<SurveyTopic> findAllByOrderByDisplayOrderAsc();
}
