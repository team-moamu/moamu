-- 분류명은 저장 전에 앞뒤 공백을 제거하고, 같은 부모 아래 중복을 방지한다.
-- NULL 부모를 갖는 대분류의 동시 생성은 확정 트랜잭션의 조회 잠금으로 제어한다.
ALTER TABLE `goal_category`
    ADD CONSTRAINT `uk_goal_category_parent_name` UNIQUE (`parent_cate_id`, `cate_name`);
