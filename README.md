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
- 앱이 뜰 때 Flyway가 `src/main/resources/db/migration`의 SQL을 먼저 적용하고, 그다음 JPA `ddl-auto: update`가 마이그레이션에 없는 테이블을 만듭니다.
  - 회원·설문·온보딩·기숙사 테이블은 Flyway로 관리합니다. 스키마를 바꿀 때는 기존 파일을 고치지 말고 `V{다음번호}__설명.sql`을 새로 추가합니다.
  - 추천·채팅·조율 테이블은 엔티티 기준으로 `update`가 만듭니다. 같은 테이블을 Flyway와 엔티티 양쪽에서 만들지 않습니다.
- 일반 `jar`는 비활성화하고 실행용 `bootJar`만 만듭니다.

## 공통 HTTP 계약

성공 응답은 `success, code, message, data`, 실패 응답은 `success, code, message, errors`를 사용합니다. 기본 성공 코드는 `COMMON2000`입니다. `ErrorCode`가 공통 오류 코드·메시지·HTTP 상태를 관리하고, `ApiExceptionHandler`가 예외를 같은 형식으로 반환합니다. HTTP 응답 필드명은 snake_case입니다. 보호된 경로는 서비스가 발급한 `Authorization: Bearer <access_token>`을 받습니다.

JPA 엔티티가 생성·수정 시각이 필요하면 `BaseEntity`를 상속합니다. `created_at`과 `updated_at`은 JPA Auditing으로 기록됩니다.

`GET /api/health`를 실행할 수 있습니다. 설문 문항은 `GET /api/surveys/current`, 내 설문 응답은 `GET`·`PUT /api/me/survey`로 조회·저장합니다(로그인 필요). 설문 차수가 올라가면 `SurveyRevisionChangedEvent(userId, revision)`를 발행합니다. `POST /api/auth/login`은 Google 로그인 구현을 연결할 공개 경로로 설정했습니다. Google ID 토큰 검증, PostgreSQL 저장 및 실제 매칭 점수 계산은 후속 기능에서 구현합니다.

## 채희님 연동용 내부 메서드

B 모듈은 `kr.bokjakbokjak.matching`의 인터페이스를 생성자로 주입받습니다.

| 인터페이스 | 메서드 | 반환 |
| --- | --- | --- |
| `UserQueryService` | `findCandidates(Long viewerId)` | `List<CandidateDto>` |
| `UserQueryService` | `getUserSummaries(List<Long> userIds)` | `List<UserSummaryDto>` |
| `MatchingEngine` | `calculate(Long viewerId, Long targetId)` | `MatchResultDto` |

기본 프로필에서는 `MatchingEngine`으로 실제 설문 답변을 계산하는 `SurveyMatchingEngine`이 등록됩니다. `UserQueryService` 실구현은 후보 조회 작업에서 추가할 예정입니다. 샘플 구현이 필요하면 `SPRING_PROFILES_ACTIVE=matching-fixture ./gradlew bootRun`으로 실행합니다.

### 매칭 점수 계산

- 주제마다 상대의 실제 습관이 내 허용범위 안이면 1점, 벗어나면 한 칸마다 0.25점씩 깎습니다(1 → 0.75 → 0.5 → 0.25 → 0). 주제별 구간은 `TopicRules`에 있습니다.
- "잘 모르겠다", "일정하지 않다"는 0.5점, "조용하기만 하면 상관없다"처럼 조건부 허용은 1점으로 계산하고 라벨을 `NEED_CHECK`로 둡니다. 흡연은 조합표로 판정합니다.
- 카테고리 가중치는 모두 기본 10점에 선택 순위 보너스(1~5순위 +12 / +6 / +2 / +1 / +0.5)를 더합니다. 한 카테고리에 주제가 여러 개면 가중치를 똑같이 나눕니다.
- `viewerToTarget` = Σ(viewer 주제 가중치 × target 습관 점수) ÷ Σ(viewer 주제 가중치). 반대 방향도 같은 방식입니다.
- `mutualScore`는 두 방향의 조화평균입니다. 둘 다 0이면 0입니다.
- 주제 라벨은 한쪽이라도 허용범위를 벗어나면 `NEED_ADJUST`, 조건부·모름이 섞이면 `NEED_CHECK`, 나머지는 `GOOD`입니다.

- 활성 사용자 `7`과 `12`는 서로 후보로 조회됩니다. 최종 매칭 완료 여부에 따른 추천 제외는 B에서 처리합니다.
- `getUserSummaries`는 탈퇴 샘플 `99`도 `WITHDRAWN` 상태와 함께 반환합니다. 존재하지 않는 ID는 생략합니다.
- `calculate`는 샘플 쌍 `7 ↔ 12`를 지원합니다. `calculate(7L, 12L)`는 두 방향 `0.85 / 0.79`와 상호 점수 `0.82`를 반환합니다. 이 값은 연동용 고정 예시이며 실제 매칭 산식의 결과가 아닙니다.
- `viewerToTarget`는 **viewer의 허용 기준과 가중치**로 본 상대 점수입니다. 점수는 0~1이고, 화면 퍼센트가 필요하면 100을 곱합니다.
