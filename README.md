# MOAMU (모아뮤)

MOAMU는 목표에 도전하고 사진으로 수행을 인증하면, 인증 결과에 따라 작품의 퍼즐 조각을 공개하는 서비스입니다. 완료한 도전의 작품은 개인 미술관에서 확인할 수 있도록 기획하고 있습니다.

> 현재 상태: **프로젝트 초기 세팅 단계**  
> 이 저장소에는 **Backend와 Frontend**가 포함되며, **AI 서버는 별도 GitHub 저장소(`moamu-ai`)**에서 관리할 예정입니다.

## 프로젝트 구조

```text
moamu/
├── backend/                       # Java / Spring Boot
│   ├── .editorconfig              # 백엔드 코드 편집 규칙
│   ├── .env.example               # 환경 변수 이름 예시 (실제 비밀값 제외)
│   ├── gradle/wrapper/            # Gradle Wrapper
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/moamu/
│   │   │   │   ├── BackendApplication.java
│   │   │   │   ├── domain/         # 기능별 패키지 (현재 빈 패키지)
│   │   │   │   └── global/         # 공통 설정·응답·예외·보안 (현재 빈 패키지)
│   │   │   └── resources/
│   │   │       └── application.yaml
│   │   └── test/java/com/moamu/
│   │       └── BackendApplicationTests.java
│   ├── build.gradle
│   ├── settings.gradle
│   ├── gradlew
│   └── gradlew.bat
├── frontend/                      # 프론트엔드 (설정 전)
├── .gitattributes
├── .gitignore
└── README.md
```

`backend/.gradle/`, `backend/build/`, `backend/bin/` 등은 로컬에서 생성되는 파일이며 Git 관리 대상에서 제외합니다. Java의 빈 패키지 디렉토리는 Git에 별도로 저장되지 않을 수 있습니다.

### Backend 도메인

`domain/` 아래에 다음 패키지를 준비했습니다. **현재 비즈니스 로직 구현 전**입니다.

| 패키지 | 예정 역할 |
| --- | --- |
| `user` | 회원 및 인증 |
| `goal` | 등록된 목표(GoalTemplate) |
| `challenge` | 도전 생성 및 관리 |
| `schedule` | 인증 회차 및 마감 |
| `verification` | 사진 인증 제출 및 결과 |
| `media` | 사진 업로드 및 파일 관리 |
| `ai` | AI 판정 서버 연동 |
| `artwork` | 작품 연결 |
| `puzzle` | 퍼즐 조각 상태 |
| `museum` | 개인 미술관 조회 |

공통 기능은 `global/config`, `global/security`, `global/exception`, `global/response`에 구성할 예정입니다. 세부 구현 방식은 팀 협의 후 확정합니다.

## 기술 스택

### 현재 적용된 Backend 환경

| 구분 | 기술 |
| --- | --- |
| 언어 | Java 21 |
| 프레임워크 | Spring Boot 3.5.16 |
| 빌드 | Gradle (Groovy DSL, Wrapper 포함) |
| 웹 | Spring Web |
| 보안 | Spring Security |
| 데이터 접근 | Spring Data JPA |
| DB 드라이버 | MySQL Connector/J |
| Redis 연동 | Spring Data Redis |
| 검증 | Spring Validation |
| 모니터링 | Spring Boot Actuator |
| 코드 편의 | Lombok |
| 테스트 | Spring Boot Starter Test, Spring Security Test (JUnit 5 기반) |

### 도입 예정 / 팀 협의 후 설정

- JWT 라이브러리 및 인증 정책
- QueryDSL
- Springdoc OpenAPI (Swagger UI)
- Flyway (도입 결정, 의존성 및 마이그레이션 설정 전)
- Testcontainers
- MySQL·Redis·AI 서버 실제 연결
- 프론트엔드 프레임워크 및 실행 환경
- CI/CD 및 배포 환경 (인프라 담당자와 협의)

## 로컬 개발 환경

- **JDK 21** 설치
- Git 설치
- VS Code 또는 IntelliJ IDEA
- 별도의 Gradle 전역 설치 불필요 (Gradle Wrapper 사용)

프로젝트 생성 및 컴파일 확인에 사용한 환경은 Windows PowerShell입니다. 아래 명령어도 Windows 기준입니다.

### 1. 프로젝트 받기

GitHub에서 저장소를 Clone한 후 `backend`로 이동합니다.

```powershell
cd backend
```

### 2. Java 및 Gradle 확인

```powershell
java -version
.\gradlew.bat --version
```

Java 21을 사용하는지 확인합니다.

### 3. 컴파일

```powershell
.\gradlew.bat clean compileJava
```

### 4. 테스트를 제외한 빌드

```powershell
.\gradlew.bat clean build -x test
```

> 현재는 MySQL 연결 설정이 완료되지 않았으므로 `@SpringBootTest`의 `contextLoads()`가 DataSource 생성 중 실패할 수 있습니다. 위 명령어의 성공은 **컴파일·패키징 성공**을 의미하며, 전체 테스트나 DB 연동 성공을 의미하지 않습니다.

### 5. Spring Boot 실행

DB 인프라가 준비되고 연결 설정이 끝나면 다음 명령어로 실행합니다.

```powershell
.\gradlew.bat bootRun
```

현재 **DB가 없는 초기 프로젝트를 일시적으로 실행 확인**하려면, JPA를 사용하는 애플리케이션 코드가 없다는 전제에서 아래와 같이 실행할 수 있습니다.

```powershell
.\gradlew.bat bootRun --args="--spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
```

`Started BackendApplication` 로그가 나타나면 애플리케이션이 시작된 것입니다. 이 옵션은 초기 실행 확인용으로만 사용하고 DB 연동 시 제거합니다. Spring Security 기본 설정에 따라 `/` 접속 시 로그인 또는 401/403 응답이 나올 수 있습니다.

## 설정 파일 및 환경 변수

- `backend/src/main/resources/application.yaml`: 현재 애플리케이션 이름(`moamu-backend`)만 설정
- `backend/.env.example`: 필요한 환경 변수 이름을 정리한 예시 파일
- 루트 `.gitignore`: 빌드 산출물, 개인 개발 환경, `.env` 비밀 파일 제외
- `backend/.editorconfig`: Java·Gradle·YAML 등 기본 편집 형식

현재 `.env.example`에는 아래 변수 이름이 정의되어 있습니다.

```dotenv
DB_URL=
DB_USERNAME=
DB_PASSWORD=
REDIS_HOST=
REDIS_PORT=
JWT_SECRET=
AI_SERVER_URL=
```

**주의:** 이 변수들은 아직 `application.yaml`과 연결되지 않았습니다. Spring Boot는 `.env` 파일을 기본적으로 자동으로 읽지 않으므로, 추후 팀에서 환경 변수 주입 방식을 정합니다. 비밀번호·토큰·API 키 등 실제 비밀값은 Git에 커밋하지 않습니다.

## 팀 협의 예정

프로젝트 초기 세팅에 필요한 다음 항목은 팀 회의에서 결정합니다.

- GitHub 브랜치·커밋·PR 규칙
- Gradle 추가 의존성 및 버전 관리
- Spring Profile 및 환경 변수 운용 방식
- JPA Auditing / QueryDSL 공통 설정
- Swagger 및 테스트 환경
- 공통 API 응답·예외 처리 기본 규칙

## 현재 진행 상태

- [x] Backend Spring Boot 프로젝트 생성
- [x] Java 21 / Spring Boot 3.5.16 / Gradle 설정
- [x] 컴파일 및 테스트 제외 빌드 확인
- [x] DB 자동 설정을 일시 제외한 서버 시작 확인
- [x] 도메인 및 global 기본 디렉토리 구성
- [ ] 팀 공통 개발 규칙 확정
- [ ] 추가 라이브러리 및 공통 설정
- [ ] Frontend 프로젝트 초기화
- [ ] MySQL / Redis / AI 서버 연결
- [ ] 전체 테스트 및 CI/CD 구성

---

**참고:** 1차 MVP에서는 DB에 사전 등록된 GoalTemplate과 준비된 작품을 사용합니다. 사진 인증 AI 연동 및 작품 조각 공개 기능은 추후 개발합니다.
