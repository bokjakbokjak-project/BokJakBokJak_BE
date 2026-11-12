package kr.bokjakbokjak.survey.repository;

import java.util.Collection;
import java.util.List;
import kr.bokjakbokjak.survey.entity.UserWeight;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserWeightRepository extends JpaRepository<UserWeight, UserWeight.Id> {
    List<UserWeight> findAllByIdUserIdOrderByRankAsc(Long userId);

    List<UserWeight> findAllByIdUserIdInOrderByRankAsc(Collection<Long> userIds);
}
