package kr.bokjakbokjak.survey;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserWeightRepository extends JpaRepository<UserWeight, UserWeight.Id> {
    List<UserWeight> findAllByIdUserIdOrderByRankAsc(Long userId);
}
