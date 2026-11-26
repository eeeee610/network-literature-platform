-- 全局部署只读结构校验；不得用于 novel_platform 正式库。
SET NAMES utf8mb4;
USE `novel_platform_it_20261123_01`;

SELECT 'CHECK' AS result_type, 'TABLE_COUNT' AS check_name, COUNT(*) AS actual, 19 AS expected
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'novel_platform_it_20261123_01' AND TABLE_TYPE = 'BASE TABLE'
UNION ALL
SELECT 'CHECK', 'VIEW_COUNT', COUNT(*), 5
FROM information_schema.VIEWS WHERE TABLE_SCHEMA = 'novel_platform_it_20261123_01'
UNION ALL
SELECT 'CHECK', 'PROCEDURE_COUNT', COUNT(*), 15
FROM information_schema.ROUTINES
WHERE ROUTINE_SCHEMA = 'novel_platform_it_20261123_01' AND ROUTINE_TYPE = 'PROCEDURE'
UNION ALL
SELECT 'CHECK', 'TRIGGER_COUNT', COUNT(*), 8
FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA = 'novel_platform_it_20261123_01'
UNION ALL
SELECT 'CHECK', 'FUNCTION_COUNT', COUNT(*), 0
FROM information_schema.ROUTINES
WHERE ROUTINE_SCHEMA = 'novel_platform_it_20261123_01' AND ROUTINE_TYPE = 'FUNCTION'
UNION ALL
SELECT 'CHECK', 'FOREIGN_KEY_COUNT', COUNT(*), 30
FROM information_schema.REFERENTIAL_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = 'novel_platform_it_20261123_01'
UNION ALL
SELECT 'CHECK', 'INDEX_COUNT', COUNT(DISTINCT TABLE_NAME, INDEX_NAME), 75
FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = 'novel_platform_it_20261123_01'
UNION ALL
SELECT 'CHECK', 'NON_INNODB_TABLE_COUNT', COUNT(*), 0
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'novel_platform_it_20261123_01'
  AND TABLE_TYPE = 'BASE TABLE' AND ENGINE <> 'InnoDB'
UNION ALL
SELECT 'CHECK', 'BAD_DATABASE_DEFAULT_COUNT', COUNT(*), 0
FROM information_schema.SCHEMATA
WHERE SCHEMA_NAME = 'novel_platform_it_20261123_01'
  AND (DEFAULT_CHARACTER_SET_NAME <> 'utf8mb4'
       OR DEFAULT_COLLATION_NAME <> 'utf8mb4_0900_ai_ci')
UNION ALL
SELECT 'CHECK', 'BAD_TABLE_COLLATION_COUNT', COUNT(*), 0
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'novel_platform_it_20261123_01'
  AND TABLE_TYPE = 'BASE TABLE'
  AND (TABLE_COLLATION IS NULL OR TABLE_COLLATION <> 'utf8mb4_0900_ai_ci')
UNION ALL
SELECT 'CHECK', 'BAD_TEXT_COLUMN_COLLATION_COUNT', COUNT(*), 0
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'novel_platform_it_20261123_01'
  AND DATA_TYPE IN ('char','varchar','tinytext','text','mediumtext','longtext','enum','set')
  AND (COLLATION_NAME IS NULL OR COLLATION_NAME <> 'utf8mb4_0900_ai_ci')
UNION ALL
SELECT 'CHECK', 'FK_WITHOUT_LEADING_INDEX_COUNT', COUNT(*), 0
FROM information_schema.KEY_COLUMN_USAGE k
WHERE k.CONSTRAINT_SCHEMA = 'novel_platform_it_20261123_01'
  AND k.REFERENCED_TABLE_NAME IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS s
    WHERE s.TABLE_SCHEMA = k.TABLE_SCHEMA
      AND s.TABLE_NAME = k.TABLE_NAME
      AND s.COLUMN_NAME = k.COLUMN_NAME
      AND s.SEQ_IN_INDEX = 1
  )
UNION ALL
SELECT 'CHECK', 'BAD_FK_DELETE_RULE_COUNT', COUNT(*), 0
FROM information_schema.REFERENTIAL_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = 'novel_platform_it_20261123_01'
  AND NOT (
    (CONSTRAINT_NAME = 'fk_shelf_last_chapter' AND DELETE_RULE = 'SET NULL')
    OR (CONSTRAINT_NAME <> 'fk_shelf_last_chapter' AND DELETE_RULE = 'RESTRICT')
  )
UNION ALL
SELECT 'CHECK', 'BAD_FK_UPDATE_RULE_COUNT', COUNT(*), 0
FROM information_schema.REFERENTIAL_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = 'novel_platform_it_20261123_01'
  AND NOT (
    (CONSTRAINT_NAME IN ('fk_audit_log_admin','fk_admin_operation_admin') AND UPDATE_RULE = 'RESTRICT')
    OR (CONSTRAINT_NAME NOT IN ('fk_audit_log_admin','fk_admin_operation_admin') AND UPDATE_RULE = 'CASCADE')
  );

SELECT 'OBJECT' AS result_type, 'TABLE' AS object_type, TABLE_NAME AS object_name
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'novel_platform_it_20261123_01' AND TABLE_TYPE = 'BASE TABLE'
ORDER BY TABLE_NAME;
SELECT 'OBJECT' AS result_type, 'VIEW' AS object_type, TABLE_NAME AS object_name
FROM information_schema.VIEWS WHERE TABLE_SCHEMA = 'novel_platform_it_20261123_01'
ORDER BY TABLE_NAME;
SELECT 'OBJECT' AS result_type, 'PROCEDURE' AS object_type, ROUTINE_NAME AS object_name
FROM information_schema.ROUTINES
WHERE ROUTINE_SCHEMA = 'novel_platform_it_20261123_01' AND ROUTINE_TYPE = 'PROCEDURE'
ORDER BY ROUTINE_NAME;
SELECT 'OBJECT' AS result_type, 'TRIGGER' AS object_type, TRIGGER_NAME AS object_name
FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA = 'novel_platform_it_20261123_01'
ORDER BY TRIGGER_NAME;

SELECT CONCAT('VIEW|v_user_balance_reconciliation|', COUNT(*)) AS view_result
FROM v_user_balance_reconciliation;
SELECT CONCAT('VIEW|v_comment_visible|', COUNT(*)) AS view_result
FROM v_comment_visible;
SELECT CONCAT('VIEW|v_novel_rating_summary|', COUNT(*)) AS view_result
FROM v_novel_rating_summary;
SELECT CONCAT('VIEW|v_ranking_item_detail|', COUNT(*)) AS view_result
FROM v_ranking_item_detail;
SELECT CONCAT('VIEW|v_ranking_latest|', COUNT(*)) AS view_result
FROM v_ranking_latest;
