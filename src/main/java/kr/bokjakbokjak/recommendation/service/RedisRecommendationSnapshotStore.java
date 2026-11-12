package kr.bokjakbokjak.recommendation.service;

import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
class RedisRecommendationSnapshotStore implements RecommendationSnapshotStore {
    private static final Logger log = LoggerFactory.getLogger(RedisRecommendationSnapshotStore.class);
    private static final String KEY_PREFIX = "recommendation:snapshot:";
    private static final Duration TTL = Duration.ofDays(1);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    RedisRecommendationSnapshotStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<RecommendationSnapshot> find(Long viewerId) {
        try {
            return Optional.ofNullable(redisTemplate.opsForValue().get(KEY_PREFIX + viewerId))
                    .map(json -> objectMapper.readValue(json, RecommendationSnapshot.class));
        } catch (RuntimeException exception) {
            log.warn("Recommendation snapshot read failed: viewer={}", viewerId, exception);
            return Optional.empty();
        }
    }

    @Override
    public void save(Long viewerId, RecommendationSnapshot snapshot) {
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + viewerId, objectMapper.writeValueAsString(snapshot), TTL);
        } catch (RuntimeException exception) {
            log.warn("Recommendation snapshot write failed: viewer={}", viewerId, exception);
        }
    }
}
