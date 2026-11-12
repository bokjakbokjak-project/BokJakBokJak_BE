-- 시연·연동 테스트용 페르소나 13명 (test_user = TRUE, 온보딩·설문 완료 상태)
-- 기본은 동국대 남산학사 2인실. 옆동주민은 다른 기숙사, 옆방타입은 다른 방 타입, 민트초코는 남성이라 책갈피의 후보에서 빠져야 한다

-- 회원 ID는 1000부터 쓴다. 테스트 코드가 임의로 쓰는 작은 ID(7, 12 등)가 실제 회원과 겹치지 않게 한다
ALTER TABLE app_user ALTER COLUMN id RESTART WITH 1000;

INSERT INTO app_user (university_id, nickname, gender, birth_year, department, student_number, room_type_id,
                      status, test_user, onboarding_completed_at, survey_revision, survey_completed_at, created_at, updated_at)
SELECT u.id, p.nickname, p.gender, p.birth_year, p.department, p.student_number, r.id,
       'ACTIVE', TRUE, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES
    ('책갈피', 'FEMALE', 2004, '산업공학과', '20236305', '남산학사', '2인실'),
    ('깔끔이', 'FEMALE', 2003, '화학과', '20223471', '남산학사', '2인실'),
    ('털털이', 'FEMALE', 2005, '경영학과', '20247468', '남산학사', '2인실'),
    ('아침햇살', 'FEMALE', 2004, '디자인학과', '20231791', '남산학사', '2인실'),
    ('새벽감성', 'FEMALE', 2005, '컴퓨터공학과', '20242186', '남산학사', '2인실'),
    ('연기한줄', 'FEMALE', 2002, '기계공학과', '20219779', '남산학사', '2인실'),
    ('드르렁', 'FEMALE', 2004, '영어영문학과', '20232542', '남산학사', '2인실'),
    ('무난이', 'FEMALE', 2004, '사회복지학과', '20236991', '남산학사', '2인실'),
    ('갸우뚱', 'FEMALE', 2006, '미디어디자인학과', '20251950', '남산학사', '2인실'),
    ('금연중', 'FEMALE', 2003, '법학과', '20229313', '남산학사', '2인실'),
    ('민트초코', 'MALE', 2003, '컴퓨터공학과', '20224517', '남산학사', '2인실'),
    ('옆동주민', 'FEMALE', 2004, '수학과', '20231614', '고양학사', '2인실'),
    ('옆방타입', 'FEMALE', 2004, '국어국문학과', '20232408', '남산학사', '1인실(장애인실)')) AS p (nickname, gender, birth_year, department, student_number, dormitory_key, room_type_name)
JOIN university u ON u.code = 'DONGGUK'
JOIN dormitory_room_type r ON r.university_id = u.id AND r.dormitory_key = p.dormitory_key AND r.name = p.room_type_name;

INSERT INTO user_weight (user_id, category_code, priority_rank)
SELECT u.id, w.category_code, w.priority_rank
FROM (VALUES
    ('책갈피', 'CLEANING', 1),
    ('책갈피', 'SLEEP_SCHEDULE', 2),
    ('책갈피', 'SMOKING', 3),
    ('깔끔이', 'CLEANING', 1),
    ('깔끔이', 'PERSONAL_HYGIENE', 2),
    ('깔끔이', 'SLEEP_SCHEDULE', 3),
    ('털털이', 'SLEEP_HABITS', 1),
    ('털털이', 'SMOKING', 2),
    ('털털이', 'SLEEP_SCHEDULE', 3),
    ('아침햇살', 'SLEEP_SCHEDULE', 1),
    ('아침햇살', 'SLEEP_HABITS', 2),
    ('아침햇살', 'CLEANING', 3),
    ('새벽감성', 'SLEEP_SCHEDULE', 1),
    ('새벽감성', 'PERSONAL_HYGIENE', 2),
    ('새벽감성', 'CLEANING', 3),
    ('연기한줄', 'CLEANING', 1),
    ('연기한줄', 'SLEEP_SCHEDULE', 2),
    ('연기한줄', 'PERSONAL_HYGIENE', 3),
    ('드르렁', 'SLEEP_HABITS', 1),
    ('드르렁', 'CLEANING', 2),
    ('드르렁', 'SMOKING', 3),
    ('무난이', 'PERSONAL_HYGIENE', 1),
    ('무난이', 'CLEANING', 2),
    ('무난이', 'SLEEP_HABITS', 3),
    ('갸우뚱', 'SMOKING', 1),
    ('갸우뚱', 'CLEANING', 2),
    ('갸우뚱', 'SLEEP_SCHEDULE', 3),
    ('금연중', 'CLEANING', 1),
    ('금연중', 'PERSONAL_HYGIENE', 2),
    ('금연중', 'SLEEP_HABITS', 3),
    ('민트초코', 'SMOKING', 1),
    ('민트초코', 'CLEANING', 2),
    ('민트초코', 'PERSONAL_HYGIENE', 3),
    ('옆동주민', 'CLEANING', 1),
    ('옆동주민', 'SLEEP_SCHEDULE', 2),
    ('옆동주민', 'SMOKING', 3),
    ('옆방타입', 'CLEANING', 1),
    ('옆방타입', 'SLEEP_SCHEDULE', 2),
    ('옆방타입', 'SMOKING', 3)) AS w (nickname, category_code, priority_rank)
JOIN app_user u ON u.nickname = w.nickname AND u.test_user = TRUE;

INSERT INTO survey_answer (user_id, question_code, option_code)
SELECT u.id, a.question_code, a.option_code
FROM (VALUES
    ('책갈피', 'SLEEP_HABITS_H', 'RARE'), ('책갈피', 'SLEEP_HABITS_T', 'RARE_OK'), ('책갈피', 'BEDTIME_H', 'FROM_23_TO_24'), ('책갈피', 'BEDTIME_T', 'BY_24'), ('책갈피', 'ALARM_H', 'AT_MOST_2'), ('책갈피', 'ALARM_T', 'UP_TO_2'), ('책갈피', 'FLOOR_CLEAN_H', 'TWO_THREE_WEEKLY'), ('책갈피', 'FLOOR_CLEAN_T', 'WEEKLY_MIN'),
    ('책갈피', 'FOOD_WASTE_H', 'SAME_DAY'), ('책갈피', 'FOOD_WASTE_T', 'SAME_DAY_OK'), ('책갈피', 'BELONGINGS_H', 'SAME_DAY'), ('책갈피', 'BELONGINGS_T', 'SAME_DAY_OK'), ('책갈피', 'DISHES_H', 'SAME_DAY'), ('책갈피', 'DISHES_T', 'SAME_DAY_OK'), ('책갈피', 'SMOKING_H', 'NON_SMOKER'), ('책갈피', 'SMOKING_T', 'NON_SMOKER_ONLY'),
    ('깔끔이', 'SLEEP_HABITS_H', 'NONE'), ('깔끔이', 'SLEEP_HABITS_T', 'RARE_OK'), ('깔끔이', 'BEDTIME_H', 'FROM_22_TO_23'), ('깔끔이', 'BEDTIME_T', 'BY_23'), ('깔끔이', 'ALARM_H', 'ONCE'), ('깔끔이', 'ALARM_T', 'UP_TO_2'), ('깔끔이', 'FLOOR_CLEAN_H', 'DAILY'), ('깔끔이', 'FLOOR_CLEAN_T', 'TWO_THREE_WEEKLY_MIN'),
    ('깔끔이', 'FOOD_WASTE_H', 'IMMEDIATELY'), ('깔끔이', 'FOOD_WASTE_T', 'IMMEDIATELY_ONLY'), ('깔끔이', 'BELONGINGS_H', 'IMMEDIATELY'), ('깔끔이', 'BELONGINGS_T', 'SAME_DAY_OK'), ('깔끔이', 'DISHES_H', 'IMMEDIATELY'), ('깔끔이', 'DISHES_T', 'IMMEDIATELY_ONLY'), ('깔끔이', 'SMOKING_H', 'NON_SMOKER'), ('깔끔이', 'SMOKING_T', 'PREFER_NON_SMOKER'),
    ('털털이', 'SLEEP_HABITS_H', 'OCCASIONAL'), ('털털이', 'SLEEP_HABITS_T', 'FREQUENT_OK'), ('털털이', 'BEDTIME_H', 'FROM_00_TO_01'), ('털털이', 'BEDTIME_T', 'BY_02'), ('털털이', 'ALARM_H', 'THREE_TO_FOUR'), ('털털이', 'ALARM_T', 'MANY_OK'), ('털털이', 'FLOOR_CLEAN_H', 'RARELY'), ('털털이', 'FLOOR_CLEAN_T', 'WHEN_NEEDED'),
    ('털털이', 'FOOD_WASTE_H', 'NEXT_DAY'), ('털털이', 'FOOD_WASTE_T', 'NO_ISSUE_OK'), ('털털이', 'BELONGINGS_H', 'WHEN_NEEDED'), ('털털이', 'BELONGINGS_T', 'NO_SPACE_INVASION'), ('털털이', 'DISHES_H', 'TWO_DAYS_PLUS'), ('털털이', 'DISHES_T', 'NO_ISSUE_OK'), ('털털이', 'SMOKING_H', 'NON_SMOKER'), ('털털이', 'SMOKING_T', 'ANY_OK'),
    ('아침햇살', 'SLEEP_HABITS_H', 'NONE'), ('아침햇살', 'SLEEP_HABITS_T', 'RARE_OK'), ('아침햇살', 'BEDTIME_H', 'BEFORE_22'), ('아침햇살', 'BEDTIME_T', 'BY_23'), ('아침햇살', 'ALARM_H', 'ONCE'), ('아침햇살', 'ALARM_T', 'ONCE_ONLY'), ('아침햇살', 'FLOOR_CLEAN_H', 'WEEKLY'), ('아침햇살', 'FLOOR_CLEAN_T', 'WEEKLY_MIN'),
    ('아침햇살', 'FOOD_WASTE_H', 'SAME_DAY'), ('아침햇살', 'FOOD_WASTE_T', 'SAME_DAY_OK'), ('아침햇살', 'BELONGINGS_H', 'SAME_DAY'), ('아침햇살', 'BELONGINGS_T', 'FEW_DAYS_OK'), ('아침햇살', 'DISHES_H', 'SAME_DAY'), ('아침햇살', 'DISHES_T', 'NEXT_DAY_OK'), ('아침햇살', 'SMOKING_H', 'NON_SMOKER'), ('아침햇살', 'SMOKING_T', 'PREFER_NON_SMOKER'),
    ('새벽감성', 'SLEEP_HABITS_H', 'RARE'), ('새벽감성', 'SLEEP_HABITS_T', 'OCCASIONAL_OK'), ('새벽감성', 'BEDTIME_H', 'AFTER_02'), ('새벽감성', 'BEDTIME_T', 'QUIET_ANY'), ('새벽감성', 'ALARM_H', 'FIVE_OR_MORE'), ('새벽감성', 'ALARM_T', 'MANY_OK'), ('새벽감성', 'FLOOR_CLEAN_H', 'BIWEEKLY'), ('새벽감성', 'FLOOR_CLEAN_T', 'BIWEEKLY_MIN'),
    ('새벽감성', 'FOOD_WASTE_H', 'NEXT_DAY'), ('새벽감성', 'FOOD_WASTE_T', 'NEXT_DAY_OK'), ('새벽감성', 'BELONGINGS_H', 'FEW_DAYS'), ('새벽감성', 'BELONGINGS_T', 'FEW_DAYS_OK'), ('새벽감성', 'DISHES_H', 'NEXT_DAY'), ('새벽감성', 'DISHES_T', 'NEXT_DAY_OK'), ('새벽감성', 'SMOKING_H', 'NON_SMOKER'), ('새벽감성', 'SMOKING_T', 'ANY_OK'),
    ('연기한줄', 'SLEEP_HABITS_H', 'RARE'), ('연기한줄', 'SLEEP_HABITS_T', 'OCCASIONAL_OK'), ('연기한줄', 'BEDTIME_H', 'FROM_23_TO_24'), ('연기한줄', 'BEDTIME_T', 'BY_01'), ('연기한줄', 'ALARM_H', 'AT_MOST_2'), ('연기한줄', 'ALARM_T', 'UP_TO_4'), ('연기한줄', 'FLOOR_CLEAN_H', 'WEEKLY'), ('연기한줄', 'FLOOR_CLEAN_T', 'WEEKLY_MIN'),
    ('연기한줄', 'FOOD_WASTE_H', 'SAME_DAY'), ('연기한줄', 'FOOD_WASTE_T', 'SAME_DAY_OK'), ('연기한줄', 'BELONGINGS_H', 'SAME_DAY'), ('연기한줄', 'BELONGINGS_T', 'SAME_DAY_OK'), ('연기한줄', 'DISHES_H', 'SAME_DAY'), ('연기한줄', 'DISHES_T', 'SAME_DAY_OK'), ('연기한줄', 'SMOKING_H', 'OUTDOOR_SMOKER'), ('연기한줄', 'SMOKING_T', 'ANY_OK'),
    ('드르렁', 'SLEEP_HABITS_H', 'FREQUENT'), ('드르렁', 'SLEEP_HABITS_T', 'FREQUENT_OK'), ('드르렁', 'BEDTIME_H', 'FROM_23_TO_24'), ('드르렁', 'BEDTIME_T', 'BY_24'), ('드르렁', 'ALARM_H', 'AT_MOST_2'), ('드르렁', 'ALARM_T', 'UP_TO_4'), ('드르렁', 'FLOOR_CLEAN_H', 'TWO_THREE_WEEKLY'), ('드르렁', 'FLOOR_CLEAN_T', 'WEEKLY_MIN'),
    ('드르렁', 'FOOD_WASTE_H', 'SAME_DAY'), ('드르렁', 'FOOD_WASTE_T', 'SAME_DAY_OK'), ('드르렁', 'BELONGINGS_H', 'SAME_DAY'), ('드르렁', 'BELONGINGS_T', 'SAME_DAY_OK'), ('드르렁', 'DISHES_H', 'SAME_DAY'), ('드르렁', 'DISHES_T', 'SAME_DAY_OK'), ('드르렁', 'SMOKING_H', 'NON_SMOKER'), ('드르렁', 'SMOKING_T', 'NON_SMOKER_ONLY'),
    ('무난이', 'SLEEP_HABITS_H', 'RARE'), ('무난이', 'SLEEP_HABITS_T', 'OCCASIONAL_OK'), ('무난이', 'BEDTIME_H', 'FROM_23_TO_24'), ('무난이', 'BEDTIME_T', 'BY_01'), ('무난이', 'ALARM_H', 'AT_MOST_2'), ('무난이', 'ALARM_T', 'UP_TO_4'), ('무난이', 'FLOOR_CLEAN_H', 'TWO_THREE_WEEKLY'), ('무난이', 'FLOOR_CLEAN_T', 'WEEKLY_MIN'),
    ('무난이', 'FOOD_WASTE_H', 'SAME_DAY'), ('무난이', 'FOOD_WASTE_T', 'NEXT_DAY_OK'), ('무난이', 'BELONGINGS_H', 'SAME_DAY'), ('무난이', 'BELONGINGS_T', 'FEW_DAYS_OK'), ('무난이', 'DISHES_H', 'SAME_DAY'), ('무난이', 'DISHES_T', 'NEXT_DAY_OK'), ('무난이', 'SMOKING_H', 'NON_SMOKER'), ('무난이', 'SMOKING_T', 'OUTSIDE_LOW_ODOR'),
    ('갸우뚱', 'SLEEP_HABITS_H', 'UNKNOWN'), ('갸우뚱', 'SLEEP_HABITS_T', 'OCCASIONAL_OK'), ('갸우뚱', 'BEDTIME_H', 'IRREGULAR'), ('갸우뚱', 'BEDTIME_T', 'BY_01'), ('갸우뚱', 'ALARM_H', 'THREE_TO_FOUR'), ('갸우뚱', 'ALARM_T', 'UP_TO_4'), ('갸우뚱', 'FLOOR_CLEAN_H', 'WEEKLY'), ('갸우뚱', 'FLOOR_CLEAN_T', 'WEEKLY_MIN'),
    ('갸우뚱', 'FOOD_WASTE_H', 'NEXT_DAY'), ('갸우뚱', 'FOOD_WASTE_T', 'SAME_DAY_OK'), ('갸우뚱', 'BELONGINGS_H', 'FEW_DAYS'), ('갸우뚱', 'BELONGINGS_T', 'SAME_DAY_OK'), ('갸우뚱', 'DISHES_H', 'NEXT_DAY'), ('갸우뚱', 'DISHES_T', 'SAME_DAY_OK'), ('갸우뚱', 'SMOKING_H', 'NON_SMOKER'), ('갸우뚱', 'SMOKING_T', 'NON_SMOKER_ONLY'),
    ('금연중', 'SLEEP_HABITS_H', 'NONE'), ('금연중', 'SLEEP_HABITS_T', 'RARE_OK'), ('금연중', 'BEDTIME_H', 'FROM_23_TO_24'), ('금연중', 'BEDTIME_T', 'BY_24'), ('금연중', 'ALARM_H', 'AT_MOST_2'), ('금연중', 'ALARM_T', 'UP_TO_2'), ('금연중', 'FLOOR_CLEAN_H', 'TWO_THREE_WEEKLY'), ('금연중', 'FLOOR_CLEAN_T', 'TWO_THREE_WEEKLY_MIN'),
    ('금연중', 'FOOD_WASTE_H', 'SAME_DAY'), ('금연중', 'FOOD_WASTE_T', 'SAME_DAY_OK'), ('금연중', 'BELONGINGS_H', 'SAME_DAY'), ('금연중', 'BELONGINGS_T', 'SAME_DAY_OK'), ('금연중', 'DISHES_H', 'SAME_DAY'), ('금연중', 'DISHES_T', 'SAME_DAY_OK'), ('금연중', 'SMOKING_H', 'QUIT_IN_DORM'), ('금연중', 'SMOKING_T', 'PREFER_NON_SMOKER'),
    ('민트초코', 'SLEEP_HABITS_H', 'RARE'), ('민트초코', 'SLEEP_HABITS_T', 'RARE_OK'), ('민트초코', 'BEDTIME_H', 'FROM_23_TO_24'), ('민트초코', 'BEDTIME_T', 'BY_24'), ('민트초코', 'ALARM_H', 'AT_MOST_2'), ('민트초코', 'ALARM_T', 'UP_TO_2'), ('민트초코', 'FLOOR_CLEAN_H', 'TWO_THREE_WEEKLY'), ('민트초코', 'FLOOR_CLEAN_T', 'WEEKLY_MIN'),
    ('민트초코', 'FOOD_WASTE_H', 'SAME_DAY'), ('민트초코', 'FOOD_WASTE_T', 'SAME_DAY_OK'), ('민트초코', 'BELONGINGS_H', 'SAME_DAY'), ('민트초코', 'BELONGINGS_T', 'SAME_DAY_OK'), ('민트초코', 'DISHES_H', 'SAME_DAY'), ('민트초코', 'DISHES_T', 'SAME_DAY_OK'), ('민트초코', 'SMOKING_H', 'NON_SMOKER'), ('민트초코', 'SMOKING_T', 'NON_SMOKER_ONLY'),
    ('옆동주민', 'SLEEP_HABITS_H', 'RARE'), ('옆동주민', 'SLEEP_HABITS_T', 'RARE_OK'), ('옆동주민', 'BEDTIME_H', 'FROM_23_TO_24'), ('옆동주민', 'BEDTIME_T', 'BY_24'), ('옆동주민', 'ALARM_H', 'AT_MOST_2'), ('옆동주민', 'ALARM_T', 'UP_TO_2'), ('옆동주민', 'FLOOR_CLEAN_H', 'TWO_THREE_WEEKLY'), ('옆동주민', 'FLOOR_CLEAN_T', 'WEEKLY_MIN'),
    ('옆동주민', 'FOOD_WASTE_H', 'SAME_DAY'), ('옆동주민', 'FOOD_WASTE_T', 'SAME_DAY_OK'), ('옆동주민', 'BELONGINGS_H', 'SAME_DAY'), ('옆동주민', 'BELONGINGS_T', 'SAME_DAY_OK'), ('옆동주민', 'DISHES_H', 'SAME_DAY'), ('옆동주민', 'DISHES_T', 'SAME_DAY_OK'), ('옆동주민', 'SMOKING_H', 'NON_SMOKER'), ('옆동주민', 'SMOKING_T', 'NON_SMOKER_ONLY'),
    ('옆방타입', 'SLEEP_HABITS_H', 'RARE'), ('옆방타입', 'SLEEP_HABITS_T', 'RARE_OK'), ('옆방타입', 'BEDTIME_H', 'FROM_23_TO_24'), ('옆방타입', 'BEDTIME_T', 'BY_24'), ('옆방타입', 'ALARM_H', 'AT_MOST_2'), ('옆방타입', 'ALARM_T', 'UP_TO_2'), ('옆방타입', 'FLOOR_CLEAN_H', 'TWO_THREE_WEEKLY'), ('옆방타입', 'FLOOR_CLEAN_T', 'WEEKLY_MIN'),
    ('옆방타입', 'FOOD_WASTE_H', 'SAME_DAY'), ('옆방타입', 'FOOD_WASTE_T', 'SAME_DAY_OK'), ('옆방타입', 'BELONGINGS_H', 'SAME_DAY'), ('옆방타입', 'BELONGINGS_T', 'SAME_DAY_OK'), ('옆방타입', 'DISHES_H', 'SAME_DAY'), ('옆방타입', 'DISHES_T', 'SAME_DAY_OK'), ('옆방타입', 'SMOKING_H', 'NON_SMOKER'), ('옆방타입', 'SMOKING_T', 'NON_SMOKER_ONLY')) AS a (nickname, question_code, option_code)
JOIN app_user u ON u.nickname = a.nickname AND u.test_user = TRUE;
