package kr.bokjakbokjak.recommendation.repository;

import java.util.Optional;
import kr.bokjakbokjak.recommendation.entity.AiAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, Long> {
    Optional<AiAnalysis> findByViewerIdAndTargetId(Long viewerId, Long targetId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from AiAnalysis a where a.viewerId = :userId or a.targetId = :userId")
    int deleteAllByUser(@Param("userId") Long userId);
}
