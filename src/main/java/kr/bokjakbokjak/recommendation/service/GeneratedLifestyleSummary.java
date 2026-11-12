package kr.bokjakbokjak.recommendation.service;

import java.util.List;

record GeneratedLifestyleSummary(String summary, List<String> keywords, boolean aiGenerated) {
    GeneratedLifestyleSummary {
        keywords = List.copyOf(keywords);
    }
}
