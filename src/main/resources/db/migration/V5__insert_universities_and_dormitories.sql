-- 학교 Google 계정 로그인을 지원하는 학교 (학교 메일이 Google Workspace인 곳)
INSERT INTO university (code, name) VALUES
    ('DONGGUK', '동국대학교'),
    ('KOOKMIN', '국민대학교'),
    ('SUNGSHIN', '성신여자대학교'),
    ('HANSUNG', '한성대학교'),
    ('SOOKMYUNG', '숙명여자대학교'),
    ('SEOKYEONG', '서경대학교'),
    ('KOREA', '고려대학교'),
    ('SANGMYUNG', '상명대학교');

INSERT INTO university_domain (domain, university_id)
SELECT d.domain, u.id
FROM (VALUES
    ('dgu.ac.kr', 'DONGGUK'),
    ('dongguk.ac.kr', 'DONGGUK'),
    ('kookmin.ac.kr', 'KOOKMIN'),
    ('sungshin.ac.kr', 'SUNGSHIN'),
    ('hansung.ac.kr', 'HANSUNG'),
    ('sookmyung.ac.kr', 'SOOKMYUNG'),
    ('sm.ac.kr', 'SOOKMYUNG'),
    ('skuniv.ac.kr', 'SEOKYEONG'),
    ('korea.ac.kr', 'KOREA'),
    ('smu.ac.kr', 'SANGMYUNG')) AS d (domain, university_code)
JOIN university u ON u.code = d.university_code;

-- 동국대 기숙사: 서울캠퍼스 남산학사(남녀공용), 바이오메디캠퍼스 고양학사
INSERT INTO dormitory_room_type (university_id, dormitory_key, dormitory_name, name, capacity)
SELECT u.id, r.dormitory_key, r.dormitory_name, r.name, r.capacity
FROM (VALUES
    ('남산학사', '남산학사', '2인실', 2),
    ('남산학사', '남산학사', '1인실(장애인실)', 1),
    ('고양학사', '고양학사', '2인실', 2)) AS r (dormitory_key, dormitory_name, name, capacity)
JOIN university u ON u.code = 'DONGGUK';
