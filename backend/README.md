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

### Windows (PowerShell)

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

### macOS (zsh / bash)

```sh
./gradlew bootRun --args="--spring.profiles.active=local"
```

macOS에서 실행 권한 오류가 발생하면 `chmod +x gradlew`를 먼저 실행합니다. 환경변수 로딩은 [루트 README의 운영체제별 안내](../README.md#4-현재-터미널에-환경변수-로딩)를 참고합니다.

운영은 `SPRING_PROFILES_ACTIVE=prod`를 설정하거나 JAR 실행 시 `--spring.profiles.active=prod`를 전달합니다.

Swagger에는 현재 Spring Security 기본 인증이 적용됩니다.

## 테스트

Docker 실행이 필요합니다. 팀 개발 DB와 같은 `mysql:8.4.11`을 사용하며, `TEST_MYSQL_IMAGE`로 변경할 수 있습니다.

### Windows (PowerShell)

```powershell
.\gradlew.bat test
```

### macOS (zsh / bash)

```sh
./gradlew test
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

## AI 사진 판정 연동

`PhotoVerificationService.verify(request)`는 사진의 S3 객체 키와 도전 인증 조건을 AI 서버의 `POST /internal/photo-verifications`로 전달하고, 조건별 판정을 종합해 최종 인증 결과를 반환합니다.

현재 AI 호출·요청 및 응답 검증·최종 결과 판정까지 구현되어 있습니다. 사진 업로드 흐름 연결, AI 작업 및 판정 결과의 DB 저장, 기술 오류 횟수 복구, 자동 재시도, 서버 간 인증은 아직 구현되지 않았습니다. 인증 시도 저장은 아래 createVerification 메서드로 별도 제공합니다.

### 연동 설정

`application.yaml` 기준 설정은 다음과 같습니다.

| 환경변수 | 미설정 시 기본값 | 용도 |
| --- | --- | --- |
| `AI_SERVER_URL` | `http://localhost:8000` | AI 서버 기본 주소 |
| `AI_CONNECT_TIMEOUT` | `3s` | 서버 연결 대기 제한 시간 |
| `AI_READ_TIMEOUT` | `30s` | 서버 응답 대기 제한 시간 |

`AI_SERVER_URL`에는 백엔드에서 접속 가능한 HTTP(S) 기본 주소를 설정합니다. `/internal/photo-verifications` 경로는 코드에서 붙입니다.
환경변수로 지정한 값이 기본값보다 우선합니다. `.env.example`의 `AI_CONNECT_TIMEOUT=5s`를 실행 환경에 적용하면 연결 타임아웃은 `5s`가 됩니다.

### 호출 및 결과 처리

`PhotoVerificationRequest`에는 문자열 ID(`aiTaskId`, `verificationId`), S3 객체 키(`imageKey`), 조건 목록(`conditions`: `code`, `description`)을 전달합니다. AI 서버로는 `ai_task_id`, `verification_id`, `image_key`, `conditions` 필드의 JSON을 전송하며, 사진 파일 자체는 전송하지 않습니다.

`PhotoVerificationClient`는 요청의 필수 값과 조건 코드 중복을 검사합니다. 응답은 HTTP `200`, `processing_status=SUCCEEDED`일 때만 판정에 사용하며, 필수 값·판정값·글자 수 제한, 요청과 응답의 ID 일치, 조건별 결과의 누락·중복·추가 여부를 검증합니다.

검증이 완료되면 `PhotoVerificationService`가 다음 규칙으로 최종 결과를 결정합니다.

| 조건별 판정 | 최종 인증 결과 |
| --- | --- |
| `NOT_MET`이 하나라도 있음 | `FAILED` |
| `NOT_MET`이 없고 `UNCERTAIN`이 있음 | `REVIEW_REQUIRED` |
| 모든 조건이 `MET` | `SUCCESS` |

반환값 `Result`의 `status`에는 최종 인증 결과, `response`에는 검증된 AI 응답이 담깁니다. 통신 오류·타임아웃, AI 오류 응답, 요청·응답 검증 실패는 `PhotoVerificationTechnicalException`으로 전달하며, 이때 최종 인증 결과는 반환하지 않습니다.

### 테스트

`backend/`에서 아래 테스트만 실행하면 실제 AI 서버, DB, Docker 없이 요청·응답 검증, 판정 규칙, 오류·타임아웃 처리를 확인할 수 있습니다. `MockRestServiceServer`로 AI 응답을 모의하므로 실제 AI 서버와의 통신이나 사진 판정 품질은 검증하지 않습니다.

Windows (PowerShell):

```powershell
.\gradlew.bat test --tests 'com.moamu.domain.verification.PhotoVerificationServiceTests'
```

macOS (zsh / bash):

```sh
./gradlew test --tests 'com.moamu.domain.verification.PhotoVerificationServiceTests'
```

## 미정 사항

- JWT·AI 서버 간 인증 방식
- Redis 개발 환경 및 인증 설정
- `LocalDateTime` 감사 시각의 시간대 기준
