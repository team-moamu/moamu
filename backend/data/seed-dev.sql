-- MOAMU 로컬 개발용 시드 (2026-10-23 수정). 운영 사용 금지.
-- 저장 권장 위치: 프로젝트/backend/data/seed-dev.sql
-- Flyway 공통 migration 폴더에 넣지 않습니다. 파일 저장만으로 실행되지 않습니다.

-- 전제: 제공한 스키마가 적용되어 있고 14개 도메인 테이블이 모두 비어 있는 로컬 DB.
-- 기존 회원 시드도 포함합니다. 먼저 별도로 넣지 마세요. 기존 데이터가 있으면 중단합니다.
-- 이미지 파일은 SQL로 복사되지 않습니다. backend/data/artworks, uploads를 준비하세요.
-- 비밀번호는 개발용 BCrypt 해시이며 실제 로그인은 A의 PasswordEncoder 구현과 확인하세요.

-- [로컬 DB에 넣기: macOS / zsh]
-- 1. Docker Desktop을 켜고, 터미널에서 프로젝트의 infra 폴더로 이동합니다.
--    cd /본인/프로젝트경로/moamu/infra
-- 2. infra/.env를 준비하고 MySQL을 실행합니다. backend/.env와는 다른 파일입니다.
--    docker compose up -d --wait --wait-timeout 240 mysql
-- 3. 팀의 백엔드 실행 안내에 따라 Flyway 마이그레이션을 적용합니다.
--    적용을 마치면 백엔드를 중지하고 아래 시드를 실행하세요(동시 데이터 생성 방지).
-- 4. infra 폴더에서 실행합니다. 아래 경로는 backend/data 저장했을 때 기준입니다.
--    docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysql --default-character-set=utf8mb4 --user="$MYSQL_USER" --database="$MYSQL_DATABASE"' < ../backend/data/seed-dev.sql
-- 5. 맨 아래 건수와 조각 상태 조회 결과를 확인하고 백엔드를 다시 실행합니다.

-- [Windows PowerShell: 1~3번 준비 후 infra 폴더에서]
-- Get-Content -Raw -Encoding UTF8 ../backend/data/seed-dev.sql | docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysql --default-character-set=utf8mb4 --user="$MYSQL_USER" --database="$MYSQL_DATABASE"'
-- 한국어 전송이 깨지는 구형 PowerShell에서는 먼저 다음을 실행합니다.
-- $OutputEncoding = [System.Text.UTF8Encoding]::new($false)

-- [DB 도구에서 실행]
-- 본인의 로컬 DB 연결/스키마를 선택하고 파일 전체를 SQL 스크립트로 실행하세요.
-- DELIMITER와 프로시저를 지원하지 않는 실행기는 위 mysql CLI 방식을 사용하세요.
-- INSERT 부분만 실행하면 프로시저 내부 변수 v_now/v_today를 사용할 수 없습니다.
-- CREATE ROUTINE, EXECUTE, ALTER ROUTINE 및 해당 테이블 읽기/쓰기 권한이 필요합니다.
-- 실패하면 --force로 넘기거나 기존 데이터를 지우지 말고 원인을 확인하세요.
-- 실패 후 이 파일의 프로시저가 남아 재실행을 막으면, 같은 로컬 DB에서 다음만 실행합니다.
-- DROP PROCEDURE IF EXISTS moamu_seed_dev_20261023;
-- 이 명령은 프로시저만 제거합니다. 기존 테이블/데이터를 초기화하지 않습니다.

-- [조각 표시]
-- 조각 표시 테스트용: 도전1 성공 1개·실패 1개·미확정 5개.
-- 성공 인증 기록은 화면 확인용 가상 데이터이며 실제 AI 판정 결과가 아닙니다.

SET NAMES utf8mb4;
DELIMITER $$
CREATE PROCEDURE moamu_seed_dev_20261023()
BEGIN
 DECLARE v_now DATETIME;
 DECLARE v_today DATE;
 DECLARE EXIT HANDLER FOR SQLEXCEPTION
 BEGIN
  ROLLBACK;
  RESIGNAL;
 END;
 SET v_now = UTC_TIMESTAMP();
 SET v_today = DATE(UTC_TIMESTAMP() + INTERVAL 9 HOUR);
 START TRANSACTION;

 IF EXISTS(SELECT 1 FROM `users` LIMIT 1) OR EXISTS(SELECT 1 FROM `media` LIMIT 1) OR EXISTS(SELECT 1 FROM `goal_category` LIMIT 1) OR EXISTS(SELECT 1 FROM `goal_template` LIMIT 1) OR EXISTS(SELECT 1 FROM `goal_template_condition` LIMIT 1) OR EXISTS(SELECT 1 FROM `artwork` LIMIT 1) OR EXISTS(SELECT 1 FROM `goal_template_artwork` LIMIT 1) OR EXISTS(SELECT 1 FROM `challenge` LIMIT 1) OR EXISTS(SELECT 1 FROM `challenge_condition` LIMIT 1) OR EXISTS(SELECT 1 FROM `schedule` LIMIT 1) OR EXISTS(SELECT 1 FROM `verification` LIMIT 1) OR EXISTS(SELECT 1 FROM `verification_check` LIMIT 1) OR EXISTS(SELECT 1 FROM `ai_task` LIMIT 1) OR EXISTS(SELECT 1 FROM `user_social_account` LIMIT 1) THEN
 SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Seed requires empty domain tables; existing data was not changed.';
 END IF;

INSERT INTO `users` (`user_id`, `login_id`, `password`, `name`, `status`, `del_yn`, `frst_reg_dt`, `upd_dt`, `profile_media_id`) VALUES
(1, 'member01@example.com', '$2a$10$Wxdcunlt5Gsu4ouBhCL6N.an.gaNajoAihufNvvnRxgjju1jK.QOa', '김손', 'ACTIVE', 'N', v_now, v_now, NULL),
(2, 'member02@example.com', '$2a$10$Wxdcunlt5Gsu4ouBhCL6N.an.gaNajoAihufNvvnRxgjju1jK.QOa', '김병', 'ACTIVE', 'N', v_now, v_now, NULL),
(3, 'member03@example.com', '$2a$10$Wxdcunlt5Gsu4ouBhCL6N.an.gaNajoAihufNvvnRxgjju1jK.QOa', '김찬', 'ACTIVE', 'N', v_now, v_now, NULL);

-- 기본 프로필은 시스템 공용 파일: 업로더 NULL, purpose=PROFILE. 기본 이미지 여부는 이 시드의 공용 파일 연결로 구분합니다.

INSERT INTO `media` (`media_id`, `image_key`, `file_hash`, `content_type`, `file_size`, `status`, `user_id`, `purpose`, `frst_reg_dt`) VALUES
(1, 'uploads/기본 프로필 뮤.png', '05bfda986db50f1418719a5f129f4d764a462b74f5b005d3339563c8c5672eb4', 'image/png', 1502568, 'SUCCESS', NULL, 'PROFILE', v_now);

UPDATE users SET profile_media_id = 1 WHERE user_id IN (1,2,3);

INSERT INTO `goal_category` (`cate_id`, `cate_code`, `cate_name`, `cate_source`, `frst_reg_dt`) VALUES
(1, 'DEV_ALGORITHM', '알고리즘', 'SYSTEM', v_now);

INSERT INTO `goal_template` (`goal_template_id`, `cate_id`, `goal_name`, `description`, `photo_guide`, `verification_scope`, `status`, `sort_order`, `frst_reg_dt`) VALUES
(1, 1, '알고리즘 문제 풀이 기록하기', '개발용 샘플. AI/PM 확정 조건으로 교체 필요.', '문제·풀이 코드와 실행 또는 채점 결과가 보이도록 촬영해주세요. 날짜는 보조 확인 항목입니다.', '개발용 예시: 사진에 보이는 대상만 확인하며 실제 수행 여부, 시간, 양, 섭취 여부는 보장하지 않습니다.', 'ACTIVE', 1, v_now);

INSERT INTO `goal_template_condition` (`goal_template_condition_id`, `goal_template_id`, `code`, `condition_type`, `description`, `required`, `sort_order`, `frst_reg_dt`) VALUES
(1, 1, 'C1', 'SCREEN_RECORD', '문제와 풀이 코드가 표시된 화면이 사진에 보여야 한다', 'Y', 1, v_now),
(7, 1, 'C2', 'RESULT_OUTPUT', '풀이 코드의 실행 결과 또는 채점 결과가 사진에서 확인되어야 한다', 'Y', 2, v_now),
(8, 1, 'C3', 'DATE_VISIBLE', '기록한 날짜가 사진에 보여야 한다', 'N', 3, v_now);

INSERT INTO `artwork` (`artwork_id`, `status`, `artwork_title`, `image_key`, `completed_at`, `frst_reg_dt`) VALUES
(1, 'READY', '개발용 작품 1', 'artworks/알고리즘 푸는 최후의 만찬.png', v_now, v_now);

INSERT INTO `goal_template_artwork` (`goal_template_artwork_id`, `goal_template_id`, `artwork_id`, `sort_order`, `frst_reg_dt`) VALUES
(1, 1, 1, 1, v_now);

INSERT INTO `goal_category` (`cate_id`, `cate_code`, `cate_name`, `cate_source`, `frst_reg_dt`) VALUES
(2, 'DEV_WORK', '출근', 'SYSTEM', v_now);

INSERT INTO `goal_template` (`goal_template_id`, `cate_id`, `goal_name`, `description`, `photo_guide`, `verification_scope`, `status`, `sort_order`, `frst_reg_dt`) VALUES
(2, 2, '명찰 착용 기록하기', '개발용 샘플. AI/PM 확정 조건으로 교체 필요.', '개인정보를 가린 명찰이 보이도록 촬영해주세요.', '개발용 예시: 사진에 보이는 대상만 확인하며 실제 수행 여부, 시간, 양, 섭취 여부는 보장하지 않습니다.', 'ACTIVE', 2, v_now);

INSERT INTO `goal_template_condition` (`goal_template_condition_id`, `goal_template_id`, `code`, `condition_type`, `description`, `required`, `sort_order`, `frst_reg_dt`) VALUES
(2, 2, 'C1', 'OBJECT_PRESENT', '명찰이 사진에 보여야 한다', 'Y', 1, v_now);

INSERT INTO `artwork` (`artwork_id`, `status`, `artwork_title`, `image_key`, `completed_at`, `frst_reg_dt`) VALUES
(2, 'READY', '개발용 작품 2', 'artworks/명찰 챙긴 모나리자.png', v_now, v_now);

INSERT INTO `goal_template_artwork` (`goal_template_artwork_id`, `goal_template_id`, `artwork_id`, `sort_order`, `frst_reg_dt`) VALUES
(2, 2, 2, 1, v_now);

INSERT INTO `goal_category` (`cate_id`, `cate_code`, `cate_name`, `cate_source`, `frst_reg_dt`) VALUES
(3, 'DEV_STUDY', '학습', 'SYSTEM', v_now);

INSERT INTO `goal_template` (`goal_template_id`, `cate_id`, `goal_name`, `description`, `photo_guide`, `verification_scope`, `status`, `sort_order`, `frst_reg_dt`) VALUES
(3, 3, '학습 기록하기', '개발용 샘플. AI/PM 확정 조건으로 교체 필요.', '책·노트, 직접 쓴 학습 메모, 요약 또는 문제 풀이 결과와 학습 공간이 함께 보이도록 촬영해주세요. 날짜는 보조 확인 항목입니다.', '개발용 예시: 사진에 보이는 대상만 확인하며 실제 수행 여부, 시간, 양, 섭취 여부는 보장하지 않습니다.', 'ACTIVE', 3, v_now);

INSERT INTO `goal_template_condition` (`goal_template_condition_id`, `goal_template_id`, `code`, `condition_type`, `description`, `required`, `sort_order`, `frst_reg_dt`) VALUES
(3, 3, 'C1', 'OBJECT_PRESENT', '책 또는 학습 노트가 사진에 보여야 한다', 'Y', 1, v_now),
(9, 3, 'C2', 'HANDWRITTEN_RECORD', '직접 작성한 학습 메모가 사진에서 식별되어야 한다', 'Y', 2, v_now),
(10, 3, 'C3', 'RESULT_OUTPUT', '학습한 내용을 정리한 요약문 또는 문제 풀이 결과가 사진에 보여야 한다', 'Y', 3, v_now),
(11, 3, 'C4', 'PLACE', '학습 자료를 놓은 책상이나 학습 공간이 사진에서 확인되어야 한다', 'Y', 4, v_now),
(12, 3, 'C5', 'DATE_VISIBLE', '학습 기록에 적힌 날짜가 사진에서 식별되어야 한다', 'N', 5, v_now);

INSERT INTO `artwork` (`artwork_id`, `status`, `artwork_title`, `image_key`, `completed_at`, `frst_reg_dt`) VALUES
(3, 'READY', '개발용 작품 3', 'artworks/공부하는 진주귀걸이를 한 소녀.png', v_now, v_now);

INSERT INTO `goal_template_artwork` (`goal_template_artwork_id`, `goal_template_id`, `artwork_id`, `sort_order`, `frst_reg_dt`) VALUES
(3, 3, 3, 1, v_now);

INSERT INTO `goal_category` (`cate_id`, `cate_code`, `cate_name`, `cate_source`, `frst_reg_dt`) VALUES
(4, 'DEV_EXERCISE', '운동', 'SYSTEM', v_now);

INSERT INTO `goal_template` (`goal_template_id`, `cate_id`, `goal_name`, `description`, `photo_guide`, `verification_scope`, `status`, `sort_order`, `frst_reg_dt`) VALUES
(4, 4, '계단 이용 기록하기', '개발용 샘플. AI/PM 확정 조건으로 교체 필요.', '안전한 위치에서 계단이 보이도록 촬영해주세요.', '개발용 예시: 사진에 보이는 대상만 확인하며 실제 수행 여부, 시간, 양, 섭취 여부는 보장하지 않습니다.', 'ACTIVE', 4, v_now);

INSERT INTO `goal_template_condition` (`goal_template_condition_id`, `goal_template_id`, `code`, `condition_type`, `description`, `required`, `sort_order`, `frst_reg_dt`) VALUES
(4, 4, 'C1', 'OBJECT_PRESENT', '계단이 사진에 보여야 한다', 'Y', 1, v_now);

INSERT INTO `artwork` (`artwork_id`, `status`, `artwork_title`, `image_key`, `completed_at`, `frst_reg_dt`) VALUES
(4, 'READY', '개발용 작품 4', 'artworks/계단 오르는 피리부는 소년.png', v_now, v_now);

INSERT INTO `goal_template_artwork` (`goal_template_artwork_id`, `goal_template_id`, `artwork_id`, `sort_order`, `frst_reg_dt`) VALUES
(4, 4, 4, 1, v_now);

INSERT INTO `goal_category` (`cate_id`, `cate_code`, `cate_name`, `cate_source`, `frst_reg_dt`) VALUES
(5, 'DEV_WATER', '수분 섭취', 'SYSTEM', v_now);

INSERT INTO `goal_template` (`goal_template_id`, `cate_id`, `goal_name`, `description`, `photo_guide`, `verification_scope`, `status`, `sort_order`, `frst_reg_dt`) VALUES
(5, 5, '물 마시기 기록하기', '개발용 샘플. AI/PM 확정 조건으로 교체 필요.', '물컵 또는 물병이 보이도록 촬영해주세요.', '개발용 예시: 사진에 보이는 대상만 확인하며 실제 수행 여부, 시간, 양, 섭취 여부는 보장하지 않습니다.', 'ACTIVE', 5, v_now);

INSERT INTO `goal_template_condition` (`goal_template_condition_id`, `goal_template_id`, `code`, `condition_type`, `description`, `required`, `sort_order`, `frst_reg_dt`) VALUES
(5, 5, 'C1', 'OBJECT_PRESENT', '물컵 또는 물병이 사진에 보여야 한다', 'Y', 1, v_now);

INSERT INTO `artwork` (`artwork_id`, `status`, `artwork_title`, `image_key`, `completed_at`, `frst_reg_dt`) VALUES
(5, 'READY', '개발용 작품 5', 'artworks/물마시는 뭉크.png', v_now, v_now);

INSERT INTO `goal_template_artwork` (`goal_template_artwork_id`, `goal_template_id`, `artwork_id`, `sort_order`, `frst_reg_dt`) VALUES
(5, 5, 5, 1, v_now);

INSERT INTO `goal_category` (`cate_id`, `cate_code`, `cate_name`, `cate_source`, `frst_reg_dt`) VALUES
(6, 'DEV_SUPPLEMENT', '영양제', 'SYSTEM', v_now);

INSERT INTO `goal_template` (`goal_template_id`, `cate_id`, `goal_name`, `description`, `photo_guide`, `verification_scope`, `status`, `sort_order`, `frst_reg_dt`) VALUES
(6, 6, '영양제 챙기기 기록하기', '개발용 샘플. AI/PM 확정 조건으로 교체 필요.', '영양제 용기 또는 포장이 보이도록 촬영해주세요.', '개발용 예시: 사진에 보이는 대상만 확인하며 실제 수행 여부, 시간, 양, 섭취 여부는 보장하지 않습니다.', 'ACTIVE', 6, v_now);

INSERT INTO `goal_template_condition` (`goal_template_condition_id`, `goal_template_id`, `code`, `condition_type`, `description`, `required`, `sort_order`, `frst_reg_dt`) VALUES
(6, 6, 'C1', 'OBJECT_PRESENT', '영양제 용기 또는 포장이 사진에 보여야 한다', 'Y', 1, v_now);

INSERT INTO `artwork` (`artwork_id`, `status`, `artwork_title`, `image_key`, `completed_at`, `frst_reg_dt`) VALUES
(6, 'READY', '개발용 작품 6', 'artworks/영양제 먹는 이삭줍는 여인들.png', v_now, v_now);

INSERT INTO `goal_template_artwork` (`goal_template_artwork_id`, `goal_template_id`, `artwork_id`, `sort_order`, `frst_reg_dt`) VALUES
(6, 6, 6, 1, v_now);

-- 회원 1: 기본 회차를 만든 뒤 아래에서 성공/실패/미확정 표시용으로 변경합니다.
INSERT INTO challenge (challenge_id,user_id,cate_id,challenge_title,goal_text,photo_guide,duration_days,start_date,end_date,status,goal_template_id,artwork_id,verification_scope,grid_rows,grid_cols,is_exhibited,frst_reg_dt)
SELECT 1,1,cate_id,CONCAT('[DEV] ',goal_name),goal_name,photo_guide,7,v_today,DATE_ADD(v_today,INTERVAL 6 DAY),'IN_PROGRESS',goal_template_id,1,verification_scope,1,7,'N',v_now
FROM goal_template WHERE goal_template_id=1;
INSERT INTO challenge_condition (challenge_id,code,condition_type,description,required,sort_order,frst_reg_dt)
SELECT 1,code,condition_type,description,required,sort_order,v_now FROM goal_template_condition WHERE goal_template_id=1 ORDER BY sort_order,goal_template_condition_id;

INSERT INTO `schedule` (`schedule_id`, `challenge_id`, `day_no`, `schedule_date`, `start_at`, `deadline_at`, `status`, `attempt_count`, `piece_index`, `frst_reg_dt`) VALUES
(1, 1, 1, DATE_ADD(v_today, INTERVAL 0 DAY), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 0 DAY) AS DATETIME), INTERVAL 9 HOUR), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 1 DAY) AS DATETIME), INTERVAL 9 HOUR), 'OPEN', 0, 0, v_now),
(2, 1, 2, DATE_ADD(v_today, INTERVAL 1 DAY), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 1 DAY) AS DATETIME), INTERVAL 9 HOUR), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 2 DAY) AS DATETIME), INTERVAL 9 HOUR), 'UPCOMING', 0, 1, v_now),
(3, 1, 3, DATE_ADD(v_today, INTERVAL 2 DAY), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 2 DAY) AS DATETIME), INTERVAL 9 HOUR), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 3 DAY) AS DATETIME), INTERVAL 9 HOUR), 'UPCOMING', 0, 2, v_now),
(4, 1, 4, DATE_ADD(v_today, INTERVAL 3 DAY), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 3 DAY) AS DATETIME), INTERVAL 9 HOUR), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 4 DAY) AS DATETIME), INTERVAL 9 HOUR), 'UPCOMING', 0, 3, v_now),
(5, 1, 5, DATE_ADD(v_today, INTERVAL 4 DAY), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 4 DAY) AS DATETIME), INTERVAL 9 HOUR), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 5 DAY) AS DATETIME), INTERVAL 9 HOUR), 'UPCOMING', 0, 4, v_now),
(6, 1, 6, DATE_ADD(v_today, INTERVAL 5 DAY), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 5 DAY) AS DATETIME), INTERVAL 9 HOUR), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 6 DAY) AS DATETIME), INTERVAL 9 HOUR), 'UPCOMING', 0, 5, v_now),
(7, 1, 7, DATE_ADD(v_today, INTERVAL 6 DAY), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 6 DAY) AS DATETIME), INTERVAL 9 HOUR), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 7 DAY) AS DATETIME), INTERVAL 9 HOUR), 'UPCOMING', 0, 6, v_now);

-- 회원 2: 오늘 시작하는 7일 도전. 첫 회차 OPEN, 나머지 UPCOMING.
INSERT INTO challenge (challenge_id,user_id,cate_id,challenge_title,goal_text,photo_guide,duration_days,start_date,end_date,status,goal_template_id,artwork_id,verification_scope,grid_rows,grid_cols,is_exhibited,frst_reg_dt)
SELECT 2,2,cate_id,CONCAT('[DEV] ',goal_name),goal_name,photo_guide,7,v_today,DATE_ADD(v_today,INTERVAL 6 DAY),'IN_PROGRESS',goal_template_id,2,verification_scope,1,7,'N',v_now
FROM goal_template WHERE goal_template_id=2;
INSERT INTO challenge_condition (challenge_id,code,condition_type,description,required,sort_order,frst_reg_dt)
SELECT 2,code,condition_type,description,required,sort_order,v_now FROM goal_template_condition WHERE goal_template_id=2 ORDER BY sort_order,goal_template_condition_id;

INSERT INTO `schedule` (`schedule_id`, `challenge_id`, `day_no`, `schedule_date`, `start_at`, `deadline_at`, `status`, `attempt_count`, `piece_index`, `frst_reg_dt`) VALUES
(8, 2, 1, DATE_ADD(v_today, INTERVAL 0 DAY), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 0 DAY) AS DATETIME), INTERVAL 9 HOUR), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 1 DAY) AS DATETIME), INTERVAL 9 HOUR), 'OPEN', 0, 0, v_now),
(9, 2, 2, DATE_ADD(v_today, INTERVAL 1 DAY), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 1 DAY) AS DATETIME), INTERVAL 9 HOUR), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 2 DAY) AS DATETIME), INTERVAL 9 HOUR), 'UPCOMING', 0, 1, v_now),
(10, 2, 3, DATE_ADD(v_today, INTERVAL 2 DAY), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 2 DAY) AS DATETIME), INTERVAL 9 HOUR), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 3 DAY) AS DATETIME), INTERVAL 9 HOUR), 'UPCOMING', 0, 2, v_now),
(11, 2, 4, DATE_ADD(v_today, INTERVAL 3 DAY), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 3 DAY) AS DATETIME), INTERVAL 9 HOUR), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 4 DAY) AS DATETIME), INTERVAL 9 HOUR), 'UPCOMING', 0, 3, v_now),
(12, 2, 5, DATE_ADD(v_today, INTERVAL 4 DAY), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 4 DAY) AS DATETIME), INTERVAL 9 HOUR), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 5 DAY) AS DATETIME), INTERVAL 9 HOUR), 'UPCOMING', 0, 4, v_now),
(13, 2, 6, DATE_ADD(v_today, INTERVAL 5 DAY), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 5 DAY) AS DATETIME), INTERVAL 9 HOUR), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 6 DAY) AS DATETIME), INTERVAL 9 HOUR), 'UPCOMING', 0, 5, v_now),
(14, 2, 7, DATE_ADD(v_today, INTERVAL 6 DAY), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 6 DAY) AS DATETIME), INTERVAL 9 HOUR), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 7 DAY) AS DATETIME), INTERVAL 9 HOUR), 'UPCOMING', 0, 6, v_now);

-- 회원 3: 오늘 시작하는 7일 도전. 첫 회차 OPEN, 나머지 UPCOMING.
INSERT INTO challenge (challenge_id,user_id,cate_id,challenge_title,goal_text,photo_guide,duration_days,start_date,end_date,status,goal_template_id,artwork_id,verification_scope,grid_rows,grid_cols,is_exhibited,frst_reg_dt)
SELECT 3,3,cate_id,CONCAT('[DEV] ',goal_name),goal_name,photo_guide,7,v_today,DATE_ADD(v_today,INTERVAL 6 DAY),'IN_PROGRESS',goal_template_id,3,verification_scope,1,7,'N',v_now
FROM goal_template WHERE goal_template_id=3;
INSERT INTO challenge_condition (challenge_id,code,condition_type,description,required,sort_order,frst_reg_dt)
SELECT 3,code,condition_type,description,required,sort_order,v_now FROM goal_template_condition WHERE goal_template_id=3 ORDER BY sort_order,goal_template_condition_id;

INSERT INTO `schedule` (`schedule_id`, `challenge_id`, `day_no`, `schedule_date`, `start_at`, `deadline_at`, `status`, `attempt_count`, `piece_index`, `frst_reg_dt`) VALUES
(15, 3, 1, DATE_ADD(v_today, INTERVAL 0 DAY), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 0 DAY) AS DATETIME), INTERVAL 9 HOUR), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 1 DAY) AS DATETIME), INTERVAL 9 HOUR), 'OPEN', 0, 0, v_now),
(16, 3, 2, DATE_ADD(v_today, INTERVAL 1 DAY), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 1 DAY) AS DATETIME), INTERVAL 9 HOUR), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 2 DAY) AS DATETIME), INTERVAL 9 HOUR), 'UPCOMING', 0, 1, v_now),
(17, 3, 3, DATE_ADD(v_today, INTERVAL 2 DAY), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 2 DAY) AS DATETIME), INTERVAL 9 HOUR), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 3 DAY) AS DATETIME), INTERVAL 9 HOUR), 'UPCOMING', 0, 2, v_now),
(18, 3, 4, DATE_ADD(v_today, INTERVAL 3 DAY), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 3 DAY) AS DATETIME), INTERVAL 9 HOUR), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 4 DAY) AS DATETIME), INTERVAL 9 HOUR), 'UPCOMING', 0, 3, v_now),
(19, 3, 5, DATE_ADD(v_today, INTERVAL 4 DAY), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 4 DAY) AS DATETIME), INTERVAL 9 HOUR), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 5 DAY) AS DATETIME), INTERVAL 9 HOUR), 'UPCOMING', 0, 4, v_now),
(20, 3, 6, DATE_ADD(v_today, INTERVAL 5 DAY), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 5 DAY) AS DATETIME), INTERVAL 9 HOUR), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 6 DAY) AS DATETIME), INTERVAL 9 HOUR), 'UPCOMING', 0, 5, v_now),
(21, 3, 7, DATE_ADD(v_today, INTERVAL 6 DAY), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 6 DAY) AS DATETIME), INTERVAL 9 HOUR), DATE_SUB(CAST(DATE_ADD(v_today, INTERVAL 7 DAY) AS DATETIME), INTERVAL 9 HOUR), 'UPCOMING', 0, 6, v_now);


-- 도전1을 조각 표시 전용 스냅샷으로 변경합니다. 나머지 도전은 초기 상태 유지.
UPDATE challenge SET start_date=DATE_SUB(v_today, INTERVAL 2 DAY),
 end_date=DATE_ADD(v_today, INTERVAL 4 DAY),
 frst_reg_dt=DATE_SUB(CAST(DATE_SUB(v_today, INTERVAL 2 DAY) AS DATETIME), INTERVAL 9 HOUR)
WHERE challenge_id=1;
UPDATE challenge_condition SET frst_reg_dt=(SELECT frst_reg_dt FROM challenge WHERE challenge_id=1)
WHERE challenge_id=1;
UPDATE schedule SET schedule_date=DATE_SUB(schedule_date, INTERVAL 2 DAY),
 start_at=DATE_SUB(start_at, INTERVAL 2 DAY), deadline_at=DATE_SUB(deadline_at, INTERVAL 2 DAY),
 status=CASE day_no WHEN 1 THEN 'FAILED' WHEN 2 THEN 'SUCCESS' WHEN 3 THEN 'OPEN' ELSE 'UPCOMING' END,
 attempt_count=CASE WHEN day_no=2 THEN 1 ELSE 0 END,
 frst_reg_dt=(SELECT frst_reg_dt FROM challenge WHERE challenge_id=1)
WHERE challenge_id=1;
UPDATE schedule SET decided_at=deadline_at, result_reason='[DEV] 제출 없이 마감된 실패 회차', upd_dt=deadline_at
WHERE schedule_id=1;
UPDATE schedule SET decided_at=DATE_ADD(start_at, INTERVAL 12 HOUR),
 result_reason='[DEV MOCK] 조각 표시용 성공 스냅샷. 실제 AI 판정 아님',
 upd_dt=DATE_ADD(start_at, INTERVAL 12 HOUR)
WHERE schedule_id=2;

-- 회원1의 성공 회차와 연결된 합성 제출 1건(작품 이미지를 mock 입력으로 사용).
INSERT INTO media (media_id,image_key,file_hash,content_type,file_size,status,user_id,purpose,frst_reg_dt)
SELECT 2,'artworks/알고리즘 푸는 최후의 만찬.png','19e15eda81e73aab876b98ccbaceb714d5efd8a882935180950b2556d67a14af',
 'image/png',2849563,'SUCCESS',1,'VERIFICATION',DATE_SUB(decided_at, INTERVAL 1 MINUTE)
FROM schedule WHERE schedule_id=2;
INSERT INTO verification (verification_id,schedule_id,attempt_no,idempotency_key,file_hash,status,
 submitted_at,analyzed_at,reason,model_version,frst_reg_dt,upd_dt,media_id)
SELECT 1,2,1,'dev-seed-ui-schedule-2-attempt-1',m.file_hash,'SUCCESS',
 DATE_SUB(s.decided_at, INTERVAL 1 MINUTE),s.decided_at,
 '[DEV MOCK] 화면 검증용 합성 성공. 실제 AI 판정 아님','seed-ui-mock',
 DATE_SUB(s.decided_at, INTERVAL 1 MINUTE),s.decided_at,2
FROM schedule s JOIN media m ON m.media_id=2 WHERE s.schedule_id=2;
-- 성공 시도의 모든 조건 결과를 생성합니다. 조건 개수가 늘어도 결과가 누락되지 않습니다.
INSERT INTO verification_check (verification_id,challenge_condition_id,status,reason,frst_reg_dt)
SELECT 1,cc.challenge_condition_id,'MET',
 CONCAT('[DEV MOCK] ',cc.code, ': 성공 조각 표시를 위한 합성 조건 결과'),s.decided_at
FROM schedule s JOIN challenge_condition cc ON cc.challenge_id=s.challenge_id
WHERE s.schedule_id=2
ORDER BY cc.sort_order,cc.challenge_condition_id;

-- 회원2/3의 verification / verification_check / ai_task는 실제 인증 API로 생성합니다.
-- user_social_account에는 실제 제공자 ID를 임의로 만들지 않습니다.
 COMMIT;
END$$
DELIMITER ;
CALL moamu_seed_dev_20261023();
DROP PROCEDURE moamu_seed_dev_20261023;

SELECT 'users' AS table_name, COUNT(*) AS row_count FROM users
UNION ALL SELECT 'media',COUNT(*) FROM media
UNION ALL SELECT 'goal_template',COUNT(*) FROM goal_template
UNION ALL SELECT 'artwork',COUNT(*) FROM artwork
UNION ALL SELECT 'challenge',COUNT(*) FROM challenge
UNION ALL SELECT 'schedule',COUNT(*) FROM schedule;

-- 기대 결과: 도전1 공개1/실패1/미확정5, 도전2·3 공개0/실패0/미확정7.
SELECT challenge_id,
 SUM(status='SUCCESS') AS revealed_pieces,
 SUM(status='FAILED') AS failed_pieces,
 SUM(status IN ('OPEN','UPCOMING','PROCESSING','PENDING_SETTLEMENT')) AS undecided_pieces
FROM schedule GROUP BY challenge_id;
SELECT s.challenge_id,c.artwork_id,a.image_key,s.schedule_id,s.day_no,s.piece_index,s.status,s.decided_at
FROM schedule s JOIN challenge c ON c.challenge_id=s.challenge_id
JOIN artwork a ON a.artwork_id=c.artwork_id
ORDER BY s.challenge_id,s.piece_index;

-- 조건 목록 기대 개수: 도전1=3, 도전2=1, 도전3=5. sort_order 순으로 화면에 표시합니다.
SELECT challenge_id,COUNT(*) AS condition_count,
 SUM(required='Y') AS required_count,SUM(required='N') AS optional_count
FROM challenge_condition GROUP BY challenge_id;
SELECT challenge_id,challenge_condition_id,code,condition_type,description,required,sort_order
FROM challenge_condition ORDER BY challenge_id,sort_order;
-- 가상 성공 인증1: 조건 3개, 판정 3개, 모두 MET.
SELECT v.verification_id,v.status,COUNT(vc.verification_check_id) AS check_count,
 SUM(vc.status='MET') AS met_count
FROM verification v JOIN verification_check vc ON vc.verification_id=v.verification_id
GROUP BY v.verification_id,v.status;