package kr.bokjakbokjak.dormitory.repository;

import java.util.List;
import java.util.Optional;
import kr.bokjakbokjak.dormitory.entity.DormitoryRoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DormitoryRoomTypeRepository extends JpaRepository<DormitoryRoomType, Long> {

    List<DormitoryRoomType> findAllByUniversityIdAndCapacity(Long universityId, int capacity);

    List<DormitoryRoomType> findAllByUniversityIdAndDormitoryKey(Long universityId, String dormitoryKey);

    /** 학교 코드·기숙사·방 이름으로 방 타입을 찾는다. 체험 계정의 방을 정할 때 쓴다 */
    @Query("""
            select r from DormitoryRoomType r, University u
            where u.id = r.universityId
              and u.code = :universityCode
              and r.dormitoryKey = :dormitoryKey
              and r.name = :name""")
    Optional<DormitoryRoomType> findByUniversityCodeAndDormitoryKeyAndName(
            @Param("universityCode") String universityCode,
            @Param("dormitoryKey") String dormitoryKey,
            @Param("name") String name);

    /** 주어진 방 타입과 같은 학교·같은 기숙사에 있는 방 타입 ID 전체 */
    @Query("""
            select other.id from DormitoryRoomType other, DormitoryRoomType base
            where base.id = :roomTypeId
              and other.universityId = base.universityId
              and other.dormitoryKey = base.dormitoryKey""")
    List<Long> findIdsInSameDormitory(@Param("roomTypeId") Long roomTypeId);
}
