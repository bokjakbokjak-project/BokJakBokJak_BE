-- Google 로그인 회원 식별자(sub)와 학교 이메일
ALTER TABLE app_user ADD COLUMN google_sub VARCHAR(255);
ALTER TABLE app_user ADD COLUMN email VARCHAR(320);

-- 같은 Google 계정으로 동시에 첫 로그인해도 회원은 한 명만 생긴다
CREATE UNIQUE INDEX uk_app_user_google_sub ON app_user (google_sub);
CREATE INDEX idx_app_user_email ON app_user (email);
