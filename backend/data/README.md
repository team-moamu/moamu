# 로컬 개발용 시드 데이터

현재 [Flyway 마이그레이션 V1~V4](../src/main/resources/db/migration/)가 적용된 로컬 MySQL용 시드입니다. 스키마는 Flyway가 만들고, 시드는 데이터만 입력합니다. 기존 로컬 DB 구조를 시드 SQL로 변경하지 않습니다.

실행 순서: **Flyway V1~V4 적용 확인 → 도메인 데이터 초기화 → `v1_seed-dev.sql` 한 번 실행 → 데이터 확인**

이미 현재 스키마를 사용하는 로컬 DB라면 테이블 구조와 `flyway_schema_history`는 유지하고, 14개 도메인 테이블의 데이터만 비운 뒤 4번부터 진행합니다. 새 DB라면 1~3번을 먼저 진행합니다. 시드 파일은 기존 데이터를 자동 삭제하지 않습니다.

## 파일과 대상 스키마

```text
backend/data/
├── README.md
└── seedData/
    ├── README.md
    └── v1_seed-dev.sql   # 빈 DB에 기본 데이터와 최종 영문 이미지 키 입력
```

시드는 `v1_seed-dev.sql` 하나만 실행합니다. 이름의 v1은 Flyway V1만 적용하라는 의미가 아니며, 대상 스키마는 V4까지 적용된 상태입니다. 파일명의 대소문자도 그대로 사용합니다.

| 현재 스키마 | 시드에 반영한 내용 |
| --- | --- |
| `challenge.category_id` → `cate_id` | `cate_id`로 저장 |
| `artwork.image_key`, `masterpiece_id`, `artwork_style` 삭제, `media_id` 추가 | 작품 경로를 `media.image_key`에 저장하고 `artwork.media_id`로 연결 |
| `media.status` 삭제 | INSERT와 검증에서 제외 |
| `challenge_condition.required` 삭제 | 도전 조건 INSERT와 결과 조회에서 제외 |
| `goal_template_artwork.sort_order` 삭제 | 연결 데이터 INSERT에서 제외 |
| `goal_template_condition.condition_type` 삭제 | 템플릿 조건 INSERT에서 제외 |

`challenge_condition.condition_type`은 여전히 필수 컬럼입니다. 기본 시드는 기존 예시의 유형을 각 조건에 명시해서 보존합니다. `goal_template_condition.required`와 `sort_order`는 그대로 존재하므로 유지합니다.

## 1. 로컬 MySQL 준비

[프로젝트 환경 설정](../../README.md)과 [Docker DB 실행 안내](../../infra/README.md)를 참고합니다. `infra/.env`는 Docker 계정·포트 설정이고, `backend/.env`는 백엔드 접속 설정입니다.

프로젝트 루트에서 실행합니다.

```sh
cd infra
docker compose config --quiet
docker compose up -d --wait --wait-timeout 240 mysql
docker compose ps
```

MySQL이 `healthy`인지 확인합니다. 이후 시드 입력 명령은 **infra 폴더 기준**입니다.

## 2. 기존 로컬 DB 구조가 다를 때

먼저 기존 DB에서 Flyway 적용 이력을 확인합니다.

```sql
SELECT version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

- 이력이 현재 마이그레이션과 일치하고 V2~V4만 미적용이라면, 백엔드 시작 시 Flyway가 남은 버전을 적용합니다. 기존 데이터가 있다면 먼저 백업하고 삭제되는 컬럼을 확인합니다.
- 이력 테이블이 없거나, 체크섬 오류가 나거나, 테이블을 수동으로 변경했다면 **새 로컬 데이터베이스에 현재 V1~V4를 적용하는 방법을 권장합니다.**
- 기본 시드는 14개 도메인 테이블이 모두 비어 있어야 합니다. 기존 데이터가 있는 DB에는 덧붙이지 않습니다.

기존 데이터를 보관하면서 시작하려면 같은 MySQL 컨테이너에 `moamu_seed_v4` 같은 새 DB를 만듭니다. 아래는 기본 개발 계정이 `moamu`인 경우입니다.

infra 폴더에서 관리자 클라이언트를 열고, 프롬프트에 `infra/.env`의 `MYSQL_ROOT_PASSWORD`를 입력합니다.

```sh
docker compose exec mysql sh -c 'exec mysql --user=root --password'
```

MySQL 프롬프트에서 실행합니다. 사용자 계정이 다르면 GRANT의 `moamu`를 실제 `DB_USER`로 바꿉니다. 이미 사용 중인 DB 이름이라면 다른 새 이름을 선택합니다.

```sql
CREATE DATABASE moamu_seed_v4 CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
GRANT ALL PRIVILEGES ON moamu_seed_v4.* TO 'moamu'@'%';
EXIT;
```

`backend/.env`의 `DB_URL`도 선택한 DB로 변경합니다. 아래 포트는 예시이며 실제 `DB_PORT`에 맞춥니다.

```dotenv
DB_URL=jdbc:mysql://127.0.0.1:3306/moamu_seed_v4
```

이미 생성된 Docker 볼륨은 `infra/.env`의 `DB_NAME`만 바꿔도 초기화되거나 새 DB가 생기지 않습니다. Flyway 이력을 지우거나 `repair`로 오류만 숨기지 말고, 새 DB에 현재 마이그레이션을 적용합니다.

## 3. Flyway V1~V4 적용

[환경변수 로딩 안내](../../README.md#4-현재-터미널에-환경변수-로딩)에 따라 변경한 `backend/.env`를 실행 프로세스에 전달합니다. DB 접속 값과 현재 애플리케이션에서 요구하는 S3 설정도 준비합니다.

별도 터미널의 **backend 폴더**에서 실행합니다.

macOS / Linux:

```sh
./gradlew bootRun --args='--spring.profiles.active=local'
```

Windows PowerShell:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

Flyway V1~V4 적용 및 애플리케이션 시작 성공을 확인한 뒤, 백엔드를 `Ctrl+C`로 중지합니다. 시드 입력 중 다른 요청이 데이터를 생성하지 않도록 하며, Docker MySQL은 계속 실행해 둡니다.

선택한 DB의 `flyway_schema_history`에 버전 1~4가 모두 `success=1`로 기록되어 있어야 합니다. 시드가 구조 불일치 오류를 내면 이전 DB에 접속한 것은 아닌지 먼저 확인합니다.

## 4. 시드 실행

터미널 또는 DBeaver 중 한 가지 방법으로 **`v1_seed-dev.sql` 전체를 한 번 실행**합니다. 기본 데이터와 최종 이미지 키가 함께 입력됩니다. 데이터 초기화 없이 다시 실행하면 기존 데이터 보호를 위해 중단됩니다.

### 터미널

infra 폴더에서 실행합니다. `seed_db` / `$seedDb`는 Flyway를 적용한 실제 DB 이름으로 지정합니다. 아래 예시는 앞에서 만든 `moamu_seed_v4`입니다. 비밀번호는 컨테이너의 개발 계정 환경변수를 사용합니다.

macOS / Linux:

```sh
seed_db=moamu_seed_v4
docker compose exec -T -e SEED_DB="$seed_db" mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysql --default-character-set=utf8mb4 --user="$MYSQL_USER" --database="$SEED_DB"' < ../backend/data/seedData/v1_seed-dev.sql
```

Windows PowerShell:

```powershell
$seedDb = "moamu_seed_v4"
$OutputEncoding = [System.Text.UTF8Encoding]::new($false)
Get-Content -Raw -Encoding UTF8 ../backend/data/seedData/v1_seed-dev.sql | docker compose exec -T -e "SEED_DB=$seedDb" mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysql --default-character-set=utf8mb4 --user="$MYSQL_USER" --database="$SEED_DB"'
if ($LASTEXITCODE -ne 0) { throw "시드 실패: 오류를 해결한 뒤 다시 실행하세요." }
```

### DBeaver

1. 호스트 `127.0.0.1`, `infra/.env`의 포트·개발 계정·비밀번호로 MySQL에 연결합니다.
2. 활성 데이터베이스를 Flyway가 적용된 DB로 선택합니다. 새 DB를 만들었다면 `moamu_seed_v4`를 선택합니다.
3. `seedData/v1_seed-dev.sql` 전체를 **SQL 스크립트 실행**으로 실행합니다.
4. 오류 없이 끝나면 아래 기대 건수와 이미지 키를 확인합니다.

시드는 `DELIMITER`와 프로시저를 사용합니다. INSERT 부분만 선택해 실행하지 않습니다. 구분자 처리 문제가 있으면 터미널 방법을 사용합니다.

## 5. 데이터 확인

```sql
SELECT DATABASE();
SELECT user_id, login_id, name, status, profile_media_id FROM users;
SELECT media_id, image_key, purpose, user_id FROM media ORDER BY media_id;
SELECT a.artwork_id, a.artwork_title, a.media_id, m.image_key
FROM artwork a JOIN media m ON m.media_id = a.media_id
ORDER BY a.artwork_id;
SELECT challenge_id, COUNT(*) AS condition_count
FROM challenge_condition GROUP BY challenge_id;
SELECT verification_id, challenge_condition_id, status, reason
FROM verification_check ORDER BY verification_id, challenge_condition_id;
SELECT challenge_id, day_no, piece_index, status
FROM schedule ORDER BY challenge_id, day_no;
```

| 항목 | 기대값 |
| --- | --- |
| 회원 / 카테고리 / 목표 / 작품 | 3 / 6 / 6 / 6건 |
| 미디어 | 8건: 기본 프로필 1 + 가상 인증 1 + 작품 경로 6 |
| 목표 템플릿 조건 / 목표-작품 연결 | 12 / 6건 |
| 도전 / 회차 / 도전 조건 | 3 / 21 / 9건 |
| 도전 1 / 2 / 3의 조건 수 | 3 / 1 / 5개 |
| 도전 1 회차 | FAILED 1, SUCCESS 1, OPEN 1, UPCOMING 4 |
| 도전 2·3 회차 | 각각 OPEN 1, UPCOMING 6 |
| 인증 시도 / 조건 판정 | 가상 성공 인증 1 / MET 판정 3건 |
| AI 작업 / 소셜 계정 | 각각 0건 |
| 이미지 키 | media 8개 행 모두 아래 표의 최종 영문 경로로 저장 |

도전 1의 성공 인증은 화면 확인용 합성 데이터입니다. 현재 사진 업로드 API는 AI 작업을 자동 생성하지 않으므로, 시드의 `ai_task=0`은 정상입니다.

회차 날짜는 실행 당시 한국 날짜를 기준으로 만들며, 시드의 시각 컬럼은 기존 방식대로 UTC 기준입니다. 며칠 뒤 테스트할 때는 회차 날짜와 마감 시각을 확인합니다. 로그인 비밀번호 해시는 기존 시드 값을 유지했으며, 실제 로그인 성공을 보장하는 데이터는 아닙니다.

## 6. 이미지 경로와 S3

**DB에는 이미지 파일이 아니라 S3 객체 키를 저장합니다.** 작품 경로는 `artwork → media_id → media.image_key`로 조회합니다. SQL은 파일을 복사하거나 S3에 업로드하지 않으며, 저장소의 로컬 이미지 폴더도 필요로 하지 않습니다.

시드 실행 직후 저장되는 키는 다음과 같습니다.

| media_id | 연결 | image_key |
| --- | --- | --- |
| 1 | 회원 1~3의 공용 프로필 | `uploads/default_profile_mu.png` |
| 2 | 가상 인증 1 | `artworks/last_supper_solving_algorithms.png` |
| 3 | 작품 1 | `artworks/last_supper_solving_algorithms.png` |
| 4 | 작품 2 | `artworks/mona_lisa_with_name_tag.png` |
| 5 | 작품 3 | `artworks/girl_with_pearl_earring_studying.png` |
| 6 | 작품 4 | `artworks/flute_boy_climbing_stairs.png` |
| 7 | 작품 5 | `artworks/munch_drinking_water.png` |
| 8 | 작품 6 | `artworks/gleaners_taking_supplements.png` |

실제 이미지를 표시하려면 사용하는 S3 버킷에 위 키와 일치하는 객체가 있어야 합니다. 키는 예시 데이터이며, `S3_PROFILE_PREFIX` 등 환경변수로 시드 값이 자동 치환되지는 않습니다. 버킷에서 다른 키를 사용한다면 `v1_seed-dev.sql`의 해당 경로를 맞춥니다.

`purpose`는 사용자 사진의 프로필·인증 용도를 구분합니다. **작품 경로 행(media 3~8)의 `VERIFICATION`은 아직 남아 있는 임시값이며, 작품의 올바른 용도 분류가 아닙니다.** 현재 필수 컬럼과 Java enum에 맞는 작품용 저장 값은 별도로 확정해야 합니다. 작품 연결은 `artwork.media_id`로 구분하며, 이 행들로 사용자 인증 시도를 생성하지 않습니다. 실제 파일을 확인하지 않은 작품 행의 해시와 크기는 NULL입니다. media 1·2의 해시와 크기는 기존 시드 예시 값을 유지합니다.

## 7. 오류 처리

| 오류 | 확인할 내용 |
| --- | --- |
| 스키마 불일치 / `Unknown column` | 선택한 DB와 Flyway V1~V4 적용 여부 확인. 시드를 쪼개 실행하지 않기 |
| Flyway 체크섬 불일치 | 현재 마이그레이션과 기존 DB 이력이 다름. 이력을 삭제하지 말고 새 로컬 DB에 적용 |
| `Seed requires empty domain tables` | V1이 기존 데이터 보호를 위해 중단됨. 재실행하지 말고 현재 데이터 확인 또는 새 DB 사용 |
| `Access denied` / `Unknown database` | DB 이름·계정·권한 확인. 새 DB에 개발 계정 권한을 부여했는지 확인 |
| 프로시저 권한 오류 | 로컬 계정의 CREATE ROUTINE, ALTER ROUTINE, EXECUTE, CREATE TEMPORARY TABLES 및 데이터 읽기·쓰기 권한 확인 |

데이터 입력 중 SQL 오류·경고가 발생하면 시드의 트랜잭션 전체가 롤백됩니다. 원인을 해결하고 도메인 테이블이 비어 있는지 확인한 뒤 같은 파일을 다시 실행합니다. 실패 후 남은 시드 전용 프로시저는 다음 실행 시작 시 정리합니다. `--force`로 오류를 무시하지 않습니다.
