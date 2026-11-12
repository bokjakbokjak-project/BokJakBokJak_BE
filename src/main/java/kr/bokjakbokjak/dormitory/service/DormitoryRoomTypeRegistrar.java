package kr.bokjakbokjak.dormitory.service;

import kr.bokjakbokjak.dormitory.entity.DormitoryRoomType;
import kr.bokjakbokjak.dormitory.repository.DormitoryRoomTypeRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 태그 생성을 별도 트랜잭션으로 분리해 동시 등록 충돌을 바깥에서 처리할 수 있게 한다. */
@Component
class DormitoryRoomTypeRegistrar {
    private final DormitoryRoomTypeRepository roomTypeRepository;

    DormitoryRoomTypeRegistrar(DormitoryRoomTypeRepository roomTypeRepository) {
        this.roomTypeRepository = roomTypeRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DormitoryRoomType create(
            Long universityId, String dormitoryKey, String dormitoryName, String name, int capacity) {
        return roomTypeRepository.saveAndFlush(
                DormitoryRoomType.create(universityId, dormitoryKey, dormitoryName, name, capacity));
    }
}
