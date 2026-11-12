package kr.bokjakbokjak.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import kr.bokjakbokjak.common.BaseEntity;

@Entity
@Table(name = "app_user")
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "google_sub", unique = true)
    private String googleSub;

    @Column(length = 320)
    private String email;

    @Column(name = "university_id")
    private Long universityId;

    @Column(length = 20)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(name = "birth_year")
    private Integer birthYear;

    @Column(length = 60)
    private String department;

    @Column(name = "student_number", length = 10)
    private String studentNumber;

    @Column(name = "room_type_id")
    private Long roomTypeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    /** 시연·연동용 테스트 유저. 테스트 로그인은 이 회원만 허용한다 */
    @Column(name = "test_user", nullable = false)
    private boolean testUser;

    @Column(name = "onboarding_completed_at")
    private Instant onboardingCompletedAt;

    @Column(name = "survey_revision", nullable = false)
    private int surveyRevision;

    @Column(name = "survey_completed_at")
    private Instant surveyCompletedAt;

    protected User() {
    }

    /** Google 학교 계정으로 처음 로그인한 회원. 온보딩 정보는 비어 있다 */
    public static User register(Long universityId, String googleSub, String email) {
        User user = new User();
        user.universityId = universityId;
        user.googleSub = googleSub;
        user.email = email;
        return user;
    }

    /** 온보딩 4단계 정보를 저장한다. 첫 설문 제출 전까지는 다시 저장할 수 있다 */
    public void completeOnboarding(
            String department,
            String studentNumber,
            String nickname,
            Gender gender,
            int birthYear,
            Long roomTypeId,
            Instant completedAt) {
        this.department = department;
        this.studentNumber = studentNumber;
        this.nickname = nickname;
        this.gender = gender;
        this.birthYear = birthYear;
        this.roomTypeId = roomTypeId;
        this.onboardingCompletedAt = completedAt;
    }

    public boolean isOnboardingCompleted() {
        return onboardingCompletedAt != null;
    }

    public boolean isSurveyCompleted() {
        return surveyRevision > 0;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    /** 후보 조회에 필요한 학교·성별·방 타입이 모두 있는지 */
    public boolean hasDormitoryProfile() {
        return universityId != null && gender != null && roomTypeId != null;
    }

    /** 학번 앞 4자리를 입학년도로 본다. 형식이 맞지 않으면 null */
    public Integer getEntryYear() {
        if (studentNumber == null || studentNumber.length() < 4) {
            return null;
        }
        String prefix = studentNumber.substring(0, 4);
        return prefix.chars().allMatch(Character::isDigit) ? Integer.valueOf(prefix) : null;
    }

    /** 설문 내용이 바뀌었을 때 호출한다. 차수를 1 올리고 새 차수를 돌려준다. */
    public int completeSurvey(Instant submittedAt) {
        surveyRevision++;
        surveyCompletedAt = submittedAt;
        return surveyRevision;
    }

    public Long getId() {
        return id;
    }

    public String getGoogleSub() {
        return googleSub;
    }

    public String getEmail() {
        return email;
    }

    public Long getUniversityId() {
        return universityId;
    }

    public String getNickname() {
        return nickname;
    }

    public Gender getGender() {
        return gender;
    }

    public Integer getBirthYear() {
        return birthYear;
    }

    public String getDepartment() {
        return department;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public Long getRoomTypeId() {
        return roomTypeId;
    }

    public UserStatus getStatus() {
        return status;
    }

    public boolean isTestUser() {
        return testUser;
    }

    public Instant getOnboardingCompletedAt() {
        return onboardingCompletedAt;
    }

    public int getSurveyRevision() {
        return surveyRevision;
    }

    public Instant getSurveyCompletedAt() {
        return surveyCompletedAt;
    }
}
