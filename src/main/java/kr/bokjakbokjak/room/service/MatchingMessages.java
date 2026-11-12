package kr.bokjakbokjak.room.service;

final class MatchingMessages {
    private MatchingMessages() {
    }

    static String endedBy(String nickname) {
        return "%s님이 매칭을 중지했어요.".formatted(nickname);
    }

    static String partnerConfirmedFirst(String nickname) {
        return "%s님이 먼저 매칭을 확정했어요.".formatted(nickname);
    }

    static String partnerMatchedElsewhere() {
        return "상대가 다른 분과 매칭이 완료되었어요.";
    }
}
