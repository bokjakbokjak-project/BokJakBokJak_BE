package kr.bokjakbokjak.survey.repository;

import java.util.List;
import kr.bokjakbokjak.survey.entity.SurveyAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyAnswerRepository extends JpaRepository<SurveyAnswer, SurveyAnswer.Id> {
    List<SurveyAnswer> findAllByIdUserId(Long userId);
}
