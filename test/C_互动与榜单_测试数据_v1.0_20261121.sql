-- =====================================================================
-- 网络文学创作与阅读平台 · C 互动与榜单  测试数据与验证脚本
-- 文件：C_互动与榜单_测试数据_v1.0_20261121.sql
-- 用途：少量 INSERT + 触发器/视图/存储过程验证，可在测试库反复执行（先跑测试前置）
-- 执行顺序：
--   1) test/C_互动与榜单_测试前置_v1.0_20261121.sql
--   2) sql/ddl/C_互动与榜单_建表_v1.0_20261121.sql
--   3) sql/ddl/D_运营后台_建表_v1.0_20261121.sql（审核日志集成测试）
--   4) sql/objects/C_互动与榜单_触发器_v1.0_20261121.sql
--   5) sql/objects/C_互动与榜单_视图与存储过程_v1.0_20261121.sql
--   6) 本脚本
-- =====================================================================

USE c_module_test;
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
SELECT '=== 1. 基础数据：用户 / 小说 / 章节 ===' AS step;
-- ---------------------------------------------------------------------
INSERT INTO app_user (user_id, nickname) VALUES
  (1, '读者甲'), (2, '读者乙'), (3, '读者丙'), (4, '作者丁'), (5, '读者戊');

INSERT INTO novel (novel_id, author_id, title, status, audit_status) VALUES
  (101, 4, '星海拾遗', 0, 1),
  (102, 4, '长夜行舟', 1, 1),
  (103, 4, '旧城书事', 2, 1);   -- 已下架，榜单应排除

INSERT INTO chapter (chapter_id, novel_id, chapter_no, title) VALUES
  (1001, 101, 1, '第一章 回声'),
  (1002, 101, 2, '第二章 潮汐'),
  (2001, 102, 1, '第一章 渡口');

-- ---------------------------------------------------------------------
SELECT '=== 2. A 侧统计源：收藏 / 订阅 / 打赏 ===' AS step;
-- ---------------------------------------------------------------------
INSERT INTO user_bookshelf (user_id, novel_id) VALUES
  (1, 101), (2, 101), (3, 101), (5, 101),          -- 101 收藏 4
  (1, 102), (2, 102),                              -- 102 收藏 2
  (1, 103), (2, 103), (3, 103), (4, 103), (5, 103); -- 103 收藏 5（已下架）

INSERT INTO chapter_subscription (user_id, chapter_id, novel_id, paid_amount, status) VALUES
  (1, 1001, 101, 0.15, 'ACTIVE'),
  (2, 1001, 101, 0.15, 'ACTIVE'),
  (3, 1002, 101, 0.20, 'ACTIVE'),
  (1, 2001, 102, 0.25, 'ACTIVE'),
  (2, 2001, 102, 0.25, 'REFUNDED');

INSERT INTO reward_tip (novel_id, amount, status) VALUES
  (101, 10.00, 'SUCCESS'),
  (101, 20.00, 'SUCCESS'),
  (102,  5.00, 'SUCCESS'),
  (102, 99.00, 'REFUNDED');

-- ---------------------------------------------------------------------
SELECT '=== 3. 评论统一写入过程与 reply_count 状态迁移 ===' AS step;
-- ---------------------------------------------------------------------
INSERT INTO comment (comment_id, novel_id, chapter_id, user_id, parent_id, reply_to_user_id,
                     content, audit_status, status) VALUES
  (5001, 101, 1001, 1, NULL, NULL, '父评论甲。', 1, 1),
  (5005, 101, 1001, 1, NULL, NULL, '父评论乙。', 1, 1);

DROP PROCEDURE IF EXISTS sp_test_assert_reply_count;
DELIMITER $$
CREATE PROCEDURE sp_test_assert_reply_count(
  IN p_comment_id BIGINT UNSIGNED,
  IN p_expected   INT,
  IN p_label      VARCHAR(100)
)
BEGIN
  DECLARE v_actual INT DEFAULT NULL;
  SELECT reply_count INTO v_actual FROM comment WHERE comment_id = p_comment_id;
  IF v_actual IS NULL OR v_actual <> p_expected THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = p_label;
  END IF;
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS sp_test_assert_visible_replies;
DELIMITER $$
CREATE PROCEDURE sp_test_assert_visible_replies(
  IN p_parent_id BIGINT UNSIGNED,
  IN p_expected  INT,
  IN p_label     VARCHAR(100)
)
BEGIN
  DECLARE v_actual INT DEFAULT NULL;
  SELECT COUNT(*) INTO v_actual FROM v_comment_visible WHERE parent_id = p_parent_id;
  IF v_actual <> p_expected THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = p_label;
  END IF;
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS sp_test_invalid_status_rollback;
DELIMITER $$
CREATE PROCEDURE sp_test_invalid_status_rollback(IN p_comment_id BIGINT UNSIGNED)
BEGIN
  DECLARE v_error TINYINT DEFAULT 0;
  BEGIN
    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION SET v_error = 1;
    CALL sp_set_comment_status(p_comment_id, 9);
  END;
  IF v_error = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '非法评论状态未被拒绝';
  END IF;
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS sp_test_create_comment_failure;
DELIMITER $$
CREATE PROCEDURE sp_test_create_comment_failure(
  IN p_novel_id    BIGINT UNSIGNED,
  IN p_chapter_id  BIGINT UNSIGNED,
  IN p_parent_id   BIGINT UNSIGNED,
  IN p_label       VARCHAR(100)
)
BEGIN
  DECLARE v_error TINYINT DEFAULT 0;
  DECLARE v_comment_id BIGINT UNSIGNED DEFAULT NULL;
  DECLARE v_before_total INT;
  DECLARE v_after_total INT;
  DECLARE v_parent_count INT DEFAULT 0;
  DECLARE v_after_parent_count INT DEFAULT 0;

  SELECT COUNT(*) INTO v_before_total FROM comment;
  IF p_parent_id IS NOT NULL THEN
    SELECT reply_count INTO v_parent_count FROM comment WHERE comment_id = p_parent_id;
  END IF;
  BEGIN
    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION SET v_error = 1;
    CALL sp_create_comment(p_novel_id, p_chapter_id, 2, p_parent_id, 1, '应失败的评论', v_comment_id);
  END;
  IF v_error = 0 OR v_comment_id IS NOT NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = p_label;
  END IF;
  SELECT COUNT(*) INTO v_after_total FROM comment;
  IF p_parent_id IS NOT NULL THEN
    SELECT reply_count INTO v_after_parent_count FROM comment WHERE comment_id = p_parent_id;
  END IF;
  IF v_before_total <> v_after_total OR v_parent_count <> v_after_parent_count THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '失败新增改变了评论数据或回复数';
  END IF;
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS sp_test_counter_failure_rollback;
DELIMITER $$
CREATE PROCEDURE sp_test_counter_failure_rollback(
  IN p_comment_id BIGINT UNSIGNED,
  IN p_parent_id  BIGINT UNSIGNED
)
BEGIN
  DECLARE v_error TINYINT DEFAULT 0;
  DECLARE v_status TINYINT;
  DECLARE v_parent_count INT;
  START TRANSACTION;
  BEGIN
    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION SET v_error = 1;
    CALL sp_set_comment_status(p_comment_id, 2);
  END;
  ROLLBACK;
  IF v_error = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '父评论计数异常时应回滚评论状态';
  END IF;
  SELECT status INTO v_status FROM comment WHERE comment_id = p_comment_id;
  SELECT reply_count INTO v_parent_count FROM comment WHERE comment_id = p_parent_id;
  IF v_status <> 1 OR v_parent_count <> 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '计数失败后评论状态或父计数未回滚';
  END IF;
END$$
DELIMITER ;

-- 验证过程不会隐式提交调用方事务，并验证显式提交可正常持久化。
DROP PROCEDURE IF EXISTS sp_test_comment_caller_transaction;
DELIMITER $$
CREATE PROCEDURE sp_test_comment_caller_transaction(
  IN p_comment_id BIGINT UNSIGNED,
  IN p_parent_id  BIGINT UNSIGNED
)
BEGIN
  DECLARE v_status TINYINT;
  DECLARE v_count INT;

  START TRANSACTION;
  INSERT INTO app_user (user_id, nickname) VALUES (99, '外层事务临时用户');
  CALL sp_set_comment_status(p_comment_id, 2);
  ROLLBACK;

  SELECT status INTO v_status FROM comment WHERE comment_id = p_comment_id;
  SELECT COUNT(*) INTO v_count FROM app_user WHERE user_id = 99;
  IF v_status <> 1 OR v_count <> 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '外层回滚未撤销评论修改或前置插入';
  END IF;
  CALL sp_test_assert_reply_count(p_parent_id, 1, '外层回滚后回复计数应恢复');

  START TRANSACTION;
  CALL sp_set_comment_status(p_comment_id, 2);
  COMMIT;
  SELECT status INTO v_status FROM comment WHERE comment_id = p_comment_id;
  IF v_status <> 2 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '调用方显式提交后评论状态未持久化';
  END IF;
  CALL sp_test_assert_reply_count(p_parent_id, 0, '显式提交隐藏后计数应为 0');

  START TRANSACTION;
  CALL sp_set_comment_status(p_comment_id, 1);
  COMMIT;
END$$
DELIMITER ;

-- 新增待审核回复不计数；审核通过 +1，重复通过不重复计数。
START TRANSACTION;
CALL sp_create_comment(101, 1001, 2, 5001, 1, '第一条回复，初始待审核。', @reply1);
CALL sp_test_assert_reply_count(5001, 0, '新增待审核回复不应计数');
CALL sp_audit_comment(@reply1, 1);
CALL sp_test_assert_reply_count(5001, 1, '审核通过应 +1');
CALL sp_audit_comment(@reply1, 1);
CALL sp_test_assert_reply_count(5001, 1, '重复审核通过不应重复计数');

-- 隐藏/恢复、驳回/重新通过、软删除/恢复均按可见性迁移计数。
CALL sp_set_comment_status(@reply1, 2);
CALL sp_test_assert_reply_count(5001, 0, '隐藏可见回复应 -1');
CALL sp_set_comment_status(@reply1, 2);
CALL sp_test_assert_reply_count(5001, 0, '重复隐藏不应重复减计数');
CALL sp_set_comment_status(@reply1, 1);
CALL sp_test_assert_reply_count(5001, 1, '恢复可见回复应 +1');
CALL sp_audit_comment(@reply1, 2);
CALL sp_test_assert_reply_count(5001, 0, '驳回可见回复应 -1');
CALL sp_audit_comment(@reply1, 2);
CALL sp_test_assert_reply_count(5001, 0, '重复驳回不应重复减计数');
CALL sp_audit_comment(@reply1, 1);
CALL sp_test_assert_reply_count(5001, 1, '重新审核通过应 +1');
CALL sp_set_comment_status(@reply1, 3);
CALL sp_test_assert_reply_count(5001, 0, '软删除可见回复应 -1');
CALL sp_set_comment_status(@reply1, 3);
CALL sp_test_assert_reply_count(5001, 0, '重复软删除不应重复减计数');
CALL sp_set_comment_status(@reply1, 1);
CALL sp_test_assert_reply_count(5001, 1, '软删除恢复后应 +1');

-- 内容修改由现有触发器重新置待审核；重复修改相同内容不重复计数。
CALL sp_edit_comment_content(@reply1, '第一条回复，内容已修改。');
CALL sp_test_assert_reply_count(5001, 0, '可见回复内容修改后重新待审应 -1');
SELECT '内容修改后审核状态应为 0' AS note, comment_id, audit_status
  FROM comment WHERE comment_id = @reply1;
CALL sp_edit_comment_content(@reply1, '第一条回复，内容已修改。');
CALL sp_test_assert_reply_count(5001, 0, '重复写入相同内容不应重复减计数');
CALL sp_audit_comment(@reply1, 1);
CALL sp_test_assert_reply_count(5001, 1, '修改后重新通过应 +1');

-- 另一条一级评论接受直接回复；待审核不计数，通过后计入其一级父评论。
CALL sp_create_comment(101, 1001, 3, 5005, 1, '第二条回复，初始待审核。', @reply2);
CALL sp_test_assert_reply_count(5005, 0, '新增第二条待审核回复不应计数');
CALL sp_audit_comment(@reply2, 1);
CALL sp_test_assert_reply_count(5005, 1, '第二条回复通过后应计入一级父评论');

-- 父评论隐藏只影响在线展示，不改其子回复计数；恢复后按自身状态展示。
CALL sp_set_comment_status(5001, 2);
CALL sp_test_assert_reply_count(5001, 1, '隐藏父评论不应改变其子回复计数');
CALL sp_test_assert_visible_replies(5001, 0, '父评论隐藏后子回复不应在线展示');
CALL sp_set_comment_status(5001, 1);
CALL sp_test_assert_reply_count(5001, 1, '恢复父评论不应重复增加子回复计数');
CALL sp_test_assert_visible_replies(5001, 1, '父评论恢复后可见子回复应恢复展示');

-- 非法状态必须报错，评论状态和计数保持原值。
CALL sp_test_invalid_status_rollback(@reply1);
CALL sp_test_assert_reply_count(5001, 1, '非法状态被拒绝后父评论计数不应变化');
SELECT '非法状态被拒绝后回复仍应为正常+通过' AS note, status, audit_status
  FROM comment WHERE comment_id = @reply1;
COMMIT;

-- 调用方显式回滚/提交事务。
CALL sp_test_comment_caller_transaction(@reply1, 5001);
CALL sp_test_assert_reply_count(5001, 1, '调用方事务测试后计数应恢复');

-- ---------------------------------------------------------------------
SELECT '=== 4. 评论点赞触发器 ===' AS step;
-- ---------------------------------------------------------------------
INSERT INTO comment_like (comment_id, user_id) VALUES (5001, 2), (5001, 3), (5001, 5);
SELECT '点赞后 like_count，期望 3' AS note, comment_id, like_count
  FROM comment WHERE comment_id = 5001;

DELETE FROM comment_like WHERE comment_id = 5001 AND user_id = 3;
SELECT '取消点赞后 like_count，期望 2' AS note, comment_id, like_count
  FROM comment WHERE comment_id = 5001;

-- ---------------------------------------------------------------------
SELECT '=== 5. 评分与 novel_rating_stat 触发器 ===' AS step;
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_test_assert_rating_stat;
DELIMITER $$
CREATE PROCEDURE sp_test_assert_rating_stat(
  IN p_novel_id BIGINT UNSIGNED,
  IN p_expected_count INT,
  IN p_expected_total BIGINT,
  IN p_expected_avg DECIMAL(3,2),
  IN p_label VARCHAR(100)
)
BEGIN
  DECLARE v_count INT DEFAULT NULL;
  DECLARE v_total BIGINT DEFAULT NULL;
  DECLARE v_avg DECIMAL(3,2) DEFAULT NULL;
  SELECT rating_count, score_total, avg_score
    INTO v_count, v_total, v_avg
    FROM novel_rating_stat
   WHERE novel_id = p_novel_id;
  IF v_count IS NULL OR v_count <> p_expected_count
     OR v_total <> p_expected_total OR v_avg <> p_expected_avg THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = p_label;
  END IF;
END$$
DELIMITER ;

INSERT INTO novel_rating (user_id, novel_id, score, status) VALUES
  (1, 101, 5, 1), (2, 101, 4, 1), (3, 101, 3, 1);
SELECT '新增 3 条评分：count=3 total=12 avg=4.00' AS note, novel_id, rating_count, score_total, avg_score
  FROM novel_rating_stat WHERE novel_id = 101;
CALL sp_test_assert_rating_stat(101, 3, 12, 4.00, '新增评分后的统计断言失败');

UPDATE novel_rating SET score = 1 WHERE user_id = 3 AND novel_id = 101;
SELECT '改分后：count=3 total=10 avg=3.33' AS note, novel_id, rating_count, score_total, avg_score
  FROM novel_rating_stat WHERE novel_id = 101;

UPDATE novel_rating SET status = 2 WHERE user_id = 2 AND novel_id = 101;
SELECT '隐藏 1 条评分后：count=2 total=6 avg=3.00' AS note, novel_id, rating_count, score_total, avg_score
  FROM novel_rating_stat WHERE novel_id = 101;

DELETE FROM novel_rating WHERE user_id = 1 AND novel_id = 101;
SELECT '删除 1 条评分后：count=1 total=1 avg=1.00' AS note, novel_id, rating_count, score_total, avg_score
  FROM novel_rating_stat WHERE novel_id = 101;
CALL sp_test_assert_rating_stat(101, 1, 1, 1.00, '评分新增改分隐藏删除后的统计断言失败');

START TRANSACTION;
CALL sp_refresh_rating_stat(101);
COMMIT;
SELECT '按明细重算后与增量结果一致' AS note, novel_id, rating_count, score_total, avg_score
  FROM novel_rating_stat WHERE novel_id = 101;

-- 短评：空串存 NULL；新增/修改短评后待审核
INSERT INTO novel_rating (user_id, novel_id, score, review_content) VALUES (4, 102, 4, '');
SELECT '空短评规范化：review_content=NULL, audit=NULL' AS note,
       user_id, novel_id, review_content, review_audit_status
  FROM novel_rating WHERE user_id = 4 AND novel_id = 102;

UPDATE novel_rating SET review_content = '文风克制，余味很长。'
 WHERE user_id = 4 AND novel_id = 102;
SELECT '新增短评后应为待审核 0' AS note, review_content, review_audit_status
  FROM novel_rating WHERE user_id = 4 AND novel_id = 102;

UPDATE novel_rating SET review_audit_status = 1 WHERE user_id = 4 AND novel_id = 102;
UPDATE novel_rating SET review_content = '文风克制，余味悠长。'
 WHERE user_id = 4 AND novel_id = 102;
SELECT '审核通过后修改短评，应重新置 0' AS note, review_content, review_audit_status
  FROM novel_rating WHERE user_id = 4 AND novel_id = 102;

INSERT INTO novel_rating (user_id, novel_id, score, status) VALUES (5, 102, 5, 1);
CALL sp_test_assert_rating_stat(102, 2, 9, 4.50, '评分短评场景的数值统计断言失败');

-- ---------------------------------------------------------------------
SELECT '=== 6. 榜单配置与生成 ===' AS step;
-- ---------------------------------------------------------------------
-- 同一次测试固定一个当前日期，避免跨月及午夜边界导致期次不一致。
SET @test_today = CURDATE();
SET @period_month = DATE_FORMAT(@test_today, '%Y%m');
SET @period_history = DATE_FORMAT(DATE_SUB(@test_today, INTERVAL 1 MONTH), '%Y%m');

DROP PROCEDURE IF EXISTS sp_test_assert_ranking_snapshot;
DELIMITER $$
CREATE PROCEDURE sp_test_assert_ranking_snapshot(
  IN p_ranking_code VARCHAR(50),
  IN p_period_no VARCHAR(20),
  IN p_expected_count INT,
  IN p_rank1_novel BIGINT UNSIGNED,
  IN p_rank1_score DECIMAL(12,4),
  IN p_rank2_novel BIGINT UNSIGNED,
  IN p_rank2_score DECIMAL(12,4),
  IN p_label VARCHAR(100)
)
BEGIN
  DECLARE v_count INT DEFAULT 0;
  DECLARE v_rank1_novel BIGINT UNSIGNED DEFAULT NULL;
  DECLARE v_rank2_novel BIGINT UNSIGNED DEFAULT NULL;
  DECLARE v_rank1_score DECIMAL(12,4) DEFAULT NULL;
  DECLARE v_rank2_score DECIMAL(12,4) DEFAULT NULL;

  SELECT COUNT(*),
         MAX(CASE WHEN i.rank_no = 1 THEN i.novel_id END),
         MAX(CASE WHEN i.rank_no = 1 THEN i.score END),
         MAX(CASE WHEN i.rank_no = 2 THEN i.novel_id END),
         MAX(CASE WHEN i.rank_no = 2 THEN i.score END)
    INTO v_count, v_rank1_novel, v_rank1_score, v_rank2_novel, v_rank2_score
    FROM ranking_item i
    JOIN ranking_config c ON c.ranking_id = i.ranking_id
   WHERE c.ranking_code = p_ranking_code
     AND i.period_no = p_period_no;

  IF v_count <> p_expected_count
     OR v_rank1_novel IS NULL OR v_rank1_novel <> p_rank1_novel
     OR v_rank1_score <> p_rank1_score
     OR v_rank2_novel IS NULL OR v_rank2_novel <> p_rank2_novel
     OR v_rank2_score <> p_rank2_score THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = p_label;
  END IF;
END$$
DELIMITER ;

INSERT INTO ranking_config
  (ranking_id, ranking_code, ranking_name, ranking_type, period_type, top_n, weight_config) VALUES
  (1, 'FAVORITE_MONTH', '月收藏榜', 'FAVORITE', 'MONTHLY', 3, '{"favorite":1.0}'),
  (2, 'RATING_ALL',     '总评分榜', 'RATING',   'ALL',     3, '{"rating":1.0}'),
  (3, 'DAILY_TEST',     '日榜期次校验', 'TICKET', 'DAILY', 3, '{"ticket":1.0}'),
  (4, 'WEEKLY_TEST',    '周榜期次校验', 'TICKET', 'WEEKLY', 3, '{"ticket":1.0}');

START TRANSACTION;
CALL sp_generate_ranking(1, @period_month, 0, @cnt_fav);
COMMIT;
SELECT '收藏榜生成条数（下架作品排除），期望 2' AS note, @cnt_fav AS item_count;

START TRANSACTION;
CALL sp_generate_ranking(2, 'ALL', 0, @cnt_rate);
COMMIT;
SELECT '评分榜生成条数，期望 2' AS note, @cnt_rate AS item_count;
CALL sp_test_assert_ranking_snapshot(
  'FAVORITE_MONTH', @period_month, 2, 101, 4.0000, 102, 2.0000, '收藏榜名次或得分断言失败'
);
CALL sp_test_assert_ranking_snapshot(
  'RATING_ALL', 'ALL', 2, 102, 4.5000, 101, 1.0000, '评分榜名次或得分断言失败'
);

-- 榜单过程不得隐式提交调用方前置操作；外层回滚后旧榜单仍保留。
DROP PROCEDURE IF EXISTS sp_test_ranking_caller_rollback;
DELIMITER $$
CREATE PROCEDURE sp_test_ranking_caller_rollback(IN p_period_no VARCHAR(20))
BEGIN
  DECLARE v_user_count INT;
  DECLARE v_item_count INT;
  DECLARE v_generated INT DEFAULT 0;

  START TRANSACTION;
  INSERT INTO app_user (user_id, nickname) VALUES (98, '榜单外层事务临时用户');
  CALL sp_generate_ranking(1, p_period_no, 0, v_generated);
  ROLLBACK;

  SELECT COUNT(*) INTO v_user_count FROM app_user WHERE user_id = 98;
  SELECT COUNT(*) INTO v_item_count
    FROM ranking_item WHERE ranking_id = 1 AND period_no = p_period_no;
  IF v_user_count <> 0 OR v_item_count <> 2 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '榜单外层事务回滚结果不一致';
  END IF;
END$$
DELIMITER ;
CALL sp_test_ranking_caller_rollback(@period_month);
DROP PROCEDURE IF EXISTS sp_test_ranking_caller_rollback;

-- 非法期次及历史期次覆盖均须报错，且不得改变既有榜单条目。
DROP PROCEDURE IF EXISTS sp_test_ranking_generation_failure;
DELIMITER $$
CREATE PROCEDURE sp_test_ranking_generation_failure(
  IN p_ranking_id BIGINT UNSIGNED,
  IN p_period_no VARCHAR(20),
  IN p_allow_history_overwrite TINYINT,
  IN p_label VARCHAR(100)
)
BEGIN
  DECLARE v_error TINYINT DEFAULT 0;
  DECLARE v_item_count INT DEFAULT 0;
  DECLARE v_before_count INT DEFAULT 0;
  DECLARE v_after_count INT DEFAULT 0;

  SELECT COUNT(*) INTO v_before_count
    FROM ranking_item
   WHERE ranking_id = p_ranking_id AND period_no = p_period_no;

  START TRANSACTION;
  BEGIN
    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION SET v_error = 1;
    CALL sp_generate_ranking(p_ranking_id, p_period_no, p_allow_history_overwrite, v_item_count);
  END;
  ROLLBACK;

  SELECT COUNT(*) INTO v_after_count
    FROM ranking_item
   WHERE ranking_id = p_ranking_id AND period_no = p_period_no;
  IF v_error = 0 OR v_before_count <> v_after_count THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = p_label;
  END IF;
END$$
DELIMITER ;

CALL sp_test_ranking_generation_failure(3, '20260231', 0, '无效 DAILY 日期未被拒绝');
CALL sp_test_ranking_generation_failure(4, '2025W99', 0, '无效 WEEKLY 周未被拒绝');
CALL sp_test_ranking_generation_failure(1, '202513', 0, '无效 MONTHLY 月份未被拒绝');

-- 动态构造真实历史月份：首次补建允许，已有快照后默认禁止覆盖。
START TRANSACTION;
CALL sp_generate_ranking(1, @period_history, 0, @cnt_history);
COMMIT;
CALL sp_test_ranking_generation_failure(1, @period_history, 0, '历史期次默认覆盖保护未生效');

SELECT '收藏榜最新期次' AS note, ranking_code, period_no, rank_no, novel_title, score, metric_snapshot
  FROM v_ranking_latest
 WHERE ranking_code = 'FAVORITE_MONTH'
 ORDER BY rank_no;

SELECT '评分榜指定期次' AS note, ranking_code, period_no, rank_no, novel_title, score, metric_snapshot
  FROM v_ranking_item_detail
 WHERE ranking_code = 'RATING_ALL' AND period_no = 'ALL'
 ORDER BY rank_no;

CALL sp_get_ranking('FAVORITE_MONTH', NULL, 10);

-- ---------------------------------------------------------------------
SELECT '=== 7. 视图验证 ===' AS step;
-- ---------------------------------------------------------------------
SELECT '可见评论：仅审核通过且状态正常' AS note, comment_id, parent_id, content
  FROM v_comment_visible WHERE novel_id = 101 ORDER BY comment_id;

-- 父评论隐藏/恢复、子回复展示及计数不变已在第 3 节通过统一写入过程验证。

SELECT '评分汇总视图' AS note, novel_id, title, rating_count, score_total, avg_score
  FROM v_novel_rating_summary WHERE novel_id IN (101, 102) ORDER BY novel_id;

-- ---------------------------------------------------------------------
SELECT '=== 7.1 两级结构、归属、计数与 C/D 审核集成 ===' AS step;
-- ---------------------------------------------------------------------
-- 直接回复不能再作为父评论，禁止形成第三级。
CALL sp_test_create_comment_failure(101, 1001, @reply1, '回复直接回复形成第三级应失败');

-- 仅以直接 DML 模拟历史库已有的深层数据；应用层仍不得直接写 comment。
INSERT INTO comment (comment_id, novel_id, chapter_id, user_id, parent_id, reply_to_user_id,
                     content, audit_status, status) VALUES
  (5901, 101, 1001, 2, @reply1, 1, '历史第三级评论。', 1, 1),
  (5902, 101, 1001, 2, 5901, 1, '历史第四级评论。', 1, 1);
CALL sp_test_assert_visible_replies(@reply1, 0, '历史第三级评论不得展示');
CALL sp_test_assert_visible_replies(5901, 0, '历史第四级评论不得展示');

-- 构造其他归属及整本书评父评论，验证创建时的严格一致规则。
START TRANSACTION;
CALL sp_create_comment(102, 2001, 2, NULL, NULL, '其他小说一级评论。', @other_novel_root);
CALL sp_audit_comment(@other_novel_root, 1);
CALL sp_create_comment(101, 1002, 2, NULL, NULL, '其他章节一级评论。', @other_chapter_root);
CALL sp_audit_comment(@other_chapter_root, 1);
CALL sp_create_comment(101, NULL, 2, NULL, NULL, '整本书评一级评论。', @book_root);
CALL sp_audit_comment(@book_root, 1);
COMMIT;

-- 一级评论也须保证章节属于小说；回复继续校验父子归属和 NULL 一致性。
CALL sp_test_create_comment_failure(101, 2001, NULL, '一级评论章节属于其他小说应失败');
CALL sp_test_create_comment_failure(102, 2001, 5001, '跨小说新增回复应失败');
CALL sp_test_create_comment_failure(101, 1002, 5001, '跨章节新增回复应失败');
CALL sp_test_create_comment_failure(101, NULL, 5001, '章节回复与章节父评论 NULL 不一致应失败');
CALL sp_test_create_comment_failure(101, 1001, @book_root, '章节回复与整本书评 NULL 不一致应失败');

-- 计数更新在评论 UPDATE 之后失败时，由调用方回滚整个评论操作。
UPDATE comment SET reply_count = 0 WHERE comment_id = 5001;
CALL sp_test_counter_failure_rollback(@reply1, 5001);

-- 仅以对账目的调用全量重算，结果必须与前面的增量维护一致，并修复故障注入计数。
START TRANSACTION;
CALL sp_sync_comment_reply_count(5001);
CALL sp_sync_comment_reply_count(5005);
COMMIT;
CALL sp_test_assert_reply_count(5001, 1, '父评论甲全量对账结果不一致');
CALL sp_test_assert_reply_count(5005, 1, '父评论乙全量对账结果不一致');

-- C/D 审核集成：状态与日志同事务提交；日志 CHECK 失败时整体回滚。
DROP PROCEDURE IF EXISTS sp_test_audit_log_integration;
DELIMITER $$
CREATE PROCEDURE sp_test_audit_log_integration(IN p_comment_id BIGINT UNSIGNED)
BEGIN
  DECLARE v_error TINYINT DEFAULT 0;
  DECLARE v_step TINYINT DEFAULT 0;
  DECLARE v_audit TINYINT DEFAULT NULL;
  DECLARE v_log_before INT DEFAULT 0;
  DECLARE v_log_after INT DEFAULT 0;

  SELECT COUNT(*) INTO v_log_before
    FROM audit_log
   WHERE target_type = 'COMMENT' AND target_id = p_comment_id;

  START TRANSACTION;
  CALL sp_audit_comment(p_comment_id, 2);
  INSERT INTO audit_log (admin_id, target_type, target_id, audit_action, audit_reason)
  VALUES (4, 'COMMENT', p_comment_id, 2, 'C/D 审核集成测试驳回');
  COMMIT;

  SELECT audit_status INTO v_audit FROM comment WHERE comment_id = p_comment_id;
  SELECT COUNT(*) INTO v_log_after
    FROM audit_log
   WHERE target_type = 'COMMENT' AND target_id = p_comment_id;
  IF v_audit <> 2 OR v_log_after <> v_log_before + 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '审核状态与日志未在同一事务提交';
  END IF;

  START TRANSACTION;
  BEGIN
    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION SET v_error = 1;
    CALL sp_audit_comment(p_comment_id, 1);
    SET v_step = 1;
    INSERT INTO audit_log (admin_id, target_type, target_id, audit_action, audit_reason)
    VALUES (4, 'COMMENT', p_comment_id, 2, NULL);
  END;
  ROLLBACK;

  IF v_error = 0 OR v_step <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '未在审核更新后触发预期的日志写入失败';
  END IF;
  SELECT audit_status INTO v_audit FROM comment WHERE comment_id = p_comment_id;
  SELECT COUNT(*) INTO v_log_after
    FROM audit_log
   WHERE target_type = 'COMMENT' AND target_id = p_comment_id;
  IF v_audit <> 2 OR v_log_after <> v_log_before + 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '日志写入失败后审核状态或日志未整体回滚';
  END IF;
END$$
DELIMITER ;
CALL sp_test_audit_log_integration(@other_novel_root);

DROP PROCEDURE IF EXISTS sp_test_assert_reply_count;
DROP PROCEDURE IF EXISTS sp_test_assert_visible_replies;
DROP PROCEDURE IF EXISTS sp_test_invalid_status_rollback;
DROP PROCEDURE IF EXISTS sp_test_create_comment_failure;
DROP PROCEDURE IF EXISTS sp_test_counter_failure_rollback;
DROP PROCEDURE IF EXISTS sp_test_comment_caller_transaction;
DROP PROCEDURE IF EXISTS sp_test_assert_rating_stat;
DROP PROCEDURE IF EXISTS sp_test_assert_ranking_snapshot;
DROP PROCEDURE IF EXISTS sp_test_ranking_generation_failure;
DROP PROCEDURE IF EXISTS sp_test_audit_log_integration;

-- ---------------------------------------------------------------------
SELECT '=== 8. 对象清单校验 ===' AS step;
-- ---------------------------------------------------------------------
SELECT 'C 模块表数量' AS note, COUNT(*) AS cnt
  FROM information_schema.tables
 WHERE table_schema = 'c_module_test' AND table_name IN
       ('comment','comment_like','novel_rating','novel_rating_stat','ranking_config','ranking_item');
SELECT '触发器数量' AS note, COUNT(*) AS cnt
  FROM information_schema.triggers
 WHERE trigger_schema = 'c_module_test';
SELECT '视图数量' AS note, COUNT(*) AS cnt
  FROM information_schema.views
 WHERE table_schema = 'c_module_test';
SELECT '存储过程数量' AS note, COUNT(*) AS cnt
  FROM information_schema.routines
 WHERE routine_schema = 'c_module_test' AND routine_type = 'PROCEDURE';
