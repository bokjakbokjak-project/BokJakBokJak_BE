package kr.bokjakbokjak.university.repository;

import java.util.Optional;
import kr.bokjakbokjak.university.entity.UniversityDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UniversityDomainRepository extends JpaRepository<UniversityDomain, String> {

    /** 활성 학교에 등록된 도메인이면 그 학교 ID */
    @Query("""
            select d.universityId from UniversityDomain d, University u
            where d.domain = :domain
              and u.id = d.universityId
              and u.active = true""")
    Optional<Long> findActiveUniversityId(@Param("domain") String domain);
}
