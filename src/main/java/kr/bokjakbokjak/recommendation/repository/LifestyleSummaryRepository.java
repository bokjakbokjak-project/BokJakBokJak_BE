package kr.bokjakbokjak.recommendation.repository;

import java.util.Optional;
import kr.bokjakbokjak.recommendation.entity.LifestyleSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LifestyleSummaryRepository extends JpaRepository<LifestyleSummary, Long> {
    Optional<LifestyleSummary> findByUserId(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from LifestyleSummary s where s.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
