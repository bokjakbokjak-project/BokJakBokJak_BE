package kr.bokjakbokjak.dormitory.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import kr.bokjakbokjak.common.ApiException;
import kr.bokjakbokjak.common.ErrorCode;
import kr.bokjakbokjak.dormitory.dto.DormitoryListResponse;
import kr.bokjakbokjak.dormitory.dto.DormitoryRegisterRequest;
import kr.bokjakbokjak.dormitory.dto.DormitoryResponse;
import kr.bokjakbokjak.dormitory.dto.RoomTypeResponse;
import kr.bokjakbokjak.dormitory.entity.DormitoryRoomType;
import kr.bokjakbokjak.dormitory.repository.DormitoryRoomTypeRepository;
import kr.bokjakbokjak.user.entity.User;
import kr.bokjakbokjak.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/** 온보딩에서 쓰는 기숙사·방 타입 태그. 본인 학교 것만 다룬다. */
@Service
public class DormitoryService {
    private static final int MAX_DORMITORIES = 30;
    private static final int MAX_KEYWORD_LENGTH = 80;

    private final UserRepository userRepository;
    private final DormitoryRoomTypeRepository roomTypeRepository;
    private final DormitoryRoomTypeRegistrar registrar;

    public DormitoryService(
            UserRepository userRepository,
            DormitoryRoomTypeRepository roomTypeRepository,
            DormitoryRoomTypeRegistrar registrar) {
        this.userRepository = userRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.registrar = registrar;
    }

    /** 기숙사명 부분 검색. MVP는 2인실만 보여주고, 기숙사명 오름차순으로 최대 30개 */
    public DormitoryListResponse search(Long userId, String keyword) {
        String keywordKey = keywordKey(keyword);
        Long universityId = universityOf(userId);

        Map<String, List<DormitoryRoomType>> byDormitory = roomTypeRepository
                .findAllByUniversityIdAndCapacity(universityId, DormitoryRoomType.SUPPORTED_CAPACITY).stream()
                .filter(type -> keywordKey == null || type.getDormitoryKey().contains(keywordKey))
                .collect(Collectors.groupingBy(DormitoryRoomType::getDormitoryKey, LinkedHashMap::new,
                        Collectors.toList()));

        return new DormitoryListResponse(byDormitory.values().stream()
                .map(DormitoryService::toResponse)
                .sorted(Comparator.comparing(DormitoryResponse::name))
                .limit(MAX_DORMITORIES)
                .toList());
    }

    /** 이름에서 공백을 빼고 비교해 같은 태그면 기존 ID를 재사용한다. 새로 만들어도 재사용해도 200 */
    public DormitoryResponse register(Long userId, DormitoryRegisterRequest request) {
        if (request.capacity() != DormitoryRoomType.SUPPORTED_CAPACITY) {
            throw new ApiException(ErrorCode.UNSUPPORTED_ROOM_CAPACITY);
        }
        Long universityId = universityOf(userId);
        String dormitoryKey = normalize(request.name());
        String roomTypeName = request.roomTypeName().strip();

        DormitoryRoomType roomType = findSame(universityId, dormitoryKey, roomTypeName, request.capacity())
                .orElseGet(() -> create(universityId, dormitoryKey, request.name().strip(), roomTypeName,
                        request.capacity()));
        return toResponse(List.of(roomType));
    }

    private DormitoryRoomType create(
            Long universityId, String dormitoryKey, String dormitoryName, String roomTypeName, int capacity) {
        // 같은 기숙사가 이미 있으면 처음 등록된 이름을 그대로 쓴다
        String name = roomTypeRepository.findAllByUniversityIdAndDormitoryKey(universityId, dormitoryKey).stream()
                .findFirst()
                .map(DormitoryRoomType::getDormitoryName)
                .orElse(dormitoryName);
        try {
            return registrar.create(universityId, dormitoryKey, name, roomTypeName, capacity);
        } catch (DataIntegrityViolationException exception) {
            // 같은 태그가 동시에 등록되면 먼저 만든 행을 쓴다
            return findSame(universityId, dormitoryKey, roomTypeName, capacity).orElseThrow(() -> exception);
        }
    }

    private Optional<DormitoryRoomType> findSame(
            Long universityId, String dormitoryKey, String roomTypeName, int capacity) {
        String roomTypeKey = normalize(roomTypeName);
        return roomTypeRepository.findAllByUniversityIdAndDormitoryKey(universityId, dormitoryKey).stream()
                .filter(type -> type.getCapacity() == capacity && normalize(type.getName()).equals(roomTypeKey))
                .findFirst();
    }

    private Long universityOf(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        if (user.getUniversityId() == null) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        return user.getUniversityId();
    }

    private static String keywordKey(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        if (keyword.strip().length() > MAX_KEYWORD_LENGTH) {
            throw new ApiException(ErrorCode.INVALID_INPUT, Map.of("keyword", "1~80자로 입력해주세요."));
        }
        return normalize(keyword);
    }

    /** 공백을 모두 빼고 소문자로 맞춘다. 「남산 학사」와 「남산학사」를 같은 기숙사로 본다 */
    static String normalize(String value) {
        return value.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private static DormitoryResponse toResponse(List<DormitoryRoomType> roomTypes) {
        DormitoryRoomType first = roomTypes.getFirst();
        return new DormitoryResponse(
                first.getDormitoryKey(),
                first.getDormitoryName(),
                roomTypes.stream()
                        .sorted(Comparator.comparing(DormitoryRoomType::getName))
                        .map(RoomTypeResponse::from)
                        .toList());
    }
}
