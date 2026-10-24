-- MOAMU 로컬 개발 전용. V1 실행 후 변경된 이미지 파일명을 반영하는 추가 수정입니다.
-- backend/data/seedData/V2_seed-dev.sql에 저장하세요. Flyway 마이그레이션 폴더에는 넣지 마세요.
-- 새로운 전용 DB 연결에서 mysql 명령줄 도구로 이 파일 전체를 실행하세요.
-- 대상 개발 DB를 선택하고 백업한 뒤, 백엔드와 DB에 데이터를 쓰는 다른 작업을 먼저 중지하세요.
-- V1 재실행, 데이터 초기화, --force 옵션 사용, 기존 트랜잭션 안에서의 실행은 금지합니다.
-- 필요한 권한: 루틴 생성(CREATE ROUTINE), 루틴 변경(ALTER ROUTINE), 실행(EXECUTE),
-- 임시 테이블 생성(CREATE TEMPORARY TABLES), 조회(SELECT), 수정(UPDATE).
-- 모든 트리거 메타데이터를 조회할 수 있는 스키마 소유자 개발 계정을 사용하세요(TRIGGER 권한 필요).
-- image_key 값 8개만 수정하며, 이미 새 경로로 변경된 값은 정상으로 인정하고 건너뜁니다.
-- 파일명 변경은 파일 내용에 영향을 주지 않으므로 해시, 파일 크기, 모든 연결 관계를 그대로 유지합니다.
-- 이미지 키는 backend/data 기준 상대 경로입니다. seedData는 SQL 파일을 보관하는 폴더일 뿐입니다.
-- media 2번은 artwork 1번 이미지를 재사용합니다. 존재하지 않는 verification/* 경로를 만들지 않습니다.
-- SQL은 디스크 파일을 확인하거나 복사하거나 이름을 바꾸지 않습니다. 정확한 이름의 파일을 먼저 준비하세요.
-- 보조 프로시저의 생성·삭제는 데이터 변경 트랜잭션 밖에서 실행됩니다. 호출(CALL)에 실패하면
-- 이 파일 전용 보조 프로시저가 남을 수 있으며, 파일을 재실행하면 해당 프로시저만 교체합니다.

SET NAMES utf8mb4;

SELECT DATABASE() AS selected_database, @@hostname AS mysql_host,
       @@port AS mysql_port;

DROP PROCEDURE IF EXISTS moamu_dev_v2_image_keys_20261025;
DELIMITER $$
CREATE PROCEDURE moamu_dev_v2_image_keys_20261025()
SQL SECURITY INVOKER
BEGIN
    DECLARE v_done BOOLEAN DEFAULT FALSE;
    DECLARE v_found BOOLEAN DEFAULT TRUE;
    DECLARE v_kind VARCHAR(16);
    DECLARE v_id BIGINT;
    DECLARE v_old VARCHAR(255) CHARACTER SET utf8mb4;
    DECLARE v_new VARCHAR(255) CHARACTER SET utf8mb4;
    DECLARE v_current TEXT CHARACTER SET utf8mb4;
    DECLARE v_identity BOOLEAN;
    DECLARE v_expected INT DEFAULT 0;
    DECLARE v_changed_artwork INT DEFAULT 0;
    DECLARE v_changed_media INT DEFAULT 0;
    DECLARE v_verified INT DEFAULT 0;
    DECLARE v_message VARCHAR(128);
    DECLARE c_keys CURSOR FOR
        SELECT entity_kind, entity_id, old_key, new_key
        FROM tmp_moamu_dev_v2_image_keys_20261025
        ORDER BY entity_kind, entity_id;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = TRUE;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        DROP TEMPORARY TABLE IF EXISTS tmp_moamu_dev_v2_image_keys_20261025;
        RESIGNAL;
    END;
    DECLARE EXIT HANDLER FOR SQLWARNING
    BEGIN
        ROLLBACK;
        DROP TEMPORARY TABLE IF EXISTS tmp_moamu_dev_v2_image_keys_20261025;
        RESIGNAL;
    END;

    IF DATABASE() IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Select the intended MOAMU DEV database first.';
    END IF;
    -- 트랜잭션을 지원하지 않는 테이블이나 의도하지 않은 자동 변경이 있으면 중단합니다.
    IF (SELECT COUNT(*) FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name IN ('artwork','media')
          AND table_type = 'BASE TABLE' AND engine = 'InnoDB') <> 2 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Both artwork and media must be InnoDB base tables; no data changed.';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.triggers
        WHERE event_object_schema = DATABASE()
          AND event_object_table IN ('artwork','media')
          AND event_manipulation = 'UPDATE') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'UPDATE triggers require review before this image-only patch; no data changed.';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name IN ('artwork','media')
          AND (LOWER(extra) LIKE '%on update%' OR generation_expression <> '')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Automatic/generated columns require review before this image-only patch; no data changed.';
    END IF;

    DROP TEMPORARY TABLE IF EXISTS tmp_moamu_dev_v2_image_keys_20261025;
    CREATE TEMPORARY TABLE tmp_moamu_dev_v2_image_keys_20261025 (
        entity_kind VARCHAR(16) NOT NULL,
        entity_id BIGINT NOT NULL,
        old_key VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
        new_key VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
        PRIMARY KEY (entity_kind, entity_id)
    ) ENGINE = InnoDB;
    INSERT INTO tmp_moamu_dev_v2_image_keys_20261025 VALUES
    ('artwork', 1, 'artworks/알고리즘 푸는 최후의 만찬.png', 'artworks/last_supper_solving_algorithms.png'),
    ('artwork', 2, 'artworks/명찰 챙긴 모나리자.png', 'artworks/mona_lisa_with_name_tag.png'),
    ('artwork', 3, 'artworks/공부하는 진주귀걸이를 한 소녀.png', 'artworks/girl_with_pearl_earring_studying.png'),
    ('artwork', 4, 'artworks/계단 오르는 피리부는 소년.png', 'artworks/flute_boy_climbing_stairs.png'),
    ('artwork', 5, 'artworks/물마시는 뭉크.png', 'artworks/munch_drinking_water.png'),
    ('artwork', 6, 'artworks/영양제 먹는 이삭줍는 여인들.png', 'artworks/gleaners_taking_supplements.png'),
    ('media', 1, 'uploads/기본 프로필 뮤.png', 'uploads/default_profile_mu.png'),
    ('media', 2, 'artworks/알고리즘 푸는 최후의 만찬.png', 'artworks/last_supper_solving_algorithms.png');

    START TRANSACTION;
    OPEN c_keys;
    validate_loop: LOOP
        FETCH c_keys INTO v_kind, v_id, v_old, v_new;
        IF v_done THEN
            LEAVE validate_loop;
        END IF;
        SET v_found = TRUE;
        SET v_identity = FALSE;
        SET v_current = NULL;
        BEGIN
            DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_found = FALSE;
            IF v_kind = 'artwork' THEN
                SELECT image_key,
                    CAST(artwork_title AS BINARY) = CAST(CONCAT('개발용 작품 ', v_id) AS BINARY)
                INTO v_current, v_identity
                FROM artwork WHERE artwork_id = v_id FOR UPDATE;
            ELSEIF v_id = 1 THEN
                SELECT image_key,
                    (user_id IS NULL
                     AND CAST(purpose AS BINARY) = CAST('PROFILE' AS BINARY)
                     AND CAST(content_type AS BINARY) = CAST('image/png' AS BINARY)
                     AND file_size = 1502568
                     AND CAST(file_hash AS BINARY) = CAST('05bfda986db50f1418719a5f129f4d764a462b74f5b005d3339563c8c5672eb4' AS BINARY))
                INTO v_current, v_identity
                FROM media WHERE media_id = v_id FOR UPDATE;
            ELSE
                SELECT image_key,
                    (user_id = 1
                     AND CAST(purpose AS BINARY) = CAST('VERIFICATION' AS BINARY)
                     AND CAST(content_type AS BINARY) = CAST('image/png' AS BINARY)
                     AND file_size = 2849563
                     AND CAST(file_hash AS BINARY) = CAST('19e15eda81e73aab876b98ccbaceb714d5efd8a882935180950b2556d67a14af' AS BINARY))
                INTO v_current, v_identity
                FROM media WHERE media_id = v_id FOR UPDATE;
            END IF;
        END;
        IF NOT v_found OR NOT COALESCE(v_identity, FALSE)
           OR v_current IS NULL
           OR (CAST(v_current AS BINARY) <> CAST(v_old AS BINARY)
               AND CAST(v_current AS BINARY) <> CAST(v_new AS BINARY)) THEN
            SET v_message = CONCAT('V2 mismatch: ', v_kind, ' ID ', v_id,
                ' missing, identity changed, or unexpected image_key. All updates rolled back.');
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_message;
        END IF;
        IF CAST(v_current AS BINARY) = CAST(v_old AS BINARY) THEN
            SET v_expected = v_expected + 1;
        END IF;
    END LOOP;
    CLOSE c_keys;

    -- 8개 행 모두 검증을 통과했습니다. 변경 확정(COMMIT)까지 해당 행의 잠금을 유지합니다.
    UPDATE artwork a
    JOIN tmp_moamu_dev_v2_image_keys_20261025 k
      ON k.entity_kind = 'artwork' AND k.entity_id = a.artwork_id
    SET a.image_key = k.new_key
    WHERE CAST(a.image_key AS BINARY) = CAST(k.old_key AS BINARY);
    SET v_changed_artwork = ROW_COUNT();

    UPDATE media m
    JOIN tmp_moamu_dev_v2_image_keys_20261025 k
      ON k.entity_kind = 'media' AND k.entity_id = m.media_id
    SET m.image_key = k.new_key
    WHERE CAST(m.image_key AS BINARY) = CAST(k.old_key AS BINARY);
    SET v_changed_media = ROW_COUNT();

    SELECT COUNT(*) INTO v_verified
    FROM tmp_moamu_dev_v2_image_keys_20261025 k
    LEFT JOIN artwork a ON k.entity_kind = 'artwork' AND a.artwork_id = k.entity_id
    LEFT JOIN media m ON k.entity_kind = 'media' AND m.media_id = k.entity_id
    WHERE CAST(CASE WHEN k.entity_kind = 'artwork' THEN a.image_key ELSE m.image_key END AS BINARY)
          = CAST(k.new_key AS BINARY);

    IF v_changed_artwork + v_changed_media <> v_expected OR v_verified <> 8 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V2 post-update verification failed. All updates rolled back.';
    END IF;
    COMMIT;

    SELECT 'COMMITTED' AS result, DATABASE() AS database_name,
           v_changed_artwork AS artwork_rows_changed,
           v_changed_media AS media_rows_changed, v_verified AS verified_keys;

    -- 정확히 8개 행이 조회되어야 하며, 모든 check_result 값이 OK여야 합니다.
    SELECT k.entity_kind, k.entity_id, k.new_key AS expected_image_key,
           CASE WHEN k.entity_kind = 'artwork' THEN a.image_key ELSE m.image_key END AS actual_image_key,
           IF(CAST(CASE WHEN k.entity_kind = 'artwork' THEN a.image_key ELSE m.image_key END AS BINARY)
              = CAST(k.new_key AS BINARY), 'OK', 'MISMATCH') AS check_result
    FROM tmp_moamu_dev_v2_image_keys_20261025 k
    LEFT JOIN artwork a ON k.entity_kind = 'artwork' AND a.artwork_id = k.entity_id
    LEFT JOIN media m ON k.entity_kind = 'media' AND m.media_id = k.entity_id
    ORDER BY k.entity_kind, k.entity_id;
    DROP TEMPORARY TABLE tmp_moamu_dev_v2_image_keys_20261025;
END$$
DELIMITER ;

CALL moamu_dev_v2_image_keys_20261025();
DROP PROCEDURE moamu_dev_v2_image_keys_20261025;

-- 읽기 전용 검증 조회입니다. 현재 연결을 종료한 뒤 새 연결에서 별도로 실행해도 됩니다.
SELECT artwork_id, artwork_title, image_key
FROM artwork WHERE artwork_id IN (1,2,3,4,5,6) ORDER BY artwork_id;
SELECT media_id, image_key, purpose, user_id, file_hash, file_size
FROM media WHERE media_id IN (1,2) ORDER BY media_id;
-- 시드로 생성한 모의 인증 기록이 여전히 media 2번을 참조하는지 확인합니다.
SELECT v.verification_id, v.schedule_id, v.media_id, m.image_key
FROM verification v LEFT JOIN media m ON m.media_id = v.media_id
WHERE v.verification_id = 1;
