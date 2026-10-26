-- MOAMU 로컬 개발 전용: 반드시 V1 실행 후 이 파일 전체를 실행하세요.

SET NAMES utf8mb4;

SELECT DATABASE() AS selected_database;

DROP PROCEDURE IF EXISTS moamu_dev_v2_image_keys_20261025;
DELIMITER $$
CREATE PROCEDURE moamu_dev_v2_image_keys_20261025()
SQL SECURITY INVOKER
BEGIN
    DECLARE v_expected INT DEFAULT 0;
    DECLARE v_changed_artwork INT DEFAULT 0;
    DECLARE v_changed_media INT DEFAULT 0;
    DECLARE v_verified INT DEFAULT 0;
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
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '대상 MOAMU 개발 DB를 먼저 선택하세요.';
    END IF;
    -- 트랜잭션을 지원하지 않는 테이블이나 의도하지 않은 자동 변경이 있으면 중단합니다.
    IF (SELECT COUNT(*) FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name IN ('artwork','media')
          AND table_type = 'BASE TABLE' AND engine = 'InnoDB') <> 2 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'artwork와 media는 InnoDB 테이블이어야 합니다. 변경을 중단합니다.';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.triggers
        WHERE event_object_schema = DATABASE()
          AND event_object_table IN ('artwork','media')
          AND event_manipulation = 'UPDATE') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'UPDATE 트리거가 있습니다. 자동 변경 여부를 먼저 확인하세요.';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name IN ('artwork','media')
          AND (LOWER(extra) LIKE '%on update%' OR generation_expression <> '')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '자동 갱신 또는 생성 컬럼이 있습니다. 자동 변경 여부를 먼저 확인하세요.';
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
    -- 수정 전 경로를 조회하고, 검증·수정이 끝날 때까지 대상 행을 잠급니다.
    SELECT artwork_id, image_key FROM artwork
    WHERE artwork_id IN (1,2,3,4,5,6) ORDER BY artwork_id FOR UPDATE;
    SELECT media_id, image_key FROM media
    WHERE media_id IN (1,2) ORDER BY media_id FOR UPDATE;

    -- V1의 대상 8개 행인지 확인합니다. 기존 경로 또는 변경 완료된 경로만 허용합니다.
    IF (SELECT COUNT(*) FROM artwork a
        JOIN tmp_moamu_dev_v2_image_keys_20261025 k
          ON k.entity_kind = 'artwork' AND k.entity_id = a.artwork_id
        WHERE CAST(a.artwork_title AS BINARY) = CAST(CONCAT('개발용 작품 ', a.artwork_id) AS BINARY)
          AND CAST(a.image_key AS BINARY) IN (CAST(k.old_key AS BINARY), CAST(k.new_key AS BINARY))) <> 6 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V1 작품 데이터 또는 이미지 경로가 예상과 다릅니다. 변경을 취소합니다.';
    END IF;
    IF (SELECT COUNT(*) FROM media m
        JOIN tmp_moamu_dev_v2_image_keys_20261025 k
          ON k.entity_kind = 'media' AND k.entity_id = m.media_id
        WHERE CAST(m.image_key AS BINARY) IN (CAST(k.old_key AS BINARY), CAST(k.new_key AS BINARY))
          AND CAST(m.content_type AS BINARY) = CAST('image/png' AS BINARY)
          AND ((m.media_id = 1 AND m.user_id IS NULL
                AND CAST(m.purpose AS BINARY) = CAST('PROFILE' AS BINARY)
                AND m.file_size = 1502568
                AND CAST(m.file_hash AS BINARY) = CAST('05bfda986db50f1418719a5f129f4d764a462b74f5b005d3339563c8c5672eb4' AS BINARY))
            OR (m.media_id = 2 AND m.user_id = 1
                AND CAST(m.purpose AS BINARY) = CAST('VERIFICATION' AS BINARY)
                AND m.file_size = 2849563
                AND CAST(m.file_hash AS BINARY) = CAST('19e15eda81e73aab876b98ccbaceb714d5efd8a882935180950b2556d67a14af' AS BINARY)))) <> 2 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V1 대상 데이터 또는 이미지 경로가 예상과 다릅니다. 변경을 취소합니다.';
    END IF;

    SELECT COUNT(*) INTO v_expected
    FROM tmp_moamu_dev_v2_image_keys_20261025 k
    LEFT JOIN artwork a ON k.entity_kind = 'artwork' AND a.artwork_id = k.entity_id
    LEFT JOIN media m ON k.entity_kind = 'media' AND m.media_id = k.entity_id
    WHERE CAST(CASE WHEN k.entity_kind = 'artwork' THEN a.image_key ELSE m.image_key END AS BINARY)
          = CAST(k.old_key AS BINARY);

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
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '변경 결과 검증에 실패했습니다. 전체 변경을 취소합니다.';
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
