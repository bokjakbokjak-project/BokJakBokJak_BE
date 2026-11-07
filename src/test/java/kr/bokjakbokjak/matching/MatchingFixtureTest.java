package kr.bokjakbokjak.matching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class MatchingFixtureTest {
    @Test
    void fixtureProvidesBothContractsOnlyWhenEnabled() {
        try (var defaultContext = new AnnotationConfigApplicationContext(MatchingFixture.class)) {
            assertTrue(defaultContext.getBeansOfType(MatchingEngine.class).isEmpty());
        }

        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("matching-fixture");
            context.register(MatchingFixture.class);
            context.refresh();

            var users = context.getBean(UserQueryService.class);
            assertEquals(List.of(12L), users.findCandidates(7L).stream().map(CandidateDto::userId).toList());
            var summaries = users.getUserSummaries(List.of(12L, 99L, 100L, 12L));
            assertEquals(List.of(12L, 99L), summaries.stream().map(UserSummaryDto::userId).toList());
            assertEquals("WITHDRAWN", summaries.get(1).status());

            var engine = context.getBean(MatchingEngine.class);
            var result = engine.calculate(7L, 12L);
            var reversed = engine.calculate(12L, 7L);
            assertEquals(new BigDecimal("0.82"), result.mutualScore());
            assertEquals(new BigDecimal("0.85"), result.viewerToTarget());
            assertEquals(new BigDecimal("0.79"), result.targetToViewer());
            assertEquals(result.viewerToTarget(), reversed.targetToViewer());
            assertEquals(result.targetToViewer(), reversed.viewerToTarget());
            assertEquals(result.items().getFirst().viewerHabit(), reversed.items().getFirst().targetHabit());
            assertThrows(IllegalArgumentException.class, () -> engine.calculate(7L, 99L));
        }
    }
}
