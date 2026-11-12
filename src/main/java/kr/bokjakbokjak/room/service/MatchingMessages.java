package kr.bokjakbokjak.room.service;

final class MatchingMessages {
    private MatchingMessages() {
    }

    static String endedBy(String nickname) {
        return "%s님이 매칭을 중지했어요.".formatted(nickname);
    }
}
