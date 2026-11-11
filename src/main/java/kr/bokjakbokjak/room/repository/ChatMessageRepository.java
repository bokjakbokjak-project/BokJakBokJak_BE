package kr.bokjakbokjak.room.repository;

import java.util.Collection;
import java.util.List;
import kr.bokjakbokjak.room.entity.ChatMessage;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findAllByMatchingIdAndIdGreaterThanOrderByIdAsc(Long matchingId, Long afterId, Limit limit);

    List<ChatMessage> findAllByMatchingIdAndIdLessThanOrderByIdDesc(Long matchingId, Long beforeId, Limit limit);

    List<ChatMessage> findAllByMatchingIdOrderByIdDesc(Long matchingId, Limit limit);

    @Query("""
            select c from ChatMessage c
            where c.id in (
                select max(c2.id) from ChatMessage c2
                where c2.matchingId in :matchingIds
                group by c2.matchingId)
            """)
    List<ChatMessage> findLatestByMatchingIdIn(@Param("matchingIds") Collection<Long> matchingIds);
}
