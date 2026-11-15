ALTER TABLE `goal_category`
    ADD COLUMN `parent_cate_id` BIGINT NULL COMMENT '부모 카테고리 ID (대분류는 NULL)' AFTER `cate_id`,
    ADD INDEX `idx_goal_category_parent` (`parent_cate_id`),
    ADD CONSTRAINT `fk_goal_category_parent`
        FOREIGN KEY (`parent_cate_id`) REFERENCES `goal_category` (`cate_id`);
