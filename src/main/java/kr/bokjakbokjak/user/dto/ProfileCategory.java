package kr.bokjakbokjak.user.dto;

import java.util.List;

/** 「상세 답변 요약」 카드 한 장. 주제가 2개인 카테고리는 카드 안에서 넘겨 본다. */
public record ProfileCategory(String categoryCode, String name, List<Topic> topics) {

    public ProfileCategory {
        topics = List.copyOf(topics);
    }

    /**
     * @param question 습관 질문 문구
     * @param habitAnswer 「나의 성향」
     * @param toleranceAnswer 「원하는 성향」
     */
    public record Topic(String topicCode, String question, String habitAnswer, String toleranceAnswer) {
    }
}
