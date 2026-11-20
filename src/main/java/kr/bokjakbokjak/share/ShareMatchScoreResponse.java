package kr.bokjakbokjak.share;

/** @param matchScore 0~100. 추천 리스트의 매칭률과 같은 계산 */
public record ShareMatchScoreResponse(Long targetUserId, int matchScore) {
}
