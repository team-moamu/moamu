-- MOAMU initial database schema
-- Flyway migration: V1__create_initial_schema.sql
-- Source: moamu(5).sql
-- Target: MySQL 8.x / InnoDB
-- Changes from source: define primary keys inside CREATE TABLE so AUTO_INCREMENT is valid.
-- Retained: 14 tables, 19 foreign keys, 11 unique constraints; no CHECK constraints.
-- Apply to an empty schema. Do not rerun manually after Flyway has applied it.

CREATE TABLE `goal_category` (
	`cate_id`	BIGINT	NOT NULL AUTO_INCREMENT	COMMENT '카테고리 ID',
	`cate_code`	VARCHAR(50)	NOT NULL	COMMENT '카테고리 코드 (예: MUSIC)',
	`cate_name`	VARCHAR(50)	NOT NULL	COMMENT '표시명 (예: 악기)',
	`cate_source`	VARCHAR(20)	NOT NULL	DEFAULT 'SYSTEM'	COMMENT '생성 출처 (SYSTEM/AI_GENERATED, 1차는 SYSTEM)',
	`default_conditions`	TEXT	NULL	COMMENT '분석 장애 대체 경로용 기본 조건 (원본 JSON, ERD 표시용 TEXT)',
	`del_yn`	CHAR(1)	NOT NULL	DEFAULT 'N'	COMMENT '삭제 여부 (Y/N)',
	`frst_reg_dt`	DATETIME	NOT NULL	COMMENT '등록 일시',
	`upd_dt`	DATETIME	NULL	COMMENT '수정 일시',
	CONSTRAINT `PK_GOAL_CATEGORY` PRIMARY KEY (`cate_id`)
);

CREATE TABLE `goal_template_condition` (
	`goal_template_condition_id`	BIGINT	NOT NULL AUTO_INCREMENT	COMMENT '목표 템플릿 인증 조건 ID',
	`goal_template_id`	BIGINT	NOT NULL	COMMENT '목표 템플릿 ID',
	`code`	VARCHAR(10)	NOT NULL	COMMENT '조건 코드 (C1, C2 등)',
	`condition_type`	VARCHAR(30)	NOT NULL	COMMENT '조건 유형 (OBJECT_PRESENT 등 7종)',
	`description`	VARCHAR(300)	NOT NULL	COMMENT '사진에서 확인할 조건 설명',
	`required`	CHAR(1)	NOT NULL	DEFAULT 'Y'	COMMENT '필수 여부 (Y/N, DATE_VISIBLE은 보조만 허용)',
	`sort_order`	INT	NOT NULL	DEFAULT 0	COMMENT '조건 표시 순서',
	`del_yn`	CHAR(1)	NOT NULL	DEFAULT 'N'	COMMENT '삭제 여부 (Y/N)',
	`frst_reg_dt`	DATETIME	NOT NULL	COMMENT '등록 일시',
	`upd_dt`	DATETIME	NULL	COMMENT '수정 일시',
	CONSTRAINT `PK_GOAL_TEMPLATE_CONDITION` PRIMARY KEY (`goal_template_condition_id`)
);

CREATE TABLE `verification_check` (
	`verification_check_id`	BIGINT	NOT NULL AUTO_INCREMENT	COMMENT '조건 판정 ID',
	`verification_id`	BIGINT	NOT NULL	COMMENT '인증 시도 ID',
	`challenge_condition_id`	BIGINT	NOT NULL	COMMENT '도전 생성 당시 인증 조건 ID',
	`status`	VARCHAR(20)	NOT NULL	COMMENT '조건별 판정 (MET/NOT_MET/UNCERTAIN)',
	`reason`	VARCHAR(500)	NULL	COMMENT '조건별 판정 사유',
	`frst_reg_dt`	DATETIME	NOT NULL	COMMENT '등록 일시',
	CONSTRAINT `PK_VERIFICATION_CHECK` PRIMARY KEY (`verification_check_id`)
);

CREATE TABLE `challenge` (
	`challenge_id`	BIGINT	NOT NULL AUTO_INCREMENT	COMMENT '도전 ID',
	`user_id`	BIGINT	NOT NULL	COMMENT '회원 ID',
	`category_id`	BIGINT	NOT NULL	COMMENT '목표 카테고리 ID',
	`challenge_title`	VARCHAR(100)	NOT NULL	COMMENT '사용자 도전 표시 이름',
	`goal_text`	VARCHAR(200)	NOT NULL	COMMENT '생성 당시 목표 문장 스냅샷',
	`input_goal_text`	VARCHAR(500)	NULL	COMMENT '사용자가 직접 입력한 원본 목표 문장',
	`verifiability`	VARCHAR(20)	NOT NULL	DEFAULT 'VERIFIABLE'	COMMENT '사진 인증 가능성 (VERIFIABLE/SUBSTITUTE, 기존 컬럼; 1차 활용 검토)',
	`substitute_accepted`	CHAR(1)	NOT NULL	DEFAULT 'N'	COMMENT '대체 조건 수락 여부 (Y/N, 기존 컬럼; 후속 활용 검토)',
	`photo_guide`	VARCHAR(500)	NULL	COMMENT '생성 당시 촬영 가이드 스냅샷',
	`duration_days`	INT	NOT NULL	COMMENT '도전 기간 일 수 (7/14/30)',
	`start_date`	DATE	NOT NULL	COMMENT '도전 시작일',
	`end_date`	DATE	NOT NULL	COMMENT '도전 종료일',
	`status`	VARCHAR(20)	NOT NULL	DEFAULT 'READY'	COMMENT '도전 상태 (READY/IN_PROGRESS/SETTLING/COMPLETED/STOPPED 등)',
	`stopped_at`	DATETIME	NULL	COMMENT '도전 중단 시각',
	`del_yn`	CHAR(1)	NOT NULL	DEFAULT 'N'	COMMENT '삭제 여부 (Y/N)',
	`frst_reg_dt`	DATETIME	NOT NULL	COMMENT '등록 일시',
	`upd_dt`	DATETIME	NULL	COMMENT '수정 일시',
	`goal_template_id`	BIGINT	NOT NULL	COMMENT '선택한 목표 템플릿 ID (v1.4 추가)',
	`artwork_id`	BIGINT	NOT NULL	COMMENT '해당 도전에 연결된 원본 작품 ID (v1.4 추가)',
	`verification_scope`	VARCHAR(1000)	NOT NULL	COMMENT '도전 생성 당시 사진 인증 범위 안내 스냅샷 (v1.4 추가)',
	`grid_rows`	INT	NOT NULL	COMMENT '퍼즐조각 그리드 행 수',
	`grid_cols`	INT	NOT NULL	COMMENT '퍼즐조각 그리드 열 수',
	`is_exhibited`	CHAR(1)	NOT NULL	DEFAULT 'N'	COMMENT '미술관 전시 여부(Y/N)',
	CONSTRAINT `PK_CHALLENGE` PRIMARY KEY (`challenge_id`)
);

CREATE TABLE `challenge_condition` (
	`challenge_condition_id`	BIGINT	NOT NULL AUTO_INCREMENT	COMMENT '도전 인증 조건 ID',
	`challenge_id`	BIGINT	NOT NULL	COMMENT '도전 ID',
	`code`	VARCHAR(10)	NOT NULL	COMMENT '조건 코드 (C1, C2 등)',
	`condition_type`	VARCHAR(30)	NOT NULL	COMMENT '조건 유형',
	`description`	VARCHAR(300)	NOT NULL	COMMENT '도전 생성 당시 조건 설명',
	`required`	CHAR(1)	NOT NULL	COMMENT '필수 여부 (Y/N)',
	`sort_order`	INT	NOT NULL	COMMENT '표시 순서',
	`frst_reg_dt`	DATETIME	NOT NULL	COMMENT '등록 일시',
	CONSTRAINT `PK_CHALLENGE_CONDITION` PRIMARY KEY (`challenge_condition_id`)
);

CREATE TABLE `artwork` (
	`artwork_id`	BIGINT	NOT NULL AUTO_INCREMENT	COMMENT '작품 ID',
	`status`	VARCHAR(20)	NOT NULL	DEFAULT 'READY'	COMMENT '작품 상태 (1차 제공 작품은 READY)',
	`masterpiece_id`	VARCHAR(50)	NULL	COMMENT '기반 명화 ID',
	`artwork_style`	VARCHAR(200)	NULL	COMMENT '작품 스타일',
	`artwork_title`	VARCHAR(100)	NULL	COMMENT '작품 제목',
	`image_key`	VARCHAR(500)	NULL	COMMENT 'S3 저장 키 (전체 URL 아님)',
	`task_version`	INT	NOT NULL	DEFAULT 1	COMMENT '최신 작품 생성 요청 버전 (3차 활용)',
	`model_version`	VARCHAR(50)	NULL	COMMENT '작품 생성 AI 모델 버전 (사전 제작 메타데이터)',
	`completed_at`	DATETIME	NULL	COMMENT '작품 준비 완료 시각',
	`del_yn`	CHAR(1)	NOT NULL	DEFAULT 'N'	COMMENT '삭제 여부 (Y/N)',
	`frst_reg_dt`	DATETIME	NOT NULL	COMMENT '등록 일시',
	`upd_dt`	DATETIME	NULL	COMMENT '수정 일시',
	CONSTRAINT `PK_ARTWORK` PRIMARY KEY (`artwork_id`)
);

CREATE TABLE `verification` (
	`verification_id`	BIGINT	NOT NULL AUTO_INCREMENT	COMMENT '인증 시도 ID',
	`schedule_id`	BIGINT	NOT NULL	COMMENT '인증 회차 ID',
	`attempt_no`	INT	NOT NULL	COMMENT '회차 내 접수 순서 (1~3)',
	`idempotency_key`	VARCHAR(100)	NOT NULL	COMMENT '중복 제출 방지 키',
	`file_hash`	CHAR(64)	NOT NULL	COMMENT '사진 파일 SHA-256 해시',
	`status`	VARCHAR(20)	NOT NULL	DEFAULT 'SUBMITTED'	COMMENT '인증 시도 상태 (SUBMITTED/ANALYZING/SUCCESS/FAILED/REVIEW_REQUIRED/TECH_ERROR)',
	`submitted_at`	DATETIME	NOT NULL	COMMENT '서버가 제출을 접수한 시각 (마감 판단 기준)',
	`analyzed_at`	DATETIME	NULL	COMMENT 'AI 판정 완료 시각',
	`reason`	VARCHAR(500)	NULL	COMMENT '종합 판정 요약 사유',
	`retake_guide`	VARCHAR(500)	NULL	COMMENT '재촬영 가이드',
	`confidence`	DECIMAL(4, 3)	NULL	COMMENT 'AI 신뢰도 참고값 (결과 단독 판단 금지)',
	`model_version`	VARCHAR(50)	NULL	COMMENT '판정 AI 모델 버전',
	`frst_reg_dt`	DATETIME	NOT NULL	COMMENT '등록 일시',
	`upd_dt`	DATETIME	NULL	COMMENT '수정 일시',
	`media_id`	BIGINT	NOT NULL	COMMENT '사진 파일 정보를 구분하는 ID',
	CONSTRAINT `PK_VERIFICATION` PRIMARY KEY (`verification_id`)
);

CREATE TABLE `schedule` (
	`schedule_id`	BIGINT	NOT NULL AUTO_INCREMENT	COMMENT '회차 ID',
	`challenge_id`	BIGINT	NOT NULL	COMMENT '도전 ID',
	`day_no`	INT	NOT NULL	COMMENT '도전 회차',
	`schedule_date`	DATE	NOT NULL	COMMENT '인증 날짜',
	`start_at`	DATETIME	NOT NULL	COMMENT '인증 가능 시작 시각',
	`deadline_at`	DATETIME	NOT NULL	COMMENT '마감 시각',
	`status`	VARCHAR(30)	NOT NULL	DEFAULT 'UPCOMING'	COMMENT '회차 상태 (UPCOMING/OPEN/PROCESSING/SUCCESS/FAILED/PENDING_SETTLEMENT 등)',
	`attempt_count`	INT	NOT NULL	DEFAULT 0	COMMENT '접수된 사용자 인증 시도 횟수 (최대 3)',
	`result_reason`	VARCHAR(500)	NULL	COMMENT '최종 실패 또는 보류 사유',
	`decided_at`	DATETIME	NULL	COMMENT '최종 회차 결과 확정 시각',
	`version`	INT	NOT NULL	DEFAULT 0	COMMENT '동시 수정 방지 버전(낙관적 락을 위한 버전)',
	`piece_index`	INT	NOT NULL	COMMENT '퍼즐 조각 위치 번호',
	`frst_reg_dt`	DATETIME	NOT NULL	COMMENT '등록 일시',
	`upd_dt`	DATETIME	NULL	COMMENT '수정 일시',
	CONSTRAINT `PK_SCHEDULE` PRIMARY KEY (`schedule_id`)
);

CREATE TABLE `goal_template` (
	`goal_template_id`	BIGINT	NOT NULL AUTO_INCREMENT	COMMENT '목표 템플릿 ID',
	`cate_id`	BIGINT	NOT NULL	COMMENT '목표 카테고리 ID',
	`goal_name`	VARCHAR(100)	NOT NULL	COMMENT '사전 등록 목표 이름',
	`description`	VARCHAR(500)	NULL	COMMENT '목표 수행 내용 설명',
	`photo_guide`	VARCHAR(500)	NOT NULL	COMMENT '조건 충족 사진 촬영 가이드',
	`verification_scope`	VARCHAR(1000)	NOT NULL	COMMENT '사진으로 확인 가능한 범위와 한계 안내',
	`status`	VARCHAR(20)	NOT NULL	DEFAULT 'DRAFT'	COMMENT '목표 제공 상태 (DRAFT/ACTIVE/INACTIVE, 제안)',
	`sort_order`	INT	NOT NULL	DEFAULT 0	COMMENT '목표 목록 표시 순서',
	`del_yn`	CHAR(1)	NOT NULL	DEFAULT 'N'	COMMENT '삭제 여부 (Y/N)',
	`frst_reg_dt`	DATETIME	NOT NULL	COMMENT '등록 일시',
	`upd_dt`	DATETIME	NULL	COMMENT '수정 일시',
	CONSTRAINT `PK_GOAL_TEMPLATE` PRIMARY KEY (`goal_template_id`)
);

CREATE TABLE `goal_template_artwork` (
	`goal_template_artwork_id`	BIGINT	NOT NULL AUTO_INCREMENT	COMMENT '목표-작품 연결 ID',
	`goal_template_id`	BIGINT	NOT NULL	COMMENT '목표 템플릿 ID',
	`artwork_id`	BIGINT	NOT NULL	COMMENT '미리 생성된 작품 ID',
	`sort_order`	INT	NOT NULL	DEFAULT 0	COMMENT '작품 연결 목록 표시 순서',
	`del_yn`	CHAR(1)	NOT NULL	DEFAULT 'N'	COMMENT '연결 삭제 여부 (Y/N)',
	`frst_reg_dt`	DATETIME	NOT NULL	COMMENT '등록 일시',
	`upd_dt`	DATETIME	NULL	COMMENT '수정 일시',
	CONSTRAINT `PK_GOAL_TEMPLATE_ARTWORK` PRIMARY KEY (`goal_template_artwork_id`)
);

CREATE TABLE `media` (
	`media_id`	BIGINT	NOT NULL AUTO_INCREMENT	COMMENT '사진 파일 정보를 구분하는 ID',
	`image_key`	VARCHAR(500)	NOT NULL	COMMENT 'S3 파일 저장된 위치',
	`file_hash`	CHAR(64)	NULL	COMMENT '같은 파일 여부 확인을 위한 SHA-256 해시',
	`content_type`	VARCHAR(100)	NULL	COMMENT '서버에서 확인한 실제 파일 형식',
	`file_size`	BIGINT	NULL	COMMENT '서버에서 확인한 파일 크기',
	`status`	VARCHAR(20)	NOT NULL	DEFAULT 'PENDING'	COMMENT '업로드 및 파일 검증 상태',
	`frst_reg_dt`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '등록 일시',
	`user_id`	BIGINT	NULL	COMMENT '사진을 업로드한 회원 ID',
	`purpose`	VARCHAR(20)	NOT NULL	COMMENT '사진 사용 목적 구분',
	CONSTRAINT `PK_MEDIA` PRIMARY KEY (`media_id`)
);

CREATE TABLE `user_social_account` (
	`social_account_id`	BIGINT	NOT NULL AUTO_INCREMENT	COMMENT '소셜 계정 ID',
	`user_id`	BIGINT	NOT NULL	COMMENT '회원 ID',
	`provider`	VARCHAR(20)	NOT NULL	COMMENT '소셜 제공자 (KAKAO/GOOGLE/NAVER 등)',
	`provider_user_id`	VARCHAR(255)	NOT NULL	COMMENT '제공자가 주는 사용자 고유 ID',
	`email`	VARCHAR(255)	NULL	COMMENT '제공자에서 받은 이메일 (참고용)',
	`frst_reg_dt`	DATETIME	NOT NULL	COMMENT '연결 일시',
	`upd_dt`	DATETIME	NULL	COMMENT '수정 일시',
	CONSTRAINT `PK_USER_SOCIAL_ACCOUNT` PRIMARY KEY (`social_account_id`)
);

CREATE TABLE `ai_task` (
	`ai_task_id`	BIGINT	NOT NULL AUTO_INCREMENT	COMMENT 'AI 작업 ID',
	`task_type`	VARCHAR(30)	NOT NULL	COMMENT '작업 유형 (VERIFY/GENERATE_ARTWORK; 1차는 VERIFY)',
	`verification_id`	BIGINT	NULL	COMMENT '사진 인증 시도 ID (VERIFY에 사용)',
	`artwork_id`	BIGINT	NULL	COMMENT '작품 ID (GENERATE_ARTWORK에 사용, 3차 예정)',
	`task_version`	INT	NOT NULL	DEFAULT 1	COMMENT '작업 요청 버전',
	`status`	VARCHAR(20)	NOT NULL	DEFAULT 'PENDING'	COMMENT 'AI 작업 상태 (PENDING/RUNNING/SUCCEEDED/RETRYING/FAILED_FINAL/CANCELED)',
	`retry_count`	INT	NOT NULL	DEFAULT 0	COMMENT '기술적 재시도 횟수',
	`next_retry_at`	DATETIME	NULL	COMMENT '다음 재시도 예정 시각',
	`locked_by`	VARCHAR(100)	NULL	COMMENT '작업을 가져간 서버 인스턴스',
	`locked_at`	DATETIME	NULL	COMMENT '작업 획득 시각',
	`last_error`	VARCHAR(1000)	NULL	COMMENT '마지막 기술 오류 내용',
	`started_at`	DATETIME	NULL	COMMENT 'AI 작업 실행 시작 시각',
	`finished_at`	DATETIME	NULL	COMMENT 'AI 작업 실행 종료 시각',
	`frst_reg_dt`	DATETIME	NOT NULL	COMMENT '등록 일시',
	`upd_dt`	DATETIME	NULL	COMMENT '수정 일시',
	CONSTRAINT `PK_AI_TASK` PRIMARY KEY (`ai_task_id`)
);

CREATE TABLE `users` (
	`user_id`	BIGINT	NOT NULL AUTO_INCREMENT	COMMENT '회원 ID',
	`login_id`	VARCHAR(50)	NULL	COMMENT '일반 로그인 아이디 (소셜 전용 회원은 NULL)',
	`password`	VARCHAR(255)	NULL	COMMENT '비밀번호 해시 BCrypt (소셜 전용 회원은 NULL)',
	`name`	VARCHAR(100)	NOT NULL	COMMENT '이름',
	`email`	VARCHAR(255)	NULL	COMMENT '이메일 (연락용)',
	`status`	VARCHAR(20)	NOT NULL	DEFAULT 'ACTIVE'	COMMENT '회원 상태 (ACTIVE/WITHDRAWN)',
	`withdrawn_at`	DATETIME	NULL	COMMENT '탈퇴 일시',
	`del_yn`	CHAR(1)	NOT NULL	DEFAULT 'N'	COMMENT '삭제 여부 (Y/N)',
	`frst_reg_dt`	DATETIME	NOT NULL	COMMENT '등록 일시',
	`upd_dt`	DATETIME	NULL	COMMENT '수정 일시',
	`profile_media_id`	BIGINT	NULL	COMMENT '사진 파일 정보를 구분하는 ID',
	CONSTRAINT `PK_USERS` PRIMARY KEY (`user_id`)
);

ALTER TABLE `goal_template_condition` ADD CONSTRAINT `FK_goal_template_TO_goal_template_condition_1` FOREIGN KEY (
	`goal_template_id`
)
REFERENCES `goal_template` (
	`goal_template_id`
);

ALTER TABLE `verification_check` ADD CONSTRAINT `FK_verification_TO_verification_check_1` FOREIGN KEY (
	`verification_id`
)
REFERENCES `verification` (
	`verification_id`
);

ALTER TABLE `verification_check` ADD CONSTRAINT `FK_challenge_condition_TO_verification_check_1` FOREIGN KEY (
	`challenge_condition_id`
)
REFERENCES `challenge_condition` (
	`challenge_condition_id`
);

ALTER TABLE `challenge` ADD CONSTRAINT `FK_users_TO_challenge_1` FOREIGN KEY (
	`user_id`
)
REFERENCES `users` (
	`user_id`
);

ALTER TABLE `challenge` ADD CONSTRAINT `FK_goal_category_TO_challenge_1` FOREIGN KEY (
	`category_id`
)
REFERENCES `goal_category` (
	`cate_id`
);

ALTER TABLE `challenge` ADD CONSTRAINT `FK_goal_template_TO_challenge_1` FOREIGN KEY (
	`goal_template_id`
)
REFERENCES `goal_template` (
	`goal_template_id`
);

ALTER TABLE `challenge` ADD CONSTRAINT `FK_artwork_TO_challenge_1` FOREIGN KEY (
	`artwork_id`
)
REFERENCES `artwork` (
	`artwork_id`
);

ALTER TABLE `challenge_condition` ADD CONSTRAINT `FK_challenge_TO_challenge_condition_1` FOREIGN KEY (
	`challenge_id`
)
REFERENCES `challenge` (
	`challenge_id`
);

ALTER TABLE `verification` ADD CONSTRAINT `FK_schedule_TO_verification_1` FOREIGN KEY (
	`schedule_id`
)
REFERENCES `schedule` (
	`schedule_id`
);

ALTER TABLE `verification` ADD CONSTRAINT `FK_media_TO_verification_1` FOREIGN KEY (
	`media_id`
)
REFERENCES `media` (
	`media_id`
);

ALTER TABLE `schedule` ADD CONSTRAINT `FK_challenge_TO_schedule_1` FOREIGN KEY (
	`challenge_id`
)
REFERENCES `challenge` (
	`challenge_id`
);

ALTER TABLE `goal_template` ADD CONSTRAINT `FK_goal_category_TO_goal_template_1` FOREIGN KEY (
	`cate_id`
)
REFERENCES `goal_category` (
	`cate_id`
);

ALTER TABLE `goal_template_artwork` ADD CONSTRAINT `FK_goal_template_TO_goal_template_artwork_1` FOREIGN KEY (
	`goal_template_id`
)
REFERENCES `goal_template` (
	`goal_template_id`
);

ALTER TABLE `goal_template_artwork` ADD CONSTRAINT `FK_artwork_TO_goal_template_artwork_1` FOREIGN KEY (
	`artwork_id`
)
REFERENCES `artwork` (
	`artwork_id`
);

ALTER TABLE `media` ADD CONSTRAINT `FK_users_TO_media_1` FOREIGN KEY (
	`user_id`
)
REFERENCES `users` (
	`user_id`
);

ALTER TABLE `user_social_account` ADD CONSTRAINT `FK_users_TO_user_social_account_1` FOREIGN KEY (
	`user_id`
)
REFERENCES `users` (
	`user_id`
);

ALTER TABLE `ai_task` ADD CONSTRAINT `FK_verification_TO_ai_task_1` FOREIGN KEY (
	`verification_id`
)
REFERENCES `verification` (
	`verification_id`
);

ALTER TABLE `ai_task` ADD CONSTRAINT `FK_artwork_TO_ai_task_1` FOREIGN KEY (
	`artwork_id`
)
REFERENCES `artwork` (
	`artwork_id`
);

ALTER TABLE `users` ADD CONSTRAINT `FK_media_TO_users_1` FOREIGN KEY (
	`profile_media_id`
)
REFERENCES `media` (
	`media_id`
);

-- 1. 회원 로그인 아이디 중복 방지
ALTER TABLE `users`
ADD CONSTRAINT `uk_users_login_id`
UNIQUE (`login_id`);

-- 2. 카테고리 코드 중복 방지
ALTER TABLE `goal_category`
ADD CONSTRAINT `uk_goal_category_code`
UNIQUE (`cate_code`);

-- 3. 동일 소셜 계정 중복 연결 방지
ALTER TABLE `user_social_account`
ADD CONSTRAINT `uk_social_provider_user`
UNIQUE (`provider`, `provider_user_id`);

-- 4. 목표 템플릿 내 인증 조건 코드 중복 방지
ALTER TABLE `goal_template_condition`
ADD CONSTRAINT `uk_template_condition_code`
UNIQUE (`goal_template_id`, `code`);

-- 5. 도전 내 인증 조건 코드 중복 방지
ALTER TABLE `challenge_condition`
ADD CONSTRAINT `uk_challenge_condition_code`
UNIQUE (`challenge_id`, `code`);

-- 6. 도전 내 동일 회차 중복 방지
ALTER TABLE `schedule`
ADD CONSTRAINT `uk_schedule_challenge_day`
UNIQUE (`challenge_id`, `day_no`);

-- 7. 도전 내 동일 퍼즐 위치 중복 방지
ALTER TABLE `schedule`
ADD CONSTRAINT `uk_schedule_challenge_piece`
UNIQUE (`challenge_id`, `piece_index`);

-- 8. 회차 내 인증 시도 번호 중복 방지
ALTER TABLE `verification`
ADD CONSTRAINT `uk_verification_schedule_attempt`
UNIQUE (`schedule_id`, `attempt_no`);

-- 9. 인증 요청 중복 접수 방지
ALTER TABLE `verification`
ADD CONSTRAINT `uk_verification_idempotency`
UNIQUE (`idempotency_key`);

-- 10. 인증 시도 내 동일 조건 판정 중복 방지
ALTER TABLE `verification_check`
ADD CONSTRAINT `uk_verification_check_condition`
UNIQUE (`verification_id`, `challenge_condition_id`);

-- 11. 목표 템플릿과 작품의 중복 연결 방지
ALTER TABLE `goal_template_artwork`
ADD CONSTRAINT `uk_template_artwork`
UNIQUE (`goal_template_id`, `artwork_id`);
