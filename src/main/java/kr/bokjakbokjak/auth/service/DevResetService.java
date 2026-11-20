package kr.bokjakbokjak.auth.service;

import kr.bokjakbokjak.auth.dto.DevResetResponse;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 시연 리허설을 반복할 수 있도록 테스트 계정끼리 만든 매칭과 그 매칭의 메시지, 안건, 알림을 지운다.
 * 실제 회원이 한쪽이라도 포함된 매칭은 건드리지 않는다.
 */
@Service
public class DevResetService {
    private static final String TEST_MATCHINGS = """
            select m.matching_id from matching m
            where m.user_low_id in (select u.id from app_user u where u.test_user = TRUE)
              and m.user_high_id in (select u.id from app_user u where u.test_user = TRUE)""";

    private final DevAccessGuard guard;
    private final JdbcClient jdbc;

    public DevResetService(DevAccessGuard guard, JdbcClient jdbc) {
        this.guard = guard;
        this.jdbc = jdbc;
    }

    @Transactional
    public DevResetResponse reset(String key) {
        guard.check(key);
        int messages = deleteFrom("chat_message");
        int agendas = deleteFrom("agenda");
        int notifications = deleteFrom("notification");
        int matchings = jdbc.sql("delete from matching where matching_id in (" + TEST_MATCHINGS + ")").update();
        return new DevResetResponse(matchings, messages, agendas, notifications);
    }

    private int deleteFrom(String table) {
        return jdbc.sql("delete from " + table + " where matching_id in (" + TEST_MATCHINGS + ")").update();
    }
}
