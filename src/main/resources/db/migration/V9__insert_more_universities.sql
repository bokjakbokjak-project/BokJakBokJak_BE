-- 지원 학교 추가: 서울 주요 대학과 4호선 인근 대학 중 학교 메일이 Google Workspace인 곳
-- 성균관대는 학교 메일(skku.edu)과 Google 계정 도메인(g.skku.edu)이 달라서 Google 계정 도메인을 넣는다
INSERT INTO university (code, name) VALUES
    ('SNU', '서울대학교'),
    ('YONSEI', '연세대학교'),
    ('SKKU', '성균관대학교'),
    ('HANYANG', '한양대학교'),
    ('HUFS', '한국외국어대학교');

INSERT INTO university_domain (domain, university_id)
SELECT d.domain, u.id
FROM (VALUES
    ('snu.ac.kr', 'SNU'),
    ('yonsei.ac.kr', 'YONSEI'),
    ('g.skku.edu', 'SKKU'),
    ('hanyang.ac.kr', 'HANYANG'),
    ('hufs.ac.kr', 'HUFS')) AS d (domain, university_code)
JOIN university u ON u.code = d.university_code;
