package kr.bokjakbokjak.dormitory.dto;

import java.util.List;

/** @param dormitoryKey 학교 안에서 기숙사를 묶는 키 (이름에서 공백을 뺀 값) */
public record DormitoryResponse(String dormitoryKey, String name, List<RoomTypeResponse> roomTypes) {

    public DormitoryResponse {
        roomTypes = List.copyOf(roomTypes);
    }
}
