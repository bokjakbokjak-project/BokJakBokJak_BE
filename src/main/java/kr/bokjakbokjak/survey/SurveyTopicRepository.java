package kr.bokjakbokjak.survey;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyTopicRepository extends JpaRepository<SurveyTopic, String> {
    List<SurveyTopic> findAllByOrderByDisplayOrderAsc();
}
