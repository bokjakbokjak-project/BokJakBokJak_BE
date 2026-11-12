package kr.bokjakbokjak.recommendation.service;

import java.util.List;

record GeneratedMatchAnalysis(List<String> goodPoints, List<String> adjustPoints, List<String> checkPoints,
        boolean aiGenerated) {
    GeneratedMatchAnalysis {
        goodPoints = List.copyOf(goodPoints);
        adjustPoints = List.copyOf(adjustPoints);
        checkPoints = List.copyOf(checkPoints);
    }
}
