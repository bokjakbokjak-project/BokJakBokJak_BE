package kr.bokjakbokjak.room.service;

import java.util.List;

final class AgendaMessages {
    private static final List<String> ORDINALS = List.of("첫", "두", "세", "네", "다섯");

    private AgendaMessages() {
    }

    static String introduce(int seq, String content) {
        return "%s 번째 안건이에요. %s".formatted(ordinal(seq), content);
    }

    private static String ordinal(int seq) {
        return seq >= 1 && seq <= ORDINALS.size() ? ORDINALS.get(seq - 1) : String.valueOf(seq);
    }
}
