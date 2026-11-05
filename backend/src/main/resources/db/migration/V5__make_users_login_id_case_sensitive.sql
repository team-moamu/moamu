-- 로그인 이메일의 대소문자를 구분
ALTER TABLE `users`
MODIFY COLUMN `login_id`
    VARCHAR(50)
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_as_cs
    NULL DEFAULT NULL
    COMMENT '일반 로그인 아이디 (소셜 전용 회원은 NULL)';