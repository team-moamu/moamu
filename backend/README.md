# MOAMU Backend

Java 21 · Spring Boot 3.5.16 · Gradle

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
| local / prod | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT` |
| test | `TEST_MYSQL_IMAGE` (MySQL 이미지명과 태그) |

`DB_URL`은 MySQL JDBC URL, `REDIS_PORT`는 숫자입니다. `.env`는 자동으로 로딩되지 않습니다. 비밀번호와 API Key는 Git에 올리지 않습니다.

## 실행

환경변수를 설정한 뒤 `backend/`에서 실행합니다.

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

운영은 `SPRING_PROFILES_ACTIVE=prod`를 설정하거나 JAR 실행 시 `--spring.profiles.active=prod`를 전달합니다.

Swagger에는 현재 Spring Security 기본 인증이 적용됩니다.

## 테스트

Docker 실행과 `TEST_MYSQL_IMAGE` 설정이 필요합니다. MySQL 버전은 운영 환경과 맞춥니다.

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

## 미정 사항

- `ddl-auto`, Flyway baseline 및 초기 마이그레이션 정책
- JWT·AI 연동 설정

Flyway 자동 설정이 활성화되어 있으므로 기존 DB에 연결하기 전에 초기 적용 정책을 확인합니다.
