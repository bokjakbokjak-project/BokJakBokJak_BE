# BokJakBokJak_BE
복작복작 - 대학생 기숙사 룸메이트 매칭 서비스 Backend

## 로컬 실행

Java 21과 저장소의 Gradle Wrapper를 사용합니다. 초기 설정은 Spring Boot 4.1.1과 Gradle 9.7.1입니다. 로컬 앱 실행에는 PostgreSQL 데이터베이스와 Redis 서버가 필요합니다. 테스트는 별도의 메모리 DB(H2)를 사용합니다.

```bash
cp .env.example .env
# .env에서 DB_URL, DB_USERNAME, DB_PASSWORD, REDIS_URL을 로컬 환경에 맞게 수정
set -a
source .env
set +a
export JWT_SECRET_BASE64="$(openssl rand -base64 32)"
./gradlew test
./gradlew bootRun
```

다른 터미널에서 `curl -i http://localhost:8080/api/health`로 확인합니다. Windows에서는 `gradlew.bat`를 사용합니다.

- `DB_URL`은 PostgreSQL JDBC 주소이며 기본값은 `jdbc:postgresql://localhost:5432/bokjakbokjak`입니다. 해당 데이터베이스를 먼저 생성해야 합니다.
- `DB_USERNAME`의 기본값은 `bokjakbokjak`이고, `DB_PASSWORD`는 사용하는 PostgreSQL 계정의 비밀번호입니다.
- `REDIS_URL`의 기본값은 `redis://localhost:6379`입니다. 인증이 필요하면 비밀번호가 포함된 연결 URL을 설정합니다.
- `JWT_SECRET_BASE64`에는 Base64로 인코딩한 32바이트 이상의 비밀 키를 넣습니다.
- `CORS_ALLOWED_ORIGINS`는 허용할 FE 주소를 쉼표로 구분합니다. 로컬 기본값은 `http://localhost:5173`입니다.
- `SERVER_PORT`의 기본값은 `8080`, `JWT_ACCESS_TTL`의 기본값은 `30m`입니다.
- `JWT_ISSUER`와 `JWT_AUDIENCE`의 기본값은 각각 `bokjakbokjak`, `bokjakbokjak-api`입니다.
- `.env.example`은 변수 이름과 예시값만 담습니다. 실제 비밀번호와 JWT 키가 들어간 `.env`는 Git에서 제외됩니다.
- 테이블은 Flyway가 관리합니다. 앱이 뜰 때 `src/main/resources/db/migration`의 SQL을 순서대로 적용하고, JPA `ddl-auto`는 `none`입니다. 스키마를 바꿀 때는 기존 파일을 고치지 말고 `V{다음번호}__설명.sql`을 새로 추가합니다.
- 일반 `jar`는 비활성화하고 실행용 `bootJar`만 만듭니다.

## 공통 HTTP 계약

성공 응답은 `success, code, message, data`, 실패 응답은 `success, code, message, errors`를 사용합니다. 기본 성공 코드는 `COMMON2000`입니다. `ErrorCode`가 공통 오류 코드·메시지·HTTP 상태를 관리하고, `ApiExceptionHandler`가 예외를 같은 형식으로 반환합니다. HTTP 응답 필드명은 snake_case입니다. 보호된 경로는 서비스가 발급한 `Authorization: Bearer <access_token>`을 받습니다.

JPA 엔티티가 생성·수정 시각이 필요하면 `BaseEntity`를 상속합니다. `created_at`과 `updated_at`은 JPA Auditing으로 기록됩니다.

`GET /api/health`를 실행할 수 있습니다. 설문 문항은 `GET /api/surveys/current`로 조회합니다(로그인 필요). `POST /api/auth/login`은 Google 로그인 구현을 연결할 공개 경로로 설정했습니다. Google ID 토큰 검증, PostgreSQL 저장 및 실제 매칭 점수 계산은 후속 기능에서 구현합니다.

## 채희님 연동용 내부 메서드

B 모듈은 `kr.bokjakbokjak.matching`의 인터페이스를 생성자로 주입받습니다.

| 인터페이스 | 메서드 | 반환 |
| --- | --- | --- |
| `UserQueryService` | `findCandidates(Long viewerId)` | `List<CandidateDto>` |
| `UserQueryService` | `getUserSummaries(List<Long> userIds)` | `List<UserSummaryDto>` |
| `MatchingEngine` | `calculate(Long viewerId, Long targetId)` | `MatchResultDto` |

샘플 구현을 쓰려면 `SPRING_PROFILES_ACTIVE=matching-fixture ./gradlew bootRun`으로 실행합니다. 기본 프로필에서는 샘플 구현을 등록하지 않습니다.

- 활성 사용자 `7`과 `12`는 서로 후보로 조회됩니다. 최종 매칭 완료 여부에 따른 추천 제외는 B에서 처리합니다.
- `getUserSummaries`는 탈퇴 샘플 `99`도 `WITHDRAWN` 상태와 함께 반환합니다. 존재하지 않는 ID는 생략합니다.
- `calculate`는 샘플 쌍 `7 ↔ 12`를 지원합니다. `calculate(7L, 12L)`는 두 방향 `0.85 / 0.79`와 상호 점수 `0.82`를 반환합니다. 이 값은 연동용 고정 예시이며 실제 매칭 산식의 결과가 아닙니다.
- `viewerToTarget`는 **viewer의 허용 기준과 가중치**로 본 상대 점수입니다. 화면 퍼센트가 필요하면 B에서 100을 곱합니다. 실제 가중치와 합산식은 엔진 구현에서 확정합니다.
