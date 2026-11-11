package kr.bokjakbokjak.user.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import kr.bokjakbokjak.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    /** 같은 사용자의 설문 저장 요청이 동시에 들어와도 차수가 꼬이지 않도록 행을 잠근다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);
}
