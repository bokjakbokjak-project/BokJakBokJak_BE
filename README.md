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
- `GOOGLE_CLIENT_ID`는 Google 로그인에 쓰는 OAuth 웹 클라이언트 ID입니다. ID 토큰의 대상(`aud`)이 이 값과 같아야 로그인됩니다. 비어 있으면 Google 로그인은 모두 실패합니다.
- `JWT_REFRESH_TTL`은 리프레시 토큰 유효 기간이며 기본값은 `14d`입니다. 리프레시 토큰은 Redis에 저장합니다.
- `DEV_LOGIN_KEY`는 테스트 로그인 키입니다. 비어 있으면 테스트 로그인 API가 꺼집니다(404).
- `MATCHING_CANDIDATE_SCOPE`는 추천 후보 범위입니다. 기본값 `ROOM_TYPE`은 같은 기숙사·같은 방 타입만, `DORMITORY`는 같은 기숙사면 방 타입이 달라도 후보로 봅니다.
- `AZURE_OPENAI_ENDPOINT`, `AZURE_OPENAI_API_KEY`, `AZURE_OPENAI_DEPLOYMENT`, `AZURE_OPENAI_API_VERSION`은 AI 요약·분석·안건 생성에 쓰는 Azure OpenAI 설정입니다. 하나라도 비어 있으면 AI를 호출하지 않고 설문 답변 기반의 기본 문구로 동작하므로, 로컬 실행과 테스트에는 없어도 됩니다.
- `.env.example`은 변수 이름과 예시값만 담습니다. 실제 비밀번호와 JWT 키가 들어간 `.env`는 Git에서 제외됩니다.
- 앱이 뜰 때 Flyway가 `src/main/resources/db/migration`의 SQL을 먼저 적용하고, 그다음 JPA `ddl-auto: update`가 마이그레이션에 없는 테이블을 만듭니다.
  - 회원·설문·온보딩·기숙사 테이블은 Flyway로 관리합니다. 스키마를 바꿀 때는 기존 파일을 고치지 말고 `V{다음번호}__설명.sql`을 새로 추가합니다.
  - 추천·채팅·조율 테이블은 엔티티 기준으로 `update`가 만듭니다. 같은 테이블을 Flyway와 엔티티 양쪽에서 만들지 않습니다.
- 일반 `jar`는 비활성화하고 실행용 `bootJar`만 만듭니다.

## 공통 HTTP 계약

성공 응답은 `success, code, message, data`, 실패 응답은 `success, code, message, errors`를 사용합니다. 기본 성공 코드는 `COMMON2000`입니다. `ErrorCode`가 공통 오류 코드·메시지·HTTP 상태를 관리하고, `ApiExceptionHandler`가 예외를 같은 형식으로 반환합니다. HTTP 응답 필드명은 snake_case입니다. 보호된 경로는 서비스가 발급한 `Authorization: Bearer <access_token>`을 받습니다.

JPA 엔티티가 생성·수정 시각이 필요하면 `BaseEntity`를 상속합니다. `created_at`과 `updated_at`은 JPA Auditing으로 기록됩니다.

`GET /api/health`를 실행할 수 있습니다. 설문 문항은 `GET /api/surveys/current`, 내 설문 응답은 `GET`·`PUT /api/me/survey`로 조회·저장합니다(로그인 필요). 설문 차수가 올라가면 `SurveyRevisionChangedEvent(userId, revision)`를 발행합니다. `POST /api/auth/login`은 Google 로그인 구현을 연결할 공개 경로로 설정했습니다. Google ID 토큰 검증, PostgreSQL 저장 및 실제 매칭 점수 계산은 후속 기능에서 구현합니다.

## 로그인과 토큰

- `POST /api/auth/login`: FE가 Google Identity Services에서 받은 ID 토큰(`id_token`)을 보내면 서명·발급자·대상(`GOOGLE_CLIENT_ID`)·만료를 검증하고 서비스 토큰을 발급합니다. 처음 로그인하면 회원을 만듭니다.
  - 학교 Google Workspace 계정만 받습니다. ID 토큰의 `hd`가 있어야 하고, `hd`나 이메일 도메인 중 하나가 `university_domain`에 등록된 학교여야 합니다. 학교 추가는 SQL로 도메인만 넣으면 됩니다.
  - 응답의 `next_step`은 온보딩 전 `ONBOARDING`, 설문 전 `SURVEY`, 둘 다 마치면 `HOME`입니다.
- 액세스 토큰은 30분, 리프레시 토큰은 14일입니다. `POST /api/auth/refresh`로 두 토큰을 모두 새로 받고, 쓴 리프레시 토큰은 바로 폐기됩니다.
- `POST /api/auth/logout`은 현재 액세스 토큰을 남은 시간 동안 차단하고 리프레시 토큰을 지웁니다.
- `POST /api/auth/dev-login`은 개발·시연용입니다. `X-Dev-Login-Key` 헤더가 `DEV_LOGIN_KEY`와 같고 대상이 테스트 유저일 때만 토큰을 줍니다. 아래 테스트 유저 표의 회원 ID로 로그인합니다.
- 테스트에서는 Redis 대신 메모리 저장소(`InMemoryAuthTokenStore`)를 씁니다.

## 채희님 연동용 내부 메서드

B 모듈은 `kr.bokjakbokjak.matching`의 인터페이스를 생성자로 주입받습니다.

| 인터페이스 | 메서드 | 반환 |
| --- | --- | --- |
| `UserQueryService` | `findCandidates(Long viewerId)` | `List<CandidateDto>` |
| `UserQueryService` | `getUserSummaries(List<Long> userIds)` | `List<UserSummaryDto>` |
| `MatchingEngine` | `calculate(Long viewerId, Long targetId)` | `MatchResultDto` |
| `SurveyProfileQueryService` | `getSurveyProfile(Long userId)` | `SurveyProfileDto` |

기본 프로필에서는 `MatchingEngine`으로 실제 설문 답변을 계산하는 `SurveyMatchingEngine`이 등록됩니다. `UserQueryService`는 실제 회원 데이터로 조회하는 `UserQueryServiceImpl`이 등록됩니다. 샘플 구현이 필요하면 `SPRING_PROFILES_ACTIVE=matching-fixture ./gradlew bootRun`으로 실행합니다.

### 추천 후보와 사용자 요약

- `findCandidates`는 조회한 사람과 같은 학교·같은 성별·같은 기숙사 방 타입이고, 온보딩과 설문을 마친 활성 회원을 돌려줍니다. 본인은 빠집니다. 최종 매칭 완료자 제외와 정렬은 B에서 처리합니다.
- 조회한 사람이 비활성이거나 기숙사 정보·설문이 비어 있으면 빈 목록입니다. 없는 사용자는 `IllegalArgumentException`을 던집니다.
- `getUserSummaries`는 요청한 순서대로 중복 없이 돌려줍니다. 탈퇴 회원도 `WITHDRAWN` 상태로 포함하고, 없는 ID는 생략합니다.
- `topCategories`는 1~3순위 카테고리 코드입니다. `entryYear`는 학번 앞 4자리입니다.
- `birthYear`로 나이를 보여줄 때는 한국식 나이(올해 − 출생년도 + 1)를 씁니다. 2026년에 2007년생은 20세입니다.
- 회원 상태는 `ACTIVE`, `WITHDRAWN`(탈퇴), `SUSPENDED`(이용 제한)입니다. 탈퇴해도 행은 지우지 않고 상태만 바꿉니다.

### 테스트 유저

Flyway(`V6__insert_test_personas.sql`)로 시연·연동용 페르소나 13명을 넣습니다. 모두 동국대 회원이고 온보딩과 설문을 마친 상태이며 `test_user = TRUE`로 표시됩니다. ID는 환경마다 다를 수 있으니 닉네임으로 조회합니다.

| 닉네임 | 확인용 |
| --- | --- |
| 책갈피 | 시연 기준 계정. 흡연자와 같은 방 불가 |
| 깔끔이 | 청결 기준이 엄격함 |
| 털털이 | 허용 범위는 넓지만 습관이 느슨함 |
| 아침햇살 / 새벽감성 | 아침형과 올빼미, 수면 영역 충돌 |
| 연기한줄 | 실외 흡연자. 후보에는 나오고 점수만 낮아짐 |
| 드르렁 | 잠버릇이 잦음 |
| 무난이 | 대부분과 점수가 높음 |
| 갸우뚱 | 잘 모르겠다·불규칙 응답 (확인 필요 라벨) |
| 금연중 | 기숙사에서는 금연 예정 |
| 민트초코 | 남성. 여성 회원의 후보에 나오면 안 됨 |
| 옆동주민 | 다른 기숙사(고양학사). 책갈피의 후보에 나오면 안 됨 |
| 옆방타입 | 같은 기숙사·다른 방 타입. 책갈피의 후보에 나오면 안 됨 |

### AI 요약용 설문 프로필

- `getSurveyProfile`은 설문을 완료한 사용자의 최신 설문 내용을 돌려줍니다. 후보 조회 실구현과 관계없이 기본 프로필에서 바로 사용할 수 있습니다.
- `topCategories`는 선택한 카테고리 중 1~3순위, `topics`는 주제 8개를 설문 순서대로 담습니다.
- 주제마다 상황문구, 내 습관 질문·답변, 허용범위 질문·답변을 담습니다. 답변은 선택지 코드가 아니라 문구라서 프롬프트에 그대로 넣을 수 있습니다.
- `surveyRevision`이 바뀌었을 때만 요약을 다시 만들면 됩니다. 설문이 바뀌면 `SurveyRevisionChangedEvent`도 발행됩니다.
- 없는 사용자나 설문을 완료하지 않은 사용자는 `IllegalArgumentException`을 던집니다.

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
