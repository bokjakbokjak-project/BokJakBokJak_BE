package kr.bokjakbokjak.dormitory.dto;

import kr.bokjakbokjak.dormitory.entity.DormitoryRoomType;

public record RoomTypeResponse(Long roomTypeId, String name, int capacity) {

    public static RoomTypeResponse from(DormitoryRoomType roomType) {
        return new RoomTypeResponse(roomType.getId(), roomType.getName(), roomType.getCapacity());
    }
}
