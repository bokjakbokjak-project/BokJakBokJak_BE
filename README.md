# BokJakBokJak_BE
복작복작 - 대학생 기숙사 룸메이트 매칭 서비스 Backend

## 로컬 실행

Java 21과 저장소의 Gradle Wrapper를 사용합니다. 초기 설정은 Spring Boot 4.1.1과 Gradle 9.7.1입니다.

```bash
./gradlew test
export JWT_SECRET_BASE64="$(openssl rand -base64 32)"
./gradlew bootRun
```

다른 터미널에서 `curl -i http://localhost:8080/api/health`로 확인합니다. Windows에서는 `gradlew.bat`를 사용합니다.

- `JWT_SECRET_BASE64`에는 Base64로 인코딩한 32바이트 이상의 비밀 키를 넣습니다.
- `CORS_ALLOWED_ORIGINS`는 허용할 FE 주소를 쉼표로 구분합니다. 로컬 기본값은 `http://localhost:5173`입니다.
- `SERVER_PORT`의 기본값은 `8080`, `JWT_ACCESS_TTL`의 기본값은 `30m`입니다.
- 비밀값은 환경변수로 주입합니다. 로컬 실행에 DB는 필요하지 않습니다.

## 공통 HTTP 계약

성공 응답은 `success, code, message, data`, 실패 응답은 `success, code, message, errors`를 사용합니다. HTTP 응답 필드명은 snake_case입니다. 보호된 경로는 서비스가 발급한 `Authorization: Bearer <access_token>`을 받습니다.

`GET /api/health`를 실행할 수 있습니다. `POST /api/auth/login`은 Google 로그인 구현을 연결할 공개 경로로 설정했습니다. Google ID 토큰 검증, PostgreSQL 저장 및 실제 매칭 점수 계산은 후속 기능에서 구현합니다.

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
- `calculate`는 샘플 쌍 `7 ↔ 12`를 지원합니다. `calculate(7L, 12L)`는 두 방향 `0.85 / 0.79`와 상호 점수 `0.82`를 반환합니다. 메서드 약속의 고정 예시입니다.
- `viewerToTarget`는 **viewer의 허용 기준과 가중치**로 본 상대 점수입니다. 화면 퍼센트가 필요하면 B에서 100을 곱합니다. 실제 가중치와 합산식은 엔진 구현에서 확정합니다.
