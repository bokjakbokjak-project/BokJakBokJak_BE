package kr.bokjakbokjak.room.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import kr.bokjakbokjak.room.entity.Matching;
import kr.bokjakbokjak.room.entity.MatchingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchingRepository extends JpaRepository<Matching, Long> {
    Optional<Matching> findByUserLowIdAndUserHighId(Long userLowId, Long userHighId);

    default Optional<Matching> findBetween(Long userId, Long otherUserId) {
        return findByUserLowIdAndUserHighId(Math.min(userId, otherUserId), Math.max(userId, otherUserId));
    }

    @Query("""
            select m from Matching m
            where (m.requesterId = :userId or m.receiverId = :userId)
            """)
    List<Matching> findAllByParticipant(@Param("userId") Long userId);

    @Query("""
            select m from Matching m
            where (m.requesterId = :userId or m.receiverId = :userId)
              and m.status in :statuses
            """)
    List<Matching> findAllByParticipantAndStatusIn(
            @Param("userId") Long userId, @Param("statuses") Collection<MatchingStatus> statuses);

    @Query("""
            select count(m) > 0 from Matching m
            where (m.requesterId = :userId or m.receiverId = :userId)
              and m.status = kr.bokjakbokjak.room.entity.MatchingStatus.CONFIRMED
            """)
    boolean existsConfirmedByParticipant(@Param("userId") Long userId);

    @Query("""
            select m from Matching m
            where (m.requesterId in :userIds or m.receiverId in :userIds)
              and m.status = kr.bokjakbokjak.room.entity.MatchingStatus.CONFIRMED
            """)
    List<Matching> findAllConfirmedByParticipantIn(@Param("userIds") Collection<Long> userIds);
}
