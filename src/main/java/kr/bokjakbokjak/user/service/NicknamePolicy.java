package kr.bokjakbokjak.user.service;

import java.util.Optional;

/** 온보딩과 닉네임 변경이 같이 쓰는 닉네임 규칙. 앞뒤 공백을 뺀 1~20자이고 줄바꿈·제어문자·보이지 않는 문자는 받지 않는다. */
final class NicknamePolicy {
    static final int MAX_LENGTH = 20;

    private NicknamePolicy() {
    }

    /** 규칙에 맞으면 앞뒤 공백을 뺀 닉네임, 아니면 빈 값 */
    static Optional<String> normalize(String nickname) {
        if (nickname == null) {
            return Optional.empty();
        }
        String stripped = nickname.strip();
        if (stripped.isEmpty() || stripped.length() > MAX_LENGTH) {
            return Optional.empty();
        }
        if (stripped.codePoints().anyMatch(NicknamePolicy::isHidden)) {
            return Optional.empty();
        }
        return Optional.of(stripped);
    }

    private static boolean isHidden(int codePoint) {
        int type = Character.getType(codePoint);
        return Character.isISOControl(codePoint)
                || type == Character.FORMAT
                || type == Character.LINE_SEPARATOR
                || type == Character.PARAGRAPH_SEPARATOR
                || codePoint == 0x3164
                || codePoint == 0x115F
                || codePoint == 0x1160;
    }
}
