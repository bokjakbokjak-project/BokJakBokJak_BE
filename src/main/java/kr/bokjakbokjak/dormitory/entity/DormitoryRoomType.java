package kr.bokjakbokjak.dormitory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 학교별 기숙사와 방 타입. 기숙사는 정규화 키(dormitory_key)로 묶는다. */
@Entity
@Table(name = "dormitory_room_type")
public class DormitoryRoomType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "university_id", nullable = false)
    private Long universityId;

    @Column(name = "dormitory_key", nullable = false)
    private String dormitoryKey;

    @Column(name = "dormitory_name", nullable = false)
    private String dormitoryName;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int capacity;

    protected DormitoryRoomType() {
    }

    public Long getId() {
        return id;
    }

    public Long getUniversityId() {
        return universityId;
    }

    public String getDormitoryKey() {
        return dormitoryKey;
    }

    public String getDormitoryName() {
        return dormitoryName;
    }

    public String getName() {
        return name;
    }

    public int getCapacity() {
        return capacity;
    }
}
