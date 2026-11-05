ALTER TABLE `challenge_condition`
    DROP COLUMN `condition_type`;

ALTER TABLE `challenge_condition`
    DROP COLUMN `SORT_ORDER`;

ALTER TABLE `goal_template_condition`
    DROP COLUMN `REQUIRED`;

ALTER TABLE `goal_template_condition`
    DROP COLUMN `SORT_ORDER`;

ALTER TABLE `schedule`
    DROP COLUMN `start_at`;

ALTER TABLE `schedule`
    DROP COLUMN `deadline_at`;
