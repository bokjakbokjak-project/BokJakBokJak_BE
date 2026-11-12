package kr.bokjakbokjak.room.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import kr.bokjakbokjak.room.entity.Agenda;
import kr.bokjakbokjak.room.entity.AgendaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AgendaRepository extends JpaRepository<Agenda, Long> {
    List<Agenda> findAllByMatchingIdOrderBySeqAsc(Long matchingId);

    Optional<Agenda> findByIdAndMatchingId(Long id, Long matchingId);

    boolean existsByMatchingId(Long matchingId);

    Optional<Agenda> findFirstByMatchingIdAndStatus(Long matchingId, AgendaStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Agenda a
            set a.status = kr.bokjakbokjak.room.entity.AgendaStatus.DECIDED,
                a.decidedBy = :userId, a.decidedAt = :decidedAt, a.updatedAt = :decidedAt
            where a.id = :id and a.status = kr.bokjakbokjak.room.entity.AgendaStatus.DISCUSSING
            """)
    int decideIfDiscussing(@Param("id") Long id, @Param("userId") Long userId,
            @Param("decidedAt") Instant decidedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Agenda a where a.matchingId = :matchingId")
    int deleteAllByMatchingId(@Param("matchingId") Long matchingId);
}
