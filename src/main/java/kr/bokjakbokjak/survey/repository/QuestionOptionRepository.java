package kr.bokjakbokjak.survey.repository;

import java.util.List;
import kr.bokjakbokjak.survey.entity.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {
    List<QuestionOption> findAllByOrderByQuestionCodeAscDisplayOrderAsc();
}
