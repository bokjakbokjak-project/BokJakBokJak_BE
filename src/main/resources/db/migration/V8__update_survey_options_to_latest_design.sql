-- 설문 선택지를 최신 디자인 문구로 맞춘다 (2026-11-13)
-- 취침 시간(Q3)을 1시간 구간에서 2시간 구간으로 바꾸고 기존 답변을 새 구간으로 옮긴다

-- 표시 순서 UNIQUE 제약을 피하려고 기존 순서를 잠시 뒤로 밀어둔다
UPDATE question_option SET display_order = display_order + 100 WHERE question_code = 'BEDTIME_H';

INSERT INTO question_option (question_code, code, label, display_order) VALUES
    ('BEDTIME_H', 'FROM_22_TO_24', '밤 10시~12시', 200),
    ('BEDTIME_H', 'FROM_00_TO_02', '새벽 12시~2시', 201);

UPDATE survey_answer SET option_code = 'FROM_22_TO_24'
WHERE question_code = 'BEDTIME_H' AND option_code IN ('FROM_22_TO_23', 'FROM_23_TO_24');

UPDATE survey_answer SET option_code = 'FROM_00_TO_02'
WHERE question_code = 'BEDTIME_H' AND option_code IN ('FROM_00_TO_01', 'FROM_01_TO_02');

DELETE FROM question_option
WHERE question_code = 'BEDTIME_H'
  AND code IN ('FROM_22_TO_23', 'FROM_23_TO_24', 'FROM_00_TO_01', 'FROM_01_TO_02');

UPDATE question_option SET display_order = 1 WHERE question_code = 'BEDTIME_H' AND code = 'BEFORE_22';
UPDATE question_option SET display_order = 2 WHERE question_code = 'BEDTIME_H' AND code = 'FROM_22_TO_24';
UPDATE question_option SET display_order = 3 WHERE question_code = 'BEDTIME_H' AND code = 'FROM_00_TO_02';
UPDATE question_option SET display_order = 4 WHERE question_code = 'BEDTIME_H' AND code = 'AFTER_02';
UPDATE question_option SET display_order = 5 WHERE question_code = 'BEDTIME_H' AND code = 'IRREGULAR';

-- 의미는 같고 문구만 바뀐 선택지
UPDATE question_option SET label = '큰 소음이 아니라면 상관없다'
WHERE question_code = 'BEDTIME_T' AND code = 'QUIET_ANY';

UPDATE question_option SET label = '흡연자이지만 기숙사에서는 자제할 예정이다'
WHERE question_code = 'SMOKING_H' AND code = 'QUIT_IN_DORM';

UPDATE survey_topic SET scenario_text = '옷과 가방, 책 같은 개인 물건이 방 안에 꺼내져 있어요.'
WHERE code = 'BELONGINGS';
