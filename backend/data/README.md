# 로컬 개발용 시드 데이터

`seed-dev.sql`은 기능 개발과 화면 확인에 사용할 데이터를 각자의 Docker MySQL에 넣는 파일입니다. 팀원의 DB와 데이터가 자동 동기화되지는 않습니다. 운영 DB에서는 실행하지 않습니다.

진행 순서: **Docker MySQL 실행 → Flyway 테이블 생성 → 시드 입력 → DBeaver 확인**

## 1. 실행 전 준비

- 팀 저장소를 이미 클론했다면 다시 클론하지 않습니다.
- Docker Desktop을 실행합니다.
- [DB 실행 안내](../../infra/README.md)에 따라 `infra/.env`를 준비합니다. 이미 있으면 덮어쓰지 않습니다.
- `infra/.env`는 Docker DB 설정, `backend/.env`는 Spring Boot 접속 설정입니다. 두 파일의 DB 이름·계정·비밀번호·포트가 서로 맞아야 합니다.
- 시드는 **14개 도메인 테이블이 모두 비어 있는 로컬 DB**에 한 번 넣습니다. 회원 데이터도 포함하므로 별도로 먼저 넣지 않습니다. Flyway 이력 테이블은 지우지 않습니다.

폴더 구조:

```text
moamu/
├── infra/
│   ├── compose.yaml
│   └── .env
└── backend/
    └── data/
        ├── README.md
        ├── seed-dev.sql
        ├── artworks/    # 사전 작품 이미지
        └── uploads/     # 기본 프로필 이미지
```

SQL은 이미지 파일을 복사하지 않고 `artworks/파일명.png`, `uploads/파일명.png` 같은 상대 경로를 저장합니다. 이미지 조회 기능에서는 `backend/data`를 기준으로 파일 경로를 해석해야 합니다.

## 2. Docker MySQL 실행

VS Code에서 `moamu` 폴더를 열고 새 터미널을 실행합니다. **프로젝트 루트 `moamu` 기준**입니다.

```shell
cd infra
docker compose config --quiet
docker compose up -d --wait --wait-timeout 240 mysql
docker compose ps
```

`config --quiet`가 아무 오류 없이 끝나고, `ps`에 MySQL 상태가 `healthy`로 표시되면 준비된 것입니다. 이후 시드 입력 명령도 **infra 폴더에서 실행**합니다.

## 3. Flyway로 테이블 생성

[프로젝트 실행 안내](../../README.md)와 [백엔드 안내](../README.md)에 따라 `backend/.env`를 준비하고 환경변수를 로딩한 뒤, 별도 터미널의 **backend 폴더**에서 local 프로필로 백엔드를 실행합니다.

macOS / Linux:

```shell
./gradlew bootRun --args='--spring.profiles.active=local'
```

Windows PowerShell:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

macOS에서 실행 권한 오류가 나면 `chmod +x gradlew`를 먼저 실행합니다. `.env` 파일을 저장하는 것만으로 Spring Boot에 환경변수가 전달되지는 않습니다.

Flyway 마이그레이션과 백엔드 시작이 성공했는지 확인한 뒤, 백엔드를 `Ctrl+C`로 중지합니다. 시드 입력 중 다른 요청이 데이터를 만들지 않도록 하기 위함입니다. **Docker MySQL은 계속 켜둡니다.**

## 4. 시드 데이터 입력

아래 방법 A 또는 B 중 **하나만** 사용합니다. 성공한 뒤 다른 방법으로 다시 입력하지 않습니다.

### 방법 A. 터미널에서 입력하고 DBeaver로 확인하기 (권장)

**infra 폴더**에서 실행합니다. MySQL 접속 값은 실행 중인 컨테이너의 환경변수에서 가져오므로 명령어에 비밀번호를 직접 적지 않습니다.

macOS / Linux:

```shell
docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysql --default-character-set=utf8mb4 --user="$MYSQL_USER" --database="$MYSQL_DATABASE"' < ../backend/data/seed-dev.sql
```

Windows PowerShell:

```powershell
$OutputEncoding = [System.Text.UTF8Encoding]::new($false)
Get-Content -Raw -Encoding UTF8 ../backend/data/seed-dev.sql | docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysql --default-character-set=utf8mb4 --user="$MYSQL_USER" --database="$MYSQL_DATABASE"'
```

오류 없이 끝나면 마지막에 테이블 건수, 회차 상태, 조건 목록과 판정 개수가 출력됩니다. 중간에 `ERROR`가 나왔다면 마지막 조회 결과만 보고 성공으로 판단하지 않습니다.

### 방법 B. DBeaver에서 직접 입력하기

먼저 아래 5번대로 로컬 MySQL에 연결합니다.

1. DBeaver에서 `seed-dev.sql` 파일을 엽니다. 연결된 DB에서 새 SQL 편집기를 열고 파일 전체를 붙여넣어도 됩니다.
2. 편집기 상단의 연결과 활성 데이터베이스가 **본인의 로컬 MySQL / moamu(또는 설정한 DB_NAME)**인지 확인합니다.
3. 텍스트 선택을 해제하고 **SQL 편집기 → SQL 스크립트 실행(Execute SQL Script)**을 선택합니다. 기본 단축키는 Windows `Alt+X`, macOS `Option+X`입니다.
4. 파일 전체를 순서대로 실행하고 결과를 확인합니다. 오류가 나면 실행을 중단합니다.

이 파일은 `DELIMITER`와 프로시저 내부 변수 `v_now`, `v_today`를 사용하므로 INSERT 부분만 선택해서 실행하면 안 됩니다. 구분자 처리 오류가 발생하면 임의로 SQL을 분해하지 말고 방법 A를 사용합니다. 앞선 실행이 일부 진행됐다면 아래 오류 안내를 먼저 확인합니다.

참고: [DBeaver SQL 실행 안내](https://dbeaver.com/docs/dbeaver/SQL-Execution/) · [단축키 안내](https://dbeaver.com/docs/dbeaver/Shortcuts/)

## 5. DBeaver 연결

1. DBeaver에서 **데이터베이스 → 새 데이터베이스 연결 → MySQL**을 선택합니다.
2. 아래 접속 정보를 입력합니다.

| DBeaver 항목 | 값 |
| --- | --- |
| Host | `127.0.0.1` |
| Port | `infra/.env`의 `DB_PORT` 값. 지정하지 않았다면 `3306` |
| Database | `infra/.env`의 `DB_NAME` 값 (팀 기본값: `moamu`) |
| Username | `infra/.env`의 `DB_USER` 값 (팀 기본값: `moamu`) |
| Password | `infra/.env`의 `DB_PASSWORD` 값 |

3. **연결 테스트(Test Connection)**를 누릅니다. 드라이버 다운로드 창이 나오면 다운로드합니다.
4. 연결 성공 후 완료하고, 연결 이름은 `MOAMU Local`처럼 구분하기 쉽게 지정합니다.

DBeaver는 Docker MySQL에 접속하는 도구입니다. DBeaver용 MySQL을 따로 설치하거나 새 DB를 만들 필요는 없습니다. 비밀번호는 README에 추가하지 않고 각자의 `.env`에서 확인합니다.

참고: [DBeaver 연결 안내](https://dbeaver.com/docs/dbeaver/Create-Connection/)

## 6. 데이터가 들어갔는지 확인

DBeaver 왼쪽 연결에서 **데이터베이스 → moamu → Tables**를 펼칩니다. 테이블을 더블클릭하고 **데이터(Data)** 탭을 선택합니다. 목록이나 데이터가 갱신되지 않으면 해당 항목을 새로고침합니다.

SQL 편집기에서도 확인할 수 있습니다. 아래 SELECT는 필요한 문장만 실행해도 됩니다.

```sql
SELECT DATABASE();
SHOW TABLES;
SELECT user_id, login_id, name, status, profile_media_id FROM users;
SELECT media_id, image_key, purpose FROM media;
SELECT challenge_id, COUNT(*) AS condition_count
FROM challenge_condition GROUP BY challenge_id;
SELECT verification_id, challenge_condition_id, status, reason
FROM verification_check ORDER BY verification_id, challenge_condition_id;
SELECT challenge_id, day_no, piece_index, status
FROM schedule ORDER BY challenge_id, day_no;
```

시드 직후 기대 결과:

| 항목 | 기대값 |
| --- | --- |
| 회원 / 사진 / 목표 / 작품 | 각각 3 / 2 / 6 / 6건 |
| 도전 / 회차 | 각각 3 / 21건 |
| 도전 1 / 2 / 3의 조건 개수 | 각각 3 / 1 / 5개 |
| 도전 1 회차 상태 | SUCCESS 1개, FAILED 1개, 미확정 5개 |
| 도전 2·3 회차 상태 | 각각 OPEN 1개, UPCOMING 6개 |
| 인증 시도 / 조건 판정 | 가상 성공 인증 1건 / MET 3건 |
| AI 작업 | 0건. 실제 인증 API 실행 시 생성 |

도전 1의 성공 인증은 **화면 확인용 가상 데이터**이며 실제 AI 판정 결과가 아닙니다. 다중 조건도 개발용 예시입니다. 날짜는 시드 실행 당시 한국 날짜를 기준으로 생성되므로, 며칠 뒤 인증 API를 테스트할 때는 회차 마감 여부를 확인합니다.

확인이 끝나면 백엔드를 다시 실행합니다. 테이블에 데이터가 들어간 것과 실제 로그인·AI 연동·화면 동작 성공은 별도로 확인합니다.

## 7. 자주 발생하는 오류

| 증상 | 확인할 내용 |
| --- | --- |
| `no configuration file provided` | 현재 터미널이 `compose.yaml`이 있는 `infra` 폴더인지 확인 |
| `DB_NAME is required` 등 | `infra/.env`의 필수 값 확인. `backend/.env`와 혼동하지 않기 |
| DBeaver 연결 거부 | Docker Desktop 실행 여부, `docker compose ps`, 호스트 포트 확인 |
| `Access denied` | DB 계정·비밀번호 확인. 이미 생성된 Docker 볼륨의 계정은 `.env` 수정만으로 바뀌지 않음 |
| 테이블이 없음 | 올바른 DB를 선택했는지, Flyway 적용이 성공했는지 확인 |
| `Seed requires empty domain tables` | 기존 데이터가 있어 시드가 중단됨. 이미 입력했다면 재실행하지 말고 데이터 확인 |
| `PROCEDURE ... already exists` | 실패한 실행이 남긴 프로시저인지 확인 후 아래 절차 사용 |
| `CREATE ROUTINE` / `EXECUTE` 등 권한 오류 | DB 담당자에게 해당 로컬 계정의 프로시저 생성·실행·삭제 권한 확인 요청 |

실패 원인을 해결했는데 `moamu_seed_dev_20261023` 프로시저만 남아 재실행을 막는다면, DBeaver에서 **해당 로컬 DB를 선택한 뒤** 아래 문장만 실행합니다.

```sql
DROP PROCEDURE IF EXISTS moamu_seed_dev_20261023;
```

이 명령은 프로시저만 제거하며 테이블 데이터는 지우지 않습니다. 도메인 테이블이 비어 있고 이전 오류를 해결했을 때만 시드를 다시 실행합니다. 기존 데이터를 삭제하거나 Docker 볼륨을 초기화하는 명령은 이 안내에 포함하지 않습니다.
