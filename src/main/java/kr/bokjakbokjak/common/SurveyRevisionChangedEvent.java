package kr.bokjakbokjak.common;

/**
 * 사용자의 설문 차수가 올라갔을 때 발행한다 (최초 제출 포함).
 * 진행 중인 조율 초기화는 이 이벤트를 받는 쪽에서 처리한다.
 */
public record SurveyRevisionChangedEvent(Long userId, int revision) {
}
