-- 접속 후 실행하는 읽기 전용 확인 쿼리입니다.
-- 도메인 테이블을 생성하거나 기존 데이터를 변경하지 않습니다.
SELECT VERSION() AS mysql_version, DATABASE() AS database_name,
       CURRENT_USER() AS database_account;
SELECT @@character_set_database AS character_set_name,
       @@collation_database AS collation_name,
       @@session.time_zone AS session_time_zone;
SELECT 1 AS connection_ok;
