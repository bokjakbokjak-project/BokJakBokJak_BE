package kr.bokjakbokjak.dormitory.dto;

import java.util.List;

public record DormitoryListResponse(List<DormitoryResponse> dormitories) {

    public DormitoryListResponse {
        dormitories = List.copyOf(dormitories);
    }
}
