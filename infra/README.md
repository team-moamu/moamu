# Moamu 팀 개발용 MySQL + Redis

이 저장소는 팀원 각자의 컴퓨터에 동일한 버전과 설정의 MySQL을 실행하기 위한 파일입니다. Git에는 설정 파일을 공유하고, Docker Compose가 공식 `mysql:8.4.11` 이미지를 Docker Hub에서 받습니다. 앱 접속 계정은 모두 `moamu / 1234`이고, 데이터는 각자의 Docker 볼륨에 저장됩니다. 한 사람이 입력한 데이터가 다른 팀원에게 자동 동기화되지는 않습니다.

## 1. 저장소 받기

Docker Desktop을 설치하고 실행한 뒤, 팀 Git 저장소를 받은 뒤 `infra` 폴더로 이동합니다. 아래 `<팀 저장소 URL>`을 실제 주소로 바꾸세요. Linux에서 Docker Engine과 Compose를 사용하는 경우에도 같은 실행 명령을 사용할 수 있습니다.

```sh
git clone <팀 저장소 URL> moamu-team-db
cd moamu-team-db/infra
cp .env.example .env
```

이미 저장소를 받은 팀원은 파일이 변경될 때 `git pull`로 업데이트할 수 있습니다. `.env`는 개인 컴퓨터에서만 사용하며 Git에 올리지 않습니다.

| 파일 | 역할 |
| --- | --- |
| `compose.yaml` | MySQL 8.4.11 + Redis 8.2 실행, 로컬 포트 바인딩, 준비 상태 확인 |
| `.env.example` | 각자 `.env`로 복사하는 공통 로컬 접속 설정 |
| `examples/application-local.yml.example` | Spring Boot 연결 예시 |
| `verify.sql` | DB에 접속한 뒤 실행하는 확인 쿼리 |
| `PORT_3306.md` | macOS에서 3306 포트를 확보하는 방법 |
| `.gitignore`, `.dockerignore` | 실제 `.env`를 Git과 이미지 빌드에서 제외 |

`.env`는 저장소에 포함하지 않습니다. 위 명령으로 `.env.example`을 복사해 만든 뒤 사용하세요. macOS Finder에서는 `Command + Shift + .`로 숨김파일을 확인할 수 있습니다. Python 설치나 비밀번호 생성 도구는 필요하지 않습니다.

## 2. 공통 접속 정보

| 항목 | 값 |
| --- | --- |
| Host | `127.0.0.1` |
| Port | `3306` |
| Database | `moamu` |
| Username | `moamu` |
| Password | `1234` |
| MySQL | `8.4.11` |
| 문자셋 / 정렬 규칙 | `utf8mb4` / `utf8mb4_0900_ai_ci` |
| DB 기본 시간대 | UTC (`+00:00`) |

root는 컨테이너 내부에서 DB를 관리할 때 사용하는 별도 계정입니다. 앱·DBeaver·IntelliJ는 위의 `moamu` 계정으로 연결하세요. 이 비밀번호들은 로컬 개발 전용이며 운영 DB에는 사용하지 않습니다.

## 3. 실행

`compose.yaml`이 있는 폴더에서 실행합니다. `pull`은 이미지를 받고, `up`은 MySQL과 Redis 컨테이너를 시작합니다. 공식 공개 이미지를 사용하므로 Docker Hub 로그인은 필요하지 않습니다.

```sh
docker compose config --quiet
docker compose pull mysql redis
docker compose up -d --wait --wait-timeout 240 mysql redis
docker compose ps
```

MySQL 상태가 `healthy`면 계정 인증과 `SELECT 1` 쿼리 확인까지 통과한 것입니다. Redis의 `healthy`는 `PING` 확인을 통과했다는 뜻입니다. 처음 실행하면 `moamu` DB와 `moamu` 계정이 자동 생성됩니다. `.env`에 있는 값으로 접속하세요.

포트 충돌이 발생하면 [3306 포트 확보 방법](PORT_3306.md)을 확인합니다. 기존 서비스를 계속 사용해야 하는 팀원은 자신의 `.env`에서 `DB_PORT=3307` 등 비어 있는 포트로 바꾸고 같은 명령으로 다시 실행하세요. DB 도구와 컴퓨터에서 실행하는 백엔드에도 바꾼 포트를 사용합니다.

터미널에 동일한 이름의 DB 환경변수가 이미 설정돼 있으면 그 값이 `.env`보다 우선합니다. 공통 설정을 적용하려면 별도로 설정한 DB_HOST·DB_PORT·DB_NAME·DB_USER·DB_PASSWORD·MYSQL_ROOT_PASSWORD 환경변수를 해제한 뒤 실행하세요.

## 4. 접속 확인

```sh
docker compose exec mysql mysql --protocol=TCP -h 127.0.0.1 -u moamu -p moamu
```

비밀번호를 물으면 `1234`를 입력합니다. 접속 후 다음 쿼리를 실행하세요. 입력한 비밀번호는 화면에 표시되지 않습니다.

```sql
SELECT VERSION(), DATABASE(), CURRENT_USER();
SELECT @@character_set_database, @@collation_database, @@session.time_zone;
SELECT 1;
```

MySQL 8.4.11, DB `moamu`, `utf8mb4`, `utf8mb4_0900_ai_ci`, 시간대 `+00:00`, 마지막 결과 `1`을 확인합니다. 같은 쿼리는 `verify.sql`에도 있습니다.

## 5. 백엔드 연결과 테이블

Spring Boot 담당자는 `examples/application-local.yml.example`을 참고하여 `src/main/resources/application-local.yml`에 설정을 반영합니다. MySQL JDBC 드라이버 `com.mysql:mysql-connector-j` 의존성이 필요합니다.

백엔드 프로젝트 루트에도 배포본의 `.env`를 두고, 그 폴더에서 local 프로필로 실행합니다. 기존 `.env`가 있다면 DB와 Redis 항목을 합칩니다. 기존 백엔드 설정이 `DB_URL`·`DB_USERNAME`을 사용한다면 각각 JDBC URL과 `DB_USER` 값으로 연결하거나, 이 예시의 설정을 반영하세요. 기존 JWT 등 다른 항목은 유지합니다.

```sh
./gradlew bootRun --args='--spring.profiles.active=local'
```

예시의 `spring.config.import`가 `.env`를 properties 형식으로 읽습니다. IDE 실행도 작업 디렉터리를 프로젝트 루트로 맞춥니다. 백엔드를 같은 Compose 안에 추가할 때는 백엔드의 DB_HOST를 `mysql`, DB_PORT를 `3306`으로 설정하고, 백엔드 서비스에 아래 설정을 추가해 DB 준비 완료 후 실행하도록 연결합니다.

```yaml
depends_on:
  mysql:
    condition: service_healthy
  redis:
    condition: service_healthy
```

이 배포본은 DB와 접속 계정을 제공합니다. 사용자·도전·회차·AIJob 등의 테이블 구조는 백엔드 ERD와 Flyway 또는 Liquibase 마이그레이션으로 추가해야 합니다. 아직 ERD가 제공되지 않아 도메인 테이블과 테스트 데이터를 임의로 생성하지 않았습니다. Spring 예시는 `ddl-auto: validate`를 사용하므로 엔티티에 맞는 테이블이 없으면 초기 마이그레이션 적용이 필요합니다.

## Redis 접속 및 회원가입 담당자 안내

컴퓨터에서 실행하는 백엔드는 `REDIS_HOST=127.0.0.1`, `REDIS_PORT=6379`를 사용합니다. Redis 비밀번호는 없습니다. 포트는 `127.0.0.1`에만 열립니다. 같은 Compose 네트워크의 백엔드는 `REDIS_HOST=redis`, `REDIS_PORT=6379`로 설정합니다. MySQL 계정과 Redis 접속 설정은 서로 별개입니다.

```sh
docker compose exec redis redis-cli ping
```

`PONG`이 나오면 정상입니다. 임시 키의 저장과 만료는 아래 명령으로 확인합니다. 실제 인증정보 대신 테스트용 값만 사용합니다.

```sh
docker compose exec redis redis-cli SET moamu:dev:connection-check ok EX 60
docker compose exec redis redis-cli TTL moamu:dev:connection-check
docker compose exec redis redis-cli DEL moamu:dev:connection-check
```

`SET`은 `OK`, `TTL`은 남은 초(0~60)를 반환합니다. 만료되면 `TTL`은 `-2`가 됩니다. `verify.sql`은 MySQL 전용이므로 Redis 확인에는 위 명령을 사용합니다.

회원 정보는 MySQL에 저장하고, Redis에는 이메일 인증번호 등 만료되는 임시 데이터를 저장합니다. 담당자는 키 이름, 유효 시간, 재발송 제한, 인증 성공 후 삭제 규칙을 구현해야 합니다. 이메일 발송용 서비스 설정은 별도입니다.

Redis는 임시 저장 용도로 디스크 영속화를 끈 구성입니다. 컨테이너를 다시 시작하거나 재생성하면 인증번호 등 Redis 데이터는 사라지므로 다시 발급해야 합니다. MySQL의 기존 데이터 보존 방식은 그대로 유지합니다. 재시작 후에도 유지해야 하는 세션·토큰 저장에 사용하려면 영속화 정책을 먼저 조정하세요.

6379 포트가 이미 사용 중이면 `infra/.env`의 `REDIS_PORT`를 `6380` 등으로 바꾸고 다시 실행합니다. 컴퓨터에서 실행하는 백엔드의 환경변수도 같은 값으로 바꿉니다. 컨테이너 내부 포트는 계속 `6379`입니다. 기존 `.env`가 있는 팀원은 `.env.example`의 Redis 두 항목을 직접 합칩니다. 셸의 동일한 환경변수가 `.env`보다 우선하므로 충돌하는 값은 해제하세요.

## 6. 종료와 기존 데이터

```sh
docker compose down
```

다시 실행할 때는 3절의 실행 명령을 사용합니다. 데이터는 각 컴퓨터의 `moamu-team-db_mysql_data` 볼륨에 유지됩니다. 같은 컴퓨터에서 이 배포본을 다시 실행하면 기존 볼륨을 사용합니다. 이전 개인 구성의 `moamu-local_mysql_data`와는 별도입니다.

`.env`의 DB 이름·사용자·비밀번호는 빈 볼륨의 최초 초기화 때 적용됩니다. 기존 볼륨에서는 `.env` 수정만으로 DB 계정 비밀번호가 바뀌지 않습니다. 기존 DB에 접속해 계정을 변경하고 설정을 맞춰야 합니다. `docker compose down -v`는 데이터까지 삭제하므로 일반 종료에 사용하지 않습니다.

## 참고

- [Docker Desktop 설치](https://docs.docker.com/desktop/setup/install/)
- [MySQL 공식 이미지](https://hub.docker.com/_/mysql)
- [Compose 환경변수 연결](https://docs.docker.com/compose/how-tos/environment-variables/set-environment-variables/)
- [서비스 준비 상태와 실행 순서](https://docs.docker.com/compose/how-tos/startup-order/)
- [Spring Boot 외부 설정](https://docs.spring.io/spring-boot/reference/features/external-config.html)

- [Redis Docker 실행](https://redis.io/docs/latest/operate/oss_and_stack/install/install-stack/docker/)
