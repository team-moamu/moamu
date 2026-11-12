# SeedData — 서비스 초기 시드 (Flyway V6)

`v6_seed-service.sql`은 현재 Flyway V1~V6 스키마에 맞는 MySQL 8용 초기 카탈로그입니다. 

## 생성 데이터

| 데이터 | 추가 건수 |
| --- | ---: |
| 시스템 카테고리 / 활성 목표 | 각각 6 |
| 목표 인증 조건 | 8 |
| 준비 완료 작품 / 목표-작품 연결 | 각각 6 |
| 공용 미디어 | 7 (프로필 1, 작품 6) |

알고리즘, 출근, 학습, 운동, 수분 섭취, 영양제 목표를 제공합니다. 사진에서 확인할 수 있는 대상과 기록을 인증 조건으로 사용하며, 실제 수행 시간이나 섭취 여부까지 보장하지 않는다는 안내를 포함합니다.

회원, 비밀번호, 도전, 회차, 가상 인증, AI 작업은 생성하지 않습니다. 모든 ID는 AUTO_INCREMENT와 LAST_INSERT_ID()로 연결하며 고정 ID를 요구하지 않습니다. 기본 프로필 미디어는 등록만 하며 기존 회원에게 자동 연결하지 않습니다.

## 반영 순서

1. 대상 DB를 백업하고 서비스의 데이터 쓰기를 중지합니다. 이 스크립트는 동시 실행을 지원하지 않습니다.
2. Flyway V1~V6가 성공 적용되었는지 확인합니다. 6개 카탈로그 테이블은 비어 있어야 합니다. 회원 및 다른 미디어는 남아 있어도 되지만 아래 7개 이미지 키와 중복되면 중단합니다.
3. 서비스 S3 버킷에 아래 PNG 객체를 준비하고 접근 권한을 확인합니다. SQL은 이미지를 업로드하거나 실제 객체의 존재를 검증하지 않습니다.
4. 아래 Windows 또는 macOS 방법으로 SQL 파일 전체를 실행합니다. 두 방법 모두 프로젝트 루트에서 시작하며, MySQL 8 클라이언트가 설치되어 PATH에 등록되어 있어야 합니다. `DELIMITER`를 지원하는 스크립트 실행 모드를 사용합니다.

5. 마지막 조회 결과가 목표별 한 행씩 총 6행인지 확인합니다. 조건 수는 순서대로 2, 1, 2, 1, 1, 1입니다. 이미지 표시와 목표 선택도 확인한 뒤 서비스를 재개합니다.

## Windows 실행 방법 (PowerShell)

프로젝트 루트로 이동하고 MySQL 클라이언트에 접속합니다. 경로와 접속 정보는 실제 환경에 맞게 바꿉니다. `--password` 다음에는 값을 적지 않으며, 접속 시 표시되는 프롬프트에서 입력합니다.

```powershell
Set-Location C:\workSpace\moamu
mysql --host=DB_HOST --port=3306 --user=DB_USER --password --default-character-set=utf8mb4 --database=moamu
```

접속한 MySQL 프롬프트에서 실행합니다. `SOURCE` 경로는 프로젝트 루트 기준입니다.

```sql
SELECT DATABASE();
SOURCE backend/data/seedData/v6_seed-service.sql;
```

오류가 발생하면 서비스를 재개하기 전에 원인을 확인합니다. 성공 후 `EXIT;`로 클라이언트를 종료합니다. PowerShell의 파일 파이프 대신 MySQL의 `SOURCE`를 사용하므로 한글 데이터를 PowerShell 인코딩 변환 없이 읽습니다.

## macOS 실행 방법 (터미널)

프로젝트 루트로 이동한 뒤 실행합니다. 경로, 호스트, 사용자와 DB 이름은 실제 환경에 맞게 바꿉니다. 비밀번호는 프롬프트에서 입력합니다.

```sh
cd /path/to/moamu
mysql --host=DB_HOST --port=3306 --user=DB_USER --password --default-character-set=utf8mb4 --database=moamu < backend/data/seedData/v6_seed-service.sql
```

명령이 끝난 직후 종료 코드를 확인합니다. `0`이면 정상 종료이고, 그 외 값이면 오류를 확인합니다.

```sh
echo $?
```

두 OS 모두 접속 정보가 `mysql_config_editor`에 이미 등록되어 있다면 `--host`, `--port`, `--user`, `--password` 대신 `--login-path=moamu-service`를 사용할 수 있습니다. 원격 DB에 SSH 터널이 필요한 환경은 터널 연결 후 해당 로컬 호스트와 포트를 지정합니다.

## 필요한 S3 객체 키

```text
uploads/default_profile_mu.png
artworks/last_supper_solving_algorithms.png
artworks/mona_lisa_with_name_tag.png
artworks/girl_with_pearl_earring_studying.png
artworks/flute_boy_climbing_stairs.png
artworks/munch_drinking_water.png
artworks/gleaners_taking_supplements.png
```

이미지 키는 기존 개발 시드와 같은 경로입니다. 서비스 버킷의 실제 키가 다르면 실행 전에 SQL을 수정합니다. 확인하지 않은 파일 해시와 크기는 NULL로 둡니다. 현재 Java `MediaPurpose`가 PROFILE/VERIFICATION만 지원하므로 작품 행은 기존 코드와 같은 VERIFICATION을 사용하며, `user_id=NULL`과 `artwork.media_id` 연결로 공용 작품을 구분합니다.

## 오류와 재실행

SQL 오류 또는 경고가 발생하면 데이터 트랜잭션을 롤백합니다. 계정에 SELECT, INSERT와 CREATE ROUTINE, ALTER ROUTINE, EXECUTE 권한이 필요합니다. 파일은 자체 트랜잭션을 시작하므로 다른 작업을 수행하던 세션에서 실행하지 않습니다.

성공 후 다시 실행하면 카탈로그 존재 검사에서 중단합니다. 기존 카탈로그를 업데이트하거나 삭제하는 스크립트가 아닙니다. `--force`로 오류를 무시하거나 INSERT 부분만 따로 실행하지 않습니다. 실패 후 프로시저가 남아도 다음 실행 시작 시 정리합니다. 프로시저 생성·삭제는 MySQL의 DDL 특성상 데이터 롤백 대상에 포함되지 않으며, 실패한 INSERT의 AUTO_INCREMENT 번호는 건너뛸 수 있습니다.
