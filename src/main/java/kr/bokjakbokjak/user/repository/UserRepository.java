package kr.bokjakbokjak.user.repository;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import kr.bokjakbokjak.user.entity.Gender;
import kr.bokjakbokjak.user.entity.User;
import kr.bokjakbokjak.user.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    /** 같은 사용자의 설문 저장 요청이 동시에 들어와도 차수가 꼬이지 않도록 행을 잠근다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    /** 같은 학교·성별·방 타입이고 온보딩과 설문을 마친 회원. 본인은 제외한다. */
    @Query("""
            select u from User u
            where u.id <> :viewerId
              and u.universityId = :universityId
              and u.gender = :gender
              and u.roomTypeId in :roomTypeIds
              and u.status = :status
              and u.onboardingCompletedAt is not null
              and u.surveyRevision > 0
            order by u.id""")
    List<User> findCandidates(
            @Param("viewerId") Long viewerId,
            @Param("universityId") Long universityId,
            @Param("gender") Gender gender,
            @Param("roomTypeIds") Collection<Long> roomTypeIds,
            @Param("status") UserStatus status);
}
