-- MOAMU service catalog seed / MySQL 8 / Flyway V1-V6
-- Run the entire file with application writes stopped. See README.md.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS moamu_seed_service_v6;
DELIMITER $$
CREATE PROCEDURE moamu_seed_service_v6()
SQL SECURITY INVOKER
BEGIN
 DECLARE v_now DATETIME;
 DECLARE v_category BIGINT;
 DECLARE v_template BIGINT;
 DECLARE v_media BIGINT;
 DECLARE v_artwork BIGINT;
 DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
 DECLARE EXIT HANDLER FOR SQLWARNING BEGIN ROLLBACK; RESIGNAL; END;

 IF DATABASE() IS NULL THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Select the target database first.';
 END IF;
 IF (SELECT COUNT(*) FROM flyway_schema_history
     WHERE version IN ('1','2','3','4','5','6') AND success = 1) <> 6 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Apply Flyway V1-V6 before seeding.';
 END IF;
 IF (SELECT COUNT(*) FROM information_schema.tables
     WHERE table_schema = DATABASE() AND engine = 'InnoDB'
       AND table_name IN ('goal_category','goal_template','goal_template_condition','media','artwork','goal_template_artwork')) <> 6
    OR (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'artwork' AND column_name = 'media_id') <> 1
    OR EXISTS (SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND ((table_name = 'goal_template_condition' AND column_name IN ('condition_type','required','sort_order'))
         OR (table_name = 'challenge_condition' AND column_name IN ('condition_type','required','sort_order'))
         OR (table_name = 'schedule' AND column_name IN ('start_at','deadline_at'))
         OR (table_name = 'media' AND column_name = 'status'))) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Schema mismatch: expected current V6 schema.';
 END IF;

 SET v_now = UTC_TIMESTAMP();
 START TRANSACTION;
 IF EXISTS (SELECT 1 FROM goal_category)
    OR EXISTS (SELECT 1 FROM goal_template)
    OR EXISTS (SELECT 1 FROM goal_template_condition)
    OR EXISTS (SELECT 1 FROM artwork)
    OR EXISTS (SELECT 1 FROM goal_template_artwork) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Catalog must be empty. Existing data was not changed.';
 END IF;
 IF EXISTS (SELECT 1 FROM media WHERE image_key IN (
  'uploads/default_profile_mu.png',
  'artworks/last_supper_solving_algorithms.png',
  'artworks/mona_lisa_with_name_tag.png',
  'artworks/girl_with_pearl_earring_studying.png',
  'artworks/flute_boy_climbing_stairs.png',
  'artworks/munch_drinking_water.png',
  'artworks/gleaners_taking_supplements.png')) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Seed image keys already exist. Inspect media before seeding.';
 END IF;

 INSERT INTO media (image_key, content_type, user_id, purpose, frst_reg_dt)
 VALUES ('uploads/default_profile_mu.png', 'image/png', NULL, 'PROFILE', v_now);

 -- 1. 알고리즘
 INSERT INTO goal_category (cate_code, cate_name, cate_source, frst_reg_dt)
 VALUES ('ALGORITHM', '알고리즘', 'SYSTEM', v_now);
 SET v_category = LAST_INSERT_ID();
 INSERT INTO goal_template (cate_id, goal_name, description, photo_guide, verification_scope, status, sort_order, frst_reg_dt)
 VALUES (v_category, '알고리즘 문제 풀이 기록하기', '문제를 풀고 코드와 실행 결과를 기록해 보세요.', '문제, 풀이 코드와 실행 또는 채점 결과가 함께 보이도록 촬영해 주세요.', '사진에 보이는 대상과 기록만 확인합니다. 실제 수행 여부, 수행 시간, 이동 거리, 섭취 여부와 섭취량은 사진 한 장으로 확인할 수 없습니다.', 'ACTIVE', 1, v_now);
 SET v_template = LAST_INSERT_ID();
 INSERT INTO goal_template_condition (goal_template_id, code, description, frst_reg_dt) VALUES
 (v_template, 'C1', '문제와 풀이 코드가 사진에서 식별되어야 합니다.', v_now),
 (v_template, 'C2', '코드 실행 결과 또는 채점 결과가 사진에 보여야 합니다.', v_now);
 INSERT INTO media (image_key, content_type, user_id, purpose, frst_reg_dt)
 VALUES ('artworks/last_supper_solving_algorithms.png', 'image/png', NULL, 'VERIFICATION', v_now);
 SET v_media = LAST_INSERT_ID();
 INSERT INTO artwork (status, artwork_title, media_id, completed_at, frst_reg_dt)
 VALUES ('READY', '최후의 만찬, 알고리즘을 풀다', v_media, v_now, v_now);
 SET v_artwork = LAST_INSERT_ID();
 INSERT INTO goal_template_artwork (goal_template_id, artwork_id, frst_reg_dt)
 VALUES (v_template, v_artwork, v_now);

 -- 2. 출근
 INSERT INTO goal_category (cate_code, cate_name, cate_source, frst_reg_dt)
 VALUES ('WORK', '출근', 'SYSTEM', v_now);
 SET v_category = LAST_INSERT_ID();
 INSERT INTO goal_template (cate_id, goal_name, description, photo_guide, verification_scope, status, sort_order, frst_reg_dt)
 VALUES (v_category, '명찰 착용 기록하기', '명찰을 착용한 모습을 기록해 보세요.', '이름, 회사명 등 개인정보를 가리고 착용한 명찰이 보이도록 촬영해 주세요.', '사진에 보이는 대상과 기록만 확인합니다. 실제 수행 여부, 수행 시간, 이동 거리, 섭취 여부와 섭취량은 사진 한 장으로 확인할 수 없습니다.', 'ACTIVE', 2, v_now);
 SET v_template = LAST_INSERT_ID();
 INSERT INTO goal_template_condition (goal_template_id, code, description, frst_reg_dt) VALUES
 (v_template, 'C1', '착용한 명찰이 사진에 보여야 합니다.', v_now);
 INSERT INTO media (image_key, content_type, user_id, purpose, frst_reg_dt)
 VALUES ('artworks/mona_lisa_with_name_tag.png', 'image/png', NULL, 'VERIFICATION', v_now);
 SET v_media = LAST_INSERT_ID();
 INSERT INTO artwork (status, artwork_title, media_id, completed_at, frst_reg_dt)
 VALUES ('READY', '명찰을 단 모나리자', v_media, v_now, v_now);
 SET v_artwork = LAST_INSERT_ID();
 INSERT INTO goal_template_artwork (goal_template_id, artwork_id, frst_reg_dt)
 VALUES (v_template, v_artwork, v_now);

 -- 3. 학습
 INSERT INTO goal_category (cate_code, cate_name, cate_source, frst_reg_dt)
 VALUES ('STUDY', '학습', 'SYSTEM', v_now);
 SET v_category = LAST_INSERT_ID();
 INSERT INTO goal_template (cate_id, goal_name, description, photo_guide, verification_scope, status, sort_order, frst_reg_dt)
 VALUES (v_category, '학습 기록하기', '공부한 내용을 메모나 문제 풀이로 남겨 보세요.', '학습 자료와 직접 작성한 메모, 요약 또는 문제 풀이 결과가 함께 보이도록 촬영해 주세요.', '사진에 보이는 대상과 기록만 확인합니다. 실제 수행 여부, 수행 시간, 이동 거리, 섭취 여부와 섭취량은 사진 한 장으로 확인할 수 없습니다.', 'ACTIVE', 3, v_now);
 SET v_template = LAST_INSERT_ID();
 INSERT INTO goal_template_condition (goal_template_id, code, description, frst_reg_dt) VALUES
 (v_template, 'C1', '책 또는 학습 자료가 사진에 보여야 합니다.', v_now),
 (v_template, 'C2', '학습 내용과 관련하여 직접 작성한 메모, 요약 또는 문제 풀이 결과가 사진에서 식별되어야 합니다.', v_now);
 INSERT INTO media (image_key, content_type, user_id, purpose, frst_reg_dt)
 VALUES ('artworks/girl_with_pearl_earring_studying.png', 'image/png', NULL, 'VERIFICATION', v_now);
 SET v_media = LAST_INSERT_ID();
 INSERT INTO artwork (status, artwork_title, media_id, completed_at, frst_reg_dt)
 VALUES ('READY', '공부하는 진주 귀걸이 소녀', v_media, v_now, v_now);
 SET v_artwork = LAST_INSERT_ID();
 INSERT INTO goal_template_artwork (goal_template_id, artwork_id, frst_reg_dt)
 VALUES (v_template, v_artwork, v_now);

 -- 4. 운동
 INSERT INTO goal_category (cate_code, cate_name, cate_source, frst_reg_dt)
 VALUES ('EXERCISE', '운동', 'SYSTEM', v_now);
 SET v_category = LAST_INSERT_ID();
 INSERT INTO goal_template (cate_id, goal_name, description, photo_guide, verification_scope, status, sort_order, frst_reg_dt)
 VALUES (v_category, '계단 이용 기록하기', '일상에서 계단을 이용하고 사진으로 기록해 보세요.', '이동을 멈춘 뒤 안전한 위치에서 계단이 보이도록 촬영해 주세요.', '사진에 보이는 대상과 기록만 확인합니다. 실제 수행 여부, 수행 시간, 이동 거리, 섭취 여부와 섭취량은 사진 한 장으로 확인할 수 없습니다.', 'ACTIVE', 4, v_now);
 SET v_template = LAST_INSERT_ID();
 INSERT INTO goal_template_condition (goal_template_id, code, description, frst_reg_dt) VALUES
 (v_template, 'C1', '계단이 사진에서 식별되어야 합니다.', v_now);
 INSERT INTO media (image_key, content_type, user_id, purpose, frst_reg_dt)
 VALUES ('artworks/flute_boy_climbing_stairs.png', 'image/png', NULL, 'VERIFICATION', v_now);
 SET v_media = LAST_INSERT_ID();
 INSERT INTO artwork (status, artwork_title, media_id, completed_at, frst_reg_dt)
 VALUES ('READY', '계단을 오르는 피리 부는 소년', v_media, v_now, v_now);
 SET v_artwork = LAST_INSERT_ID();
 INSERT INTO goal_template_artwork (goal_template_id, artwork_id, frst_reg_dt)
 VALUES (v_template, v_artwork, v_now);

 -- 5. 수분 섭취
 INSERT INTO goal_category (cate_code, cate_name, cate_source, frst_reg_dt)
 VALUES ('WATER', '수분 섭취', 'SYSTEM', v_now);
 SET v_category = LAST_INSERT_ID();
 INSERT INTO goal_template (cate_id, goal_name, description, photo_guide, verification_scope, status, sort_order, frst_reg_dt)
 VALUES (v_category, '물 마시기 기록하기', '물 마시기 습관을 사진으로 기록해 보세요.', '물이 담긴 컵 또는 물병이 보이도록 촬영해 주세요.', '사진에 보이는 대상과 기록만 확인합니다. 실제 수행 여부, 수행 시간, 이동 거리, 섭취 여부와 섭취량은 사진 한 장으로 확인할 수 없습니다.', 'ACTIVE', 5, v_now);
 SET v_template = LAST_INSERT_ID();
 INSERT INTO goal_template_condition (goal_template_id, code, description, frst_reg_dt) VALUES
 (v_template, 'C1', '물이 담긴 컵 또는 물병이 사진에 보여야 합니다.', v_now);
 INSERT INTO media (image_key, content_type, user_id, purpose, frst_reg_dt)
 VALUES ('artworks/munch_drinking_water.png', 'image/png', NULL, 'VERIFICATION', v_now);
 SET v_media = LAST_INSERT_ID();
 INSERT INTO artwork (status, artwork_title, media_id, completed_at, frst_reg_dt)
 VALUES ('READY', '물을 마시는 절규', v_media, v_now, v_now);
 SET v_artwork = LAST_INSERT_ID();
 INSERT INTO goal_template_artwork (goal_template_id, artwork_id, frst_reg_dt)
 VALUES (v_template, v_artwork, v_now);

 -- 6. 영양제
 INSERT INTO goal_category (cate_code, cate_name, cate_source, frst_reg_dt)
 VALUES ('SUPPLEMENT', '영양제', 'SYSTEM', v_now);
 SET v_category = LAST_INSERT_ID();
 INSERT INTO goal_template (cate_id, goal_name, description, photo_guide, verification_scope, status, sort_order, frst_reg_dt)
 VALUES (v_category, '영양제 챙기기 기록하기', '영양제를 챙긴 모습을 사진으로 기록해 보세요.', '개인정보를 가리고 영양제 용기 또는 포장이 보이도록 촬영해 주세요.', '사진에 보이는 대상과 기록만 확인합니다. 실제 수행 여부, 수행 시간, 이동 거리, 섭취 여부와 섭취량은 사진 한 장으로 확인할 수 없습니다.', 'ACTIVE', 6, v_now);
 SET v_template = LAST_INSERT_ID();
 INSERT INTO goal_template_condition (goal_template_id, code, description, frst_reg_dt) VALUES
 (v_template, 'C1', '영양제 용기 또는 포장이 사진에서 식별되어야 합니다.', v_now);
 INSERT INTO media (image_key, content_type, user_id, purpose, frst_reg_dt)
 VALUES ('artworks/gleaners_taking_supplements.png', 'image/png', NULL, 'VERIFICATION', v_now);
 SET v_media = LAST_INSERT_ID();
 INSERT INTO artwork (status, artwork_title, media_id, completed_at, frst_reg_dt)
 VALUES ('READY', '영양제를 챙기는 이삭 줍는 사람들', v_media, v_now, v_now);
 SET v_artwork = LAST_INSERT_ID();
 INSERT INTO goal_template_artwork (goal_template_id, artwork_id, frst_reg_dt)
 VALUES (v_template, v_artwork, v_now);

 COMMIT;
END$$
DELIMITER ;
CALL moamu_seed_service_v6();
DROP PROCEDURE moamu_seed_service_v6;

SELECT t.goal_template_id, c.cate_code, t.goal_name, t.status,
       a.artwork_title, m.image_key,
       (SELECT COUNT(*) FROM goal_template_condition tc
        WHERE tc.goal_template_id = t.goal_template_id) AS condition_count
FROM goal_template t
JOIN goal_category c ON c.cate_id = t.cate_id
JOIN goal_template_artwork ta ON ta.goal_template_id = t.goal_template_id
JOIN artwork a ON a.artwork_id = ta.artwork_id
JOIN media m ON m.media_id = a.media_id
ORDER BY t.sort_order;
