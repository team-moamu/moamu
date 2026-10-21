# MOAMU (모아뮤)

목표에 도전하고 사진으로 수행을 인증하면, 인증 결과에 따라 작품의 퍼즐 조각을 공개하는 서비스입니다. 완료한 도전의 작품은 개인 미술관에서 확인할 수 있도록 기획하고 있습니다.

## 현재 상태와 역할

- 백엔드 공통 설정을 구성했고, 로컬 Docker MySQL 연결을 확인했습니다.
- DB 서버는 준비되어 있습니다. 도메인 테이블과 엔티티는 ERD 확정 후 구현합니다.
- Redis는 인프라 준비 후 연결합니다. 현재 로컬 서버 시작에는 Redis 서버가 필요하지 않습니다.
- AI 서버는 별도 저장소(`moamu-ai`)에서 관리할 예정이며 연동은 추후 진행합니다.
- 각 기능 담당 백엔드 개발자가 해당 기능의 프론트엔드도 함께 개발합니다. 프론트엔드 프로젝트는 초기화 전입니다.
- JWT 인증과 CI/CD는 추후 구현합니다. 현재 Spring Security 기본 인증을 사용합니다.

## 프로젝트 구성

```text
moamu/
├── backend/
│   ├── .env.example                 # 개인 환경변수 템플릿
│   ├── gradle/wrapper/               # Gradle Wrapper
│   ├── src/main/java/com/moamu/
│   │   ├── BackendApplication.java
│   │   ├── domain/                   # 기능별 도메인 패키지
│   │   │   ├── user/                 # 사용자 도메인
│   │   │   │   ├── controller/       # 사용자 API 컨트롤러
│   │   │   │   ├── dto/              # 회원가입 요청 DTO
│   │   │   │   ├── entity/           # 사용자 엔티티
│   │   │   │   ├── enums/            # 사용자 상태
│   │   │   │   ├── exception/        # 사용자 도메인 오류 코드
│   │   │   │   ├── repository/       # JPA·QueryDSL 저장소
│   │   │   │   └── service/          # 서비스 인터페이스·구현체
│   │   │   ├── ai/                   # 이하 도메인은 빈 패키지 (구현 전)
│   │   │   ├── artwork/
│   │   │   ├── challenge/
│   │   │   ├── goal/
│   │   │   ├── media/
│   │   │   ├── museum/
│   │   │   ├── puzzle/
│   │   │   ├── schedule/
│   │   │   └── verification/
│   │   └── global/                   # 공통 코드
│   │       ├── config/               # JPA Auditing·QueryDSL 설정
│   │       ├── entity/               # 공통 BaseEntity
│   │       ├── exception/            # 공통 오류 코드·예외 처리
│   │       ├── response/             # 공통 API 응답
│   │       └── security/             # 보안 패키지 (구현 전)
│   ├── src/main/resources/
│   │   ├── application.yaml          # 공통 JPA·Flyway 설정
│   │   ├── application-local.yaml    # 로컬 연결·Swagger 설정
│   │   ├── application-prod.yaml     # 운영 연결 설정
│   │   └── db/migration/             # Flyway 마이그레이션
│   │       └── README.md             # SQL 작성 규칙 (도메인 SQL 작성 전)
│   ├── src/test/
│   │   ├── java/com/moamu/           # MySQL 컨텍스트·공통 엔티티·예외 처리 테스트
│   │   └── resources/
│   │       └── application-test.yaml # 테스트 프로필 설정
│   ├── build.gradle
│   ├── settings.gradle
│   ├── gradlew                       # macOS/Linux용 Gradle 실행 스크립트
│   ├── gradlew.bat                   # Windows용 Gradle 실행 스크립트
│   └── README.md                     # 백엔드 구현 규칙
├── frontend/                        # 초기화 전
└── README.md
```

위 구조는 현재 소스와 구현 예정인 빈 패키지를 함께 표시합니다. 빈 디렉토리는 Git에 저장되지 않으므로 Clone 후 `frontend/`, 구현 전 도메인 패키지, `global/security/`가 보이지 않아도 정상입니다. 구현할 때 필요한 디렉토리를 생성합니다. 개인 환경 파일과 IDE 설정, 빌드 산출물은 구조에서 생략했습니다.

| 구분 | 구성 |
| --- | --- |
| 언어·프레임워크 | Java 21, Spring Boot 3.5.16 |
| 빌드 | Gradle 8.14.6 (Wrapper 포함) |
| 웹·보안 | Spring Web, Spring Security, Validation |
| 데이터 접근 | Spring Data JPA, QueryDSL (Jakarta) |
| DB·마이그레이션 | MySQL 8.4.11, Flyway |
| API 문서 | springdoc OpenAPI / Swagger UI |
| 기타 | Lombok, Actuator, Spring Data Redis, JJWT |
| 테스트 | JUnit 5, Spring Boot Test, Spring Security Test, MySQL Testcontainers |

Redis·JJWT 의존성이 포함되어 있어도 실제 연결과 JWT 인증 구현이 완료된 것은 아닙니다.

## 팀원 로컬 환경 세팅

아래 명령은 **Windows PowerShell** 기준입니다. 순서대로 실행합니다.

### 1. 필수 프로그램

- JDK 21: `JAVA_HOME`을 JDK 설치 폴더로 지정하고 Java를 `PATH`에 등록합니다.
- Git
- Docker Desktop: Linux 컨테이너를 사용할 수 있도록 실행합니다.
- IntelliJ IDEA 또는 VS Code (Java 개발 확장 설치)

Gradle과 MySQL은 별도 설치하지 않습니다. Gradle은 Wrapper, MySQL은 Docker를 사용합니다.

```powershell
java -version
git --version
docker version
```

Java 버전이 21인지, Docker의 Client와 Server 정보가 모두 표시되는지 확인합니다. IDE의 프로젝트 SDK와 Gradle JVM도 Java 21로 맞춥니다.

### 2. 저장소 내려받기

GitHub 저장소 화면의 **Code → HTTPS** 주소를 복사해 아래 자리표시자를 바꿉니다.

```powershell
git clone <GitHub에서 복사한 저장소 주소> moamu
cd moamu/backend
.\gradlew.bat --version
```

이후 명령은 모두 `backend/`에서 실행합니다. 최초 Gradle·의존성 다운로드에는 인터넷 연결이 필요합니다.

### 3. 개인 환경변수 파일 생성

```powershell
Copy-Item .env.example .env
notepad .env
```

이미 `.env`가 있다면 복사하지 말고 기존 파일을 수정합니다. 아래 비밀번호 자리표시자를 **본인이 정한 로컬 개발용 비밀번호**로 바꿉니다.

```dotenv
DB_URL=jdbc:mysql://127.0.0.1:3306/moamu
DB_USERNAME=
DB_PASSWORD=개인_로컬_DB_비밀번호
REDIS_HOST=
REDIS_PORT=
JWT_SECRET=
AI_SERVER_URL=
TEST_MYSQL_IMAGE=mysql:8.4.11
```

`.env`는 Git에서 제외되며 팀원마다 따로 관리합니다. Spring Boot는 `.env`를 자동으로 읽지 않습니다.

### 4. 현재 터미널에 환경변수 로딩

아래 코드는 주석과 빈 값을 건너뛰고 현재 PowerShell 세션에 값을 등록합니다. `.env` 값은 따옴표 없이 작성합니다. 이 로더는 단순 `KEY=VALUE` 형식용이며 변수 치환과 여러 줄 값은 지원하지 않습니다.

```powershell
Get-Content .env -Encoding UTF8 | ForEach-Object {
    if ($_ -match '^\s*([^#=\s]+)\s*=(.*)$') {
        $key = $matches[1]
        $value = $matches[2].Trim()
        if (-not [string]::IsNullOrWhiteSpace($value)) {
            [Environment]::SetEnvironmentVariable($key, $value, 'Process')
        }
    }
}

# Redis 준비 전에는 두 변수를 실행 프로세스에 전달하지 않습니다.
Remove-Item Env:REDIS_HOST, Env:REDIS_PORT -ErrorAction SilentlyContinue
```

환경변수는 현재 터미널과 여기서 실행한 프로그램에만 적용됩니다. 새 터미널을 열거나 `.env`를 수정하면 다시 로딩합니다. Redis가 준비된 뒤에는 위 `Remove-Item`을 생략하고 실제 접속 정보를 등록합니다. 빈 Redis 값을 주입하면 기본값이 적용되지 않을 수 있습니다.

### 5. 개인 Docker MySQL 준비

`/infra/README.md` 참고

### 6. 백엔드 실행

환경변수를 로딩한 터미널에서 실행합니다.

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

`Started BackendApplication` 로그가 나타나면 시작된 것입니다. 기본 포트는 8080이며 `Ctrl+C`로 종료합니다.

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

현재 기본 인증 계정은 `user`이고, 비밀번호는 시작 로그의 `Using generated security password`에 표시됩니다. 서버 재시작 시 바뀝니다. 로그인 화면이나 401 응답은 기본 보안 설정에 따른 동작입니다. 아직 비즈니스 API가 없어 `/`는 API 확인 경로로 사용하지 않습니다.

IDE에서 직접 실행한다면 Java 21을 선택하고, 실행 구성에 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SPRING_PROFILES_ACTIVE=local`을 등록합니다. Redis 변수는 준비 전까지 생략합니다. IDE의 `.env` 로딩 기능을 사용한다면 빈 Redis 값이 주입되지 않도록 확인합니다.

#### VS Code에서 실행

저장소 루트 `moamu/`를 VS Code로 열고 권장 확장인 **Extension Pack for Java**를 설치합니다. Java 실행 환경은 각자의 PC에서 JDK 21로 설정합니다.

1. `backend/.env.example`을 `backend/.env`로 복사하고 본인 MySQL의 포트·계정·비밀번호를 입력합니다. 이미 파일이 있다면 기존 파일을 수정합니다.
2. Redis가 준비되지 않았다면 `.env`의 `REDIS_HOST`, `REDIS_PORT` 항목은 삭제하거나 `#`으로 주석 처리합니다. 빈 값으로 전달하지 않습니다.
3. Docker MySQL을 실행한 뒤 **실행 및 디버그**에서 **MOAMU Backend (local)**을 선택하고 `F5`를 누릅니다. 디버깅 없이 실행하려면 `Ctrl+F5`를 누릅니다.

공유된 `.vscode/launch.json`이 `local` 프로필을 지정하고 `backend/.env`를 읽어 서버에 전달하므로 PowerShell 환경변수 로딩은 필요하지 않습니다. 이 설정은 VS Code 실행 구성에만 적용되며, 터미널에서 `bootRun`을 직접 실행할 때는 위 환경변수 로딩 과정이 필요합니다. 서버 시작 시 Flyway가 연결한 DB에 미적용 마이그레이션을 적용합니다.

Git에는 `.vscode/launch.json`, `.vscode/extensions.json`, `.env.example`을 공유합니다. 개인 `.env`와 JDK 설치 경로 등 개인 VS Code 설정은 공유하지 않습니다.

### 7. 빌드와 테스트

```powershell
.\gradlew.bat clean build
```

Docker가 실행 중이어야 합니다. 테스트는 `test` 프로필과 MySQL Testcontainers로 별도 임시 DB를 만들며, 개인 개발 DB 접속 정보를 사용하지 않습니다. 처음 실행하면 MySQL 이미지 다운로드로 시간이 걸릴 수 있습니다.

```powershell
# 테스트만 실행
.\gradlew.bat test

# Docker 없이 컴파일·패키징만 확인 (테스트 검증 제외)
.\gradlew.bat build -x test
```

테스트 보고서: `backend/build/reports/tests/test/index.html`

macOS/Linux에서는 `.\gradlew.bat` 대신 `./gradlew`를 사용합니다. 실행 권한이 없다면 `chmod +x gradlew`를 실행합니다. 환경변수는 IDE 실행 구성 또는 각 셸의 `export`로 등록합니다. 위 PowerShell 환경변수 로더는 해당 셸에서 사용할 수 없습니다.

## 자주 발생하는 문제

| 증상 | 확인할 내용 |
| --- | --- |
| Java / Gradle JVM 버전 오류 | `JAVA_HOME`, Java 버전, IDE SDK·Gradle JVM이 모두 Java 21인지 확인 |
| `DB_URL` 관련 오류 | `.env` 생성만으로는 로딩되지 않음. 같은 터미널에서 환경변수 로더 실행 |
| MySQL 연결 거부 | Docker 실행, 컨테이너 상태, 초기화 완료 여부, 호스트 포트와 DB URL 확인 |
| `Access denied` / DB 없음 | DB 계정·비밀번호·DB 이름과 기존 볼륨의 초기 설정 확인 |
| Redis 포트 변환 오류 | 빈 `REDIS_PORT` 주입 여부 확인 후 Redis 환경변수 제거 |
| 8080 포트 사용 중 | 기존 서버 종료 또는 `--args="--spring.profiles.active=local --server.port=8081"`로 실행 |
| 테스트가 Docker를 찾지 못함 | Docker Desktop 실행 및 Linux 컨테이너 모드 확인 |
| Swagger 로그인 요구 / 401 | 시작 로그의 임시 비밀번호로 기본 인증 사용 |

## 기능 개발과 DB 변경

도메인은 `user`, `goal`, `challenge`, `schedule`, `verification`, `media`, `ai`, `artwork`, `puzzle`, `museum`으로 나눌 예정입니다. 기능별 담당자는 백엔드와 프론트엔드를 함께 구현합니다. 프론트엔드 초기화 방식과 실행 명령은 프로젝트가 구성되면 추가합니다.

ERD 확정 후 엔티티를 추가할 때 `backend/src/main/resources/db/migration/`에 `V1__설명.sql` 형식의 Flyway SQL도 함께 작성합니다. 적용된 SQL은 수정하지 않고 다음 버전을 추가합니다. 테이블 변경은 Flyway로 관리하고 JPA의 `ddl-auto: validate`로 구조를 검증합니다. 현재 도메인 테이블 SQL은 없습니다.

공통 API 응답·예외 처리와 프로필 상세 규칙은 [백엔드 README](backend/README.md)를 참고합니다.

## GitHub 공유

소스 코드, 설정 템플릿, Gradle Wrapper, 마이그레이션과 문서를 공유합니다. 개인 `.env`, IDE 설정, `build/`, `.gradle/`은 Git에서 제외됩니다. 비밀번호와 API Key를 추적되는 YAML·소스·문서에 작성하지 않습니다.

1차 MVP에서는 사전 등록된 목표(GoalTemplate)와 준비된 작품을 사용할 예정입니다. 사진 인증 AI 연동과 작품 조각 공개 기능은 추후 개발합니다.
