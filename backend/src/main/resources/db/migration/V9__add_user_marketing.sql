-- 마케팅 정보 컬럼 추가
ALTER TABLE `users`
    ADD COLUMN `marketing_agreed` BOOLEAN NOT NULL DEFAULT FALSE
        COMMENT '마케팅 정보 수신 동의 여부',
    ADD COLUMN `marketing_agreed_at` DATETIME NULL
        COMMENT '마케팅 정보 수신 동의 일시';