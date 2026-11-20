package kr.bokjakbokjak.user.service;

import java.time.Instant;
import java.util.Map;
import kr.bokjakbokjak.common.ApiException;
import kr.bokjakbokjak.common.ErrorCode;
import kr.bokjakbokjak.dormitory.entity.DormitoryRoomType;
import kr.bokjakbokjak.dormitory.repository.DormitoryRoomTypeRepository;
import kr.bokjakbokjak.user.entity.Gender;
import kr.bokjakbokjak.user.entity.User;
import kr.bokjakbokjak.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 심사·데모용 체험 계정을 만든다.
 * 체험자는 학교·기숙사·성별이 제각각이라 그대로 두면 추천 후보가 0명이 되므로,
 * 테스트 페르소나와 같은 동국대 남산학사 2인실·여성으로 고정해 같은 후보군에 넣는다.
 */
@Service
public class GuestAccountService {
    static final String UNIVERSITY_CODE = "DONGGUK";
    static final String DORMITORY_KEY = "남산학사";
    static final String ROOM_NAME = "2인실";
    static final Gender GENDER = Gender.FEMALE;
    static final String DEPARTMENT = "체험 참가자";
    static final int BIRTH_YEAR = 2004;

    private final UserRepository userRepository;
    private final DormitoryRoomTypeRepository roomTypeRepository;

    public GuestAccountService(UserRepository userRepository, DormitoryRoomTypeRepository roomTypeRepository) {
        this.userRepository = userRepository;
        this.roomTypeRepository = roomTypeRepository;
    }

    /** 부를 때마다 새 계정을 만든다. 닉네임은 일반 회원과 같은 규칙이고 중복을 허용한다 */
    @Transactional
    public User create(String nickname) {
        String validNickname = NicknamePolicy.normalize(nickname)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_NICKNAME, Map.of("nickname", "INVALID_FORMAT")));
        DormitoryRoomType roomType = roomTypeRepository
                .findByUniversityCodeAndDormitoryKeyAndName(UNIVERSITY_CODE, DORMITORY_KEY, ROOM_NAME)
                .orElseThrow(() -> new ApiException(ErrorCode.ROOM_TYPE_NOT_FOUND));
        return userRepository.save(User.guest(
                roomType.getUniversityId(), validNickname, GENDER, DEPARTMENT, BIRTH_YEAR,
                roomType.getId(), Instant.now()));
    }
}
