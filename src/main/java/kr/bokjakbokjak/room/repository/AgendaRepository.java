package kr.bokjakbokjak.room.repository;

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

    Optional<Agenda> findFirstByMatchingIdAndStatus(Long matchingId, AgendaStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Agenda a where a.matchingId = :matchingId")
    int deleteAllByMatchingId(@Param("matchingId") Long matchingId);
}
