package kr.bokjakbokjak.survey;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyAnswerRepository extends JpaRepository<SurveyAnswer, SurveyAnswer.Id> {
    List<SurveyAnswer> findAllByIdUserId(Long userId);
}
