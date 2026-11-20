-- 설문 개편 (2026-11-20): 식기 문항을 빼고 냉난방(온도) 문항을 추가한다
-- 문항 수는 16개 그대로이고 카테고리는 5개에서 6개가 된다. 온도는 흡연 다음, 맨 마지막이다

-- 1. 식기 삭제. 개인 청결 카테고리는 개인 물건 주제가 남아 있어 그대로 둔다
DELETE FROM survey_answer WHERE question_code IN ('DISHES_H', 'DISHES_T');
DELETE FROM question_option WHERE question_code IN ('DISHES_H', 'DISHES_T');
DELETE FROM question WHERE topic_code = 'DISHES';
DELETE FROM survey_topic WHERE code = 'DISHES';

-- 2. 흡연을 식기 자리(7번 주제, 13·14번 문항)로 당긴다
UPDATE survey_topic SET display_order = 7 WHERE code = 'SMOKING';
UPDATE question SET question_number = 13 WHERE code = 'SMOKING_H';
UPDATE question SET question_number = 14 WHERE code = 'SMOKING_T';

-- 3. 온도 추가
INSERT INTO survey_category (code, name, display_order) VALUES
    ('TEMPERATURE', '온도', 6);

INSERT INTO survey_topic (code, category_code, scenario_text, display_order) VALUES
    ('TEMPERATURE', 'TEMPERATURE',
     '초여름이나 초겨울, 냉난방을 사용해요. 계속 켜두는 사람도 있고, 필요할 때만 잠깐 사용하는 사람도 있어요.', 8);

INSERT INTO question (code, topic_code, kind, question_number, prompt) VALUES
    ('TEMPERATURE_H', 'TEMPERATURE', 'HABIT', 15, '평소 냉난방과 관련해 내 경험과 가까운 것을 모두 골라주세요'),
    ('TEMPERATURE_T', 'TEMPERATURE', 'TOLERANCE', 16, '룸메이트의 냉난방 습관 중, 함께 지내기 힘들 것 같은 것을 모두 골라 주세요.');

INSERT INTO question_option (question_code, code, label, display_order) VALUES
    -- Q15
    ('TEMPERATURE_H', 'ALWAYS_ON', '방에 있는 동안 계속 켜두는 편이다', 1),
    ('TEMPERATURE_H', 'LONG', '덥거나 추울 때 일정시간 이상 켜둔다', 2),
    ('TEMPERATURE_H', 'SHORT', '덥거나 추울 때 잠깐 켰다가 끈다', 3),
    ('TEMPERATURE_H', 'RARELY', '거의 사용하지 않는다', 4),
    -- Q16
    ('TEMPERATURE_T', 'ALWAYS_ON_OK', '방에 있는 동안 계속 켜두어도 괜찮다', 1),
    ('TEMPERATURE_T', 'LONG_OK', '덥거나 추울 때 일정시간 이상 켜두어도 괜찮다', 2),
    ('TEMPERATURE_T', 'SHORT_ONLY', '덥거나 추울 때 잠깐 켰다가 꺼줬으면 한다', 3),
    ('TEMPERATURE_T', 'RARELY_ONLY', '거의 사용하지 않았으면 좋겠다', 4);

-- 4. 테스트 페르소나는 온도 답을 넣어 설문 완료 상태를 유지한다
INSERT INTO survey_answer (user_id, question_code, option_code)
SELECT u.id, a.question_code, a.option_code
FROM (VALUES
    ('책갈피', 'TEMPERATURE_H', 'LONG'), ('책갈피', 'TEMPERATURE_T', 'LONG_OK'),
    ('깔끔이', 'TEMPERATURE_H', 'SHORT'), ('깔끔이', 'TEMPERATURE_T', 'SHORT_ONLY'),
    ('털털이', 'TEMPERATURE_H', 'ALWAYS_ON'), ('털털이', 'TEMPERATURE_T', 'ALWAYS_ON_OK'),
    ('아침햇살', 'TEMPERATURE_H', 'RARELY'), ('아침햇살', 'TEMPERATURE_T', 'SHORT_ONLY'),
    ('새벽감성', 'TEMPERATURE_H', 'ALWAYS_ON'), ('새벽감성', 'TEMPERATURE_T', 'LONG_OK'),
    ('연기한줄', 'TEMPERATURE_H', 'LONG'), ('연기한줄', 'TEMPERATURE_T', 'ALWAYS_ON_OK'),
    ('드르렁', 'TEMPERATURE_H', 'LONG'), ('드르렁', 'TEMPERATURE_T', 'LONG_OK'),
    ('무난이', 'TEMPERATURE_H', 'SHORT'), ('무난이', 'TEMPERATURE_T', 'LONG_OK'),
    ('갸우뚱', 'TEMPERATURE_H', 'SHORT'), ('갸우뚱', 'TEMPERATURE_T', 'ALWAYS_ON_OK'),
    ('금연중', 'TEMPERATURE_H', 'LONG'), ('금연중', 'TEMPERATURE_T', 'LONG_OK'),
    ('민트초코', 'TEMPERATURE_H', 'LONG'), ('민트초코', 'TEMPERATURE_T', 'LONG_OK'),
    ('옆동주민', 'TEMPERATURE_H', 'LONG'), ('옆동주민', 'TEMPERATURE_T', 'LONG_OK'),
    ('옆방타입', 'TEMPERATURE_H', 'LONG'), ('옆방타입', 'TEMPERATURE_T', 'LONG_OK')) AS a (nickname, question_code, option_code)
JOIN app_user u ON u.nickname = a.nickname AND u.test_user = TRUE AND u.survey_revision > 0;

-- 닉네임이 바뀐 테스트 유저 등 위에서 빠진 테스트 유저는 중간 값으로 채운다
INSERT INTO survey_answer (user_id, question_code, option_code)
SELECT u.id, d.question_code, d.option_code
FROM app_user u
CROSS JOIN (VALUES ('TEMPERATURE_H', 'LONG'), ('TEMPERATURE_T', 'LONG_OK')) AS d (question_code, option_code)
WHERE u.test_user = TRUE AND u.survey_revision > 0
  AND NOT EXISTS (SELECT 1 FROM survey_answer s WHERE s.user_id = u.id AND s.question_code = d.question_code);

-- 페르소나는 답변이 바뀌었으므로 차수를 올려 AI 분석·요약·추천 캐시를 새로 만들게 한다
UPDATE app_user SET survey_revision = survey_revision + 1 WHERE test_user = TRUE AND survey_revision > 0;

-- 5. 설문을 마친 실제 회원은 온도 답이 없으므로 다시 설문하게 한다
-- 차수는 그대로 둔다. 0으로 돌리면 다시 제출했을 때 옛 차수의 캐시가 최신으로 재사용된다
UPDATE app_user SET survey_completed_at = NULL WHERE test_user = FALSE AND survey_revision > 0;
