package kr.bokjakbokjak.survey;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, String> {
    List<Question> findAllByOrderByQuestionNumberAsc();
}
