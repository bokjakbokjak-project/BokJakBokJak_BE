package kr.bokjakbokjak.user.entity;

/** 회원 상태. 탈퇴해도 행은 지우지 않고 상태만 바꾼다. */
public enum UserStatus {
    ACTIVE,
    /** 탈퇴. 채팅 기록 등에서 상대 정보를 보여줄 수 있도록 행은 남긴다 */
    WITHDRAWN,
    /** 이용 제한. 로그인과 추천에서 제외한다 */
    SUSPENDED
}
