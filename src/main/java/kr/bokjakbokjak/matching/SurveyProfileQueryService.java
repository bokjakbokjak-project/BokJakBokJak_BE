package kr.bokjakbokjak.matching;

/** AI 성향 요약에 쓸 설문 내용 조회 계약. 후보 조회와 별개로 바로 사용할 수 있다. */
public interface SurveyProfileQueryService {
    /**
     * @throws IllegalArgumentException 없는 사용자이거나 설문을 완료하지 않은 사용자
     */
    SurveyProfileDto getSurveyProfile(Long userId);
}
