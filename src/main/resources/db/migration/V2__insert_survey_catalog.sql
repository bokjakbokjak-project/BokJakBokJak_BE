-- 설문 16문항 (2026-11-11 확정 문구)

INSERT INTO survey_category (code, name, display_order) VALUES
    ('SLEEP_HABITS', '잠버릇', 1),
    ('SLEEP_SCHEDULE', '기상 알람', 2),
    ('CLEANING', '청소 청결', 3),
    ('PERSONAL_HYGIENE', '개인 청결', 4),
    ('SMOKING', '흡연 유무', 5);

INSERT INTO survey_topic (code, category_code, scenario_text, display_order) VALUES
    ('SLEEP_HABITS', 'SLEEP_HABITS', '밤에 함께 자고 있는데, 한 사람이 자는 동안 코를 골거나 잠꼬대를 해요.', 1),
    ('BEDTIME', 'SLEEP_SCHEDULE', '밤 12시가 되었어요. 한 사람은 자려고 하고, 다른 한 사람은 아직 잠들지 않았어요.', 2),
    ('ALARM', 'SLEEP_SCHEDULE', '아침 7시, 알람이 울렸어요. 그런데 알람이 한 번으로 끝나지 않고 여러 번 반복돼요.', 3),
    ('FLOOR_CLEAN', 'CLEANING', '방 바닥에 머리카락이나 먼지가 조금씩 보이기 시작했어요.', 4),
    ('FOOD_WASTE', 'CLEANING', '배달 음식을 먹고 남은 음식과 포장 용기가 방 안에 놓여 있어요. 아직 냄새가 심하게 나지는 않아요.', 5),
    ('BELONGINGS', 'PERSONAL_HYGIENE', '옷, 가방, 책 같은 개인 물건이 방 안에 꺼내져 있어요.', 6),
    ('DISHES', 'PERSONAL_HYGIENE', '방 안 책상 위에 사용한 컵이나 그릇 같은 식기가 놓여 있어요.', 7),
    ('SMOKING', 'SMOKING', '룸메이트가 밖에서 담배를 피우고 방으로 돌아왔어요. 방 안에서는 피우지 않았지만 몸이나 옷에서 냄새가 날 수 있어요.', 8);

INSERT INTO question (code, topic_code, kind, question_number, prompt) VALUES
    ('SLEEP_HABITS_H', 'SLEEP_HABITS', 'HABIT', 1, '나는 코골이, 잠꼬대 같은 잠버릇이 어느 정도 있는 편인가요?'),
    ('SLEEP_HABITS_T', 'SLEEP_HABITS', 'TOLERANCE', 2, '룸메이트의 코골이, 잠꼬대는 어느 정도까지 괜찮을까요?'),
    ('BEDTIME_H', 'BEDTIME', 'HABIT', 3, '평일에 나는 보통 몇 시쯤 잠자리에 드나요?'),
    ('BEDTIME_T', 'BEDTIME', 'TOLERANCE', 4, '룸메이트가 방 안에서 깨어 있어도 괜찮은 마감 시간은 언제까지인가요?'),
    ('ALARM_H', 'ALARM', 'HABIT', 5, '나는 아침에 알람을 보통 어떻게 사용하는 편인가요?'),
    ('ALARM_T', 'ALARM', 'TOLERANCE', 6, '룸메이트의 알람은 어디까지 괜찮을까요?'),
    ('FLOOR_CLEAN_H', 'FLOOR_CLEAN', 'HABIT', 7, '나는 방 바닥을 보통 얼마나 자주 청소하나요?'),
    ('FLOOR_CLEAN_T', 'FLOOR_CLEAN', 'TOLERANCE', 8, '룸메이트는 최소 어느 정도로 바닥 청소를 해주면 괜찮을까요?'),
    ('FOOD_WASTE_H', 'FOOD_WASTE', 'HABIT', 9, '먹다 남긴 음식이나 냄새날 수 있는 쓰레기는 보통 언제 치우나요?'),
    ('FOOD_WASTE_T', 'FOOD_WASTE', 'TOLERANCE', 10, '룸메이트가 먹다 남긴 음식이나 냄새나는 쓰레기를 두었다면 언제까지 괜찮을까요?'),
    ('BELONGINGS_H', 'BELONGINGS', 'HABIT', 11, '나는 개인 물건을 사용한 뒤 보통 언제 정리하나요?'),
    ('BELONGINGS_T', 'BELONGINGS', 'TOLERANCE', 12, '룸메이트가 개인 물건을 꺼내놓는 것은 어디까지 괜찮을까요?'),
    ('DISHES_H', 'DISHES', 'HABIT', 13, '나는 사용한 컵이나 식기를 보통 언제 정리하나요?'),
    ('DISHES_T', 'DISHES', 'TOLERANCE', 14, '룸메이트가 사용한 컵이나 식기를 방 안에 두고 있다면 언제까지 괜찮을까요?'),
    ('SMOKING_H', 'SMOKING', 'HABIT', 15, '나는 흡연과 관련해 어느 쪽에 해당하나요?'),
    ('SMOKING_T', 'SMOKING', 'TOLERANCE', 16, '룸메이트의 흡연은 어느 정도까지 괜찮을까요?');

INSERT INTO question_option (question_code, code, label, display_order) VALUES
    -- Q1
    ('SLEEP_HABITS_H', 'FREQUENT', '자주 있다는 말을 들었다', 1),
    ('SLEEP_HABITS_H', 'OCCASIONAL', '가끔 있다는 말을 들었다', 2),
    ('SLEEP_HABITS_H', 'RARE', '예전에 한두 번 들은 적 있다', 3),
    ('SLEEP_HABITS_H', 'NONE', '없다는 말을 들었다', 4),
    ('SLEEP_HABITS_H', 'UNKNOWN', '잘 모르겠다', 5),
    -- Q2
    ('SLEEP_HABITS_T', 'FREQUENT_OK', '자주 있어도 괜찮다', 1),
    ('SLEEP_HABITS_T', 'OCCASIONAL_OK', '가끔 있는 정도는 괜찮다', 2),
    ('SLEEP_HABITS_T', 'RARE_OK', '거의 없는 편이면 괜찮다', 3),
    ('SLEEP_HABITS_T', 'NONE_ONLY', '없었으면 좋겠다', 4),
    -- Q3
    ('BEDTIME_H', 'BEFORE_22', '밤 10시 이전', 1),
    ('BEDTIME_H', 'FROM_22_TO_23', '밤 10시~11시', 2),
    ('BEDTIME_H', 'FROM_23_TO_24', '밤 11시~12시', 3),
    ('BEDTIME_H', 'FROM_00_TO_01', '새벽 12시~1시', 4),
    ('BEDTIME_H', 'FROM_01_TO_02', '새벽 1시~2시', 5),
    ('BEDTIME_H', 'AFTER_02', '새벽 2시 이후', 6),
    ('BEDTIME_H', 'IRREGULAR', '일정하지 않다', 7),
    -- Q4
    ('BEDTIME_T', 'BY_23', '밤 11시까지', 1),
    ('BEDTIME_T', 'BY_24', '밤 12시까지', 2),
    ('BEDTIME_T', 'BY_01', '새벽 1시까지', 3),
    ('BEDTIME_T', 'BY_02', '새벽 2시까지', 4),
    ('BEDTIME_T', 'QUIET_ANY', '조용하기만 하면 상관없다', 5),
    -- Q5
    ('ALARM_H', 'ONCE', '첫 알람에 바로 일어난다', 1),
    ('ALARM_H', 'AT_MOST_2', '2번 이내로 끄고 일어난다', 2),
    ('ALARM_H', 'THREE_TO_FOUR', '3~4번 정도 반복한다', 3),
    ('ALARM_H', 'FIVE_OR_MORE', '5번 이상 반복하는 편이다', 4),
    ('ALARM_H', 'NO_ALARM', '알람을 거의 사용하지 않는다', 5),
    -- Q6
    ('ALARM_T', 'MANY_OK', '여러 번 울려도 괜찮다', 1),
    ('ALARM_T', 'UP_TO_4', '3~4번까지는 괜찮다', 2),
    ('ALARM_T', 'UP_TO_2', '2번 정도까지만 괜찮다', 3),
    ('ALARM_T', 'ONCE_ONLY', '한 번만 울렸으면 좋겠다', 4),
    -- Q7
    ('FLOOR_CLEAN_H', 'DAILY', '거의 매일 한다', 1),
    ('FLOOR_CLEAN_H', 'TWO_THREE_WEEKLY', '주 2~3회 한다', 2),
    ('FLOOR_CLEAN_H', 'WEEKLY', '주 1회 한다', 3),
    ('FLOOR_CLEAN_H', 'BIWEEKLY', '2주에 1회 정도 한다', 4),
    ('FLOOR_CLEAN_H', 'RARELY', '더 드물게 한다', 5),
    -- Q8
    ('FLOOR_CLEAN_T', 'DAILY_MIN', '거의 매일 해야 한다', 1),
    ('FLOOR_CLEAN_T', 'TWO_THREE_WEEKLY_MIN', '주 2~3회는 해야 한다', 2),
    ('FLOOR_CLEAN_T', 'WEEKLY_MIN', '주 1회면 괜찮다', 3),
    ('FLOOR_CLEAN_T', 'BIWEEKLY_MIN', '2주 1회면 괜찮다', 4),
    ('FLOOR_CLEAN_T', 'WHEN_NEEDED', '필요할 때만 해도 괜찮다', 5),
    -- Q9
    ('FOOD_WASTE_H', 'IMMEDIATELY', '먹은 직후 바로 치운다', 1),
    ('FOOD_WASTE_H', 'SAME_DAY', '그날 안에 치운다', 2),
    ('FOOD_WASTE_H', 'NEXT_DAY', '다음 날 치우기도 한다', 3),
    ('FOOD_WASTE_H', 'TWO_DAYS_PLUS', '이틀 이상 두는 경우도 있다', 4),
    ('FOOD_WASTE_H', 'NOT_IN_ROOM', '방에서 음식을 먹지 않는다', 5),
    -- Q10
    ('FOOD_WASTE_T', 'IMMEDIATELY_ONLY', '먹은 직후 바로 치워줬으면 좋겠다', 1),
    ('FOOD_WASTE_T', 'SAME_DAY_OK', '그날 안에만 치우면 괜찮다', 2),
    ('FOOD_WASTE_T', 'NEXT_DAY_OK', '다음 날까지는 괜찮다', 3),
    ('FOOD_WASTE_T', 'NO_ISSUE_OK', '냄새나 위생 문제가 없다면 더 두어도 괜찮다', 4),
    -- Q11
    ('BELONGINGS_H', 'IMMEDIATELY', '바로 정리한다', 1),
    ('BELONGINGS_H', 'SAME_DAY', '그날 안에 정리한다', 2),
    ('BELONGINGS_H', 'FEW_DAYS', '며칠 모아두었다가 정리한다', 3),
    ('BELONGINGS_H', 'WHEN_NEEDED', '크게 신경 쓰지 않고 필요할 때 정리한다', 4),
    -- Q12
    ('BELONGINGS_T', 'IMMEDIATELY_ONLY', '바로 정리했으면 좋겠다', 1),
    ('BELONGINGS_T', 'SAME_DAY_OK', '그날 안에는 정리했으면 좋겠다', 2),
    ('BELONGINGS_T', 'FEW_DAYS_OK', '며칠 정도는 괜찮다', 3),
    ('BELONGINGS_T', 'NO_SPACE_INVASION', '내 공간만 침범하지 않으면 괜찮다', 4),
    -- Q13
    ('DISHES_H', 'IMMEDIATELY', '사용 직후 바로 정리한다', 1),
    ('DISHES_H', 'SAME_DAY', '그날 안에 정리한다', 2),
    ('DISHES_H', 'NEXT_DAY', '다음 날 정리하기도 한다', 3),
    ('DISHES_H', 'TWO_DAYS_PLUS', '이틀 이상 두는 경우도 있다', 4),
    ('DISHES_H', 'NOT_IN_ROOM', '방에서 식기를 거의 사용하지 않는다', 5),
    -- Q14
    ('DISHES_T', 'IMMEDIATELY_ONLY', '사용 직후 바로 정리했으면 좋겠다', 1),
    ('DISHES_T', 'SAME_DAY_OK', '그날 안에만 정리하면 괜찮다', 2),
    ('DISHES_T', 'NEXT_DAY_OK', '다음 날까지는 괜찮다', 3),
    ('DISHES_T', 'NO_ISSUE_OK', '위생 문제가 없다면 더 두어도 괜찮다', 4),
    -- Q15
    ('SMOKING_H', 'NON_SMOKER', '비흡연자다', 1),
    ('SMOKING_H', 'QUIT_IN_DORM', '흡연하지만 기숙사 생활에서는 하지 않을 예정이다', 2),
    ('SMOKING_H', 'OUTDOOR_SMOKER', '흡연하며 지정된 실외 장소를 이용할 예정이다', 3),
    -- Q16
    ('SMOKING_T', 'ANY_OK', '흡연 여부는 상관없다', 1),
    ('SMOKING_T', 'OUTSIDE_LOW_ODOR', '실외에서만 하고 냄새가 거의 없으면 괜찮다', 2),
    ('SMOKING_T', 'PREFER_NON_SMOKER', '가능한 비흡연자였으면 좋겠다', 3),
    ('SMOKING_T', 'NON_SMOKER_ONLY', '흡연자와는 같은 방이 어렵다', 4);
