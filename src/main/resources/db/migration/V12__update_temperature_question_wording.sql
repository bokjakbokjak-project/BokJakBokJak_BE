-- 온도 문항 질문 문구 최종안 (2026-11-20)
-- 하나만 고르는 문항으로 확정되어 질문에서 "모두"를 뺀다. 선택지와 상황 문구, 코드는 그대로다

UPDATE question SET prompt = '평소 냉난방과 관련해 내 경험과 가까운 것을 골라주세요'
WHERE code = 'TEMPERATURE_H';

UPDATE question SET prompt = '룸메이트의 냉난방 습관 중, 함께 지내기 힘들 것 같은 것을 골라 주세요.'
WHERE code = 'TEMPERATURE_T';
