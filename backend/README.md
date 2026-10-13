# MOAMU Backend

Java 21 · Spring Boot 3.5.16 · Gradle

처음 내려받은 팀원은 [루트 README의 로컬 환경 세팅](../README.md#팀원-로컬-환경-세팅)을 먼저 따라갑니다. Docker MySQL 생성, 개인 `.env` 로딩, 실행과 테스트 명령을 안내합니다.

## Profile

공통 설정은 `application.yaml`에서 관리하고, Profile은 실행 시 지정합니다.

| Profile | 용도 | Swagger |
| --- | --- | --- |
| local | 개발자 PC의 MySQL·Redis 연결 | UI와 API 문서 활성화 |
| test | MySQL Testcontainers를 사용하는 테스트 | 비활성화 |
| prod | 운영 MySQL·Redis 연결 | 비활성화 |

## 환경변수

`.env.example`을 참고해 실행 환경에 변수를 설정합니다.

| 환경 | 필수 변수 |
| --- | --- |
| local | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` |
| prod | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT` |
| test | `TEST_MYSQL_IMAGE` (선택, 기본값 `mysql:8.4.11`) |

local의 `REDIS_HOST`, `REDIS_PORT`는 선택 사항이며, 생략하면 `127.0.0.1:6379`를 사용합니다. Redis가 준비되지 않았다면 IDE나 터미널에 두 환경 변수를 등록하지 마세요. 빈 문자열로 등록하면 기본값이 적용되지 않습니다. 현재는 시작 시 Redis를 호출하는 코드가 없어 MySQL 연결 확인이 가능하지만, Redis 기능 사용 및 Redis 상태 점검에는 실제 서버가 필요합니다.

`DB_URL`은 MySQL JDBC URL, `REDIS_PORT`는 숫자입니다. `.env`는 자동으로 로딩되지 않습니다. 비밀번호와 API Key는 Git에 올리지 않습니다.

개발 DB의 기본 접속 주소는 `127.0.0.1:3306/moamu`, 계정은 `moamu`입니다. 개인 Docker 포트 매핑에 맞게 `.env`의 `DB_URL`을 설정합니다. 예를 들어 호스트 포트가 3307이면 `jdbc:mysql://127.0.0.1:3307/moamu`를 사용합니다. 비밀번호는 개인 로컬 `.env`에서 관리합니다.

## 실행

개인 `.env`의 DB 및 Redis 접속 값을 IDE의 환경 변수 설정이나 터미널을 통해 실행 프로세스에 전달한 뒤 `backend/`에서 실행합니다. Spring Boot는 `.env`를 자동으로 읽지 않습니다.

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

운영은 `SPRING_PROFILES_ACTIVE=prod`를 설정하거나 JAR 실행 시 `--spring.profiles.active=prod`를 전달합니다.

Swagger에는 현재 Spring Security 기본 인증이 적용됩니다.

## 테스트

Docker 실행이 필요합니다. 팀 개발 DB와 같은 `mysql:8.4.11`을 사용하며, `TEST_MYSQL_IMAGE`로 변경할 수 있습니다.

```powershell
.\gradlew.bat test
```

테스트는 `test` Profile과 MySQL Testcontainers를 사용합니다. DB 연결 정보는 컨테이너에서 공급합니다.

Redis 통합 테스트용 연결은 추후 추가합니다.

## API 응답

성공은 `ApiResponse.success(data)`, 도메인 오류는 `BusinessException`과 도메인별 `ErrorCode`를 사용합니다.

```json
{"success": true, "data": {"challengeId": 1}}
```

```json
{"success": false, "error": {"code": "INVALID_REQUEST", "message": "요청이 올바르지 않습니다."}}
```

요청 오류는 `INVALID_REQUEST`, 서버 오류는 `INTERNAL_SERVER_ERROR`로 응답합니다. Spring MVC의 HTTP 상태와 헤더는 유지합니다. 인증·인가 필터의 실패 응답은 JWT 구현 시 같은 형식으로 연결합니다.

## DB 마이그레이션

Flyway SQL은 `src/main/resources/db/migration/`에 추가합니다. 테이블 생성·변경은 Flyway가 담당하고 JPA는 `ddl-auto: validate`로 구조를 검증합니다.

로컬 Docker MySQL 연결은 준비되어 있으며, 도메인 테이블은 ERD 확정 후 구현합니다. 현재 도메인 테이블이 없어 baseline 설정은 추가하지 않았습니다. 도메인 엔티티를 추가할 때 해당 테이블의 마이그레이션 SQL도 함께 작성합니다.

## 미정 사항

- JWT·AI 연동 설정
- Redis 개발 환경 및 인증 설정
- `LocalDateTime` 감사 시각의 시간대 기준
