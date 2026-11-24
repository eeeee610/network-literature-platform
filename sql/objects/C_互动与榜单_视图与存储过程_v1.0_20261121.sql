-- =====================================================================
-- 网络文学创作与阅读平台 · C 互动与榜单  视图与存储过程脚本
-- 文件：C_互动与榜单_视图与存储过程_v1.0_20261121.sql
-- 模块：C（互动与榜单）        负责人：C        版本：v1.0 / 2026-11-21
-- 依据：docs/current/D_数据字典_v1.0_20261120.xlsx（冻结基线）
-- 数据库：MySQL 8.0+
-- -------------------------------------------------------------------
-- 对象清单：
--   视图 4 个
--     v_comment_visible        评论展示视图（审核通过 + 状态正常 + 父评论正常）
--     v_novel_rating_summary   小说评分汇总视图（唯一权威评分统计）
--     v_ranking_item_detail    榜单条目明细视图（联表书名与榜单元信息）
--     v_ranking_latest         榜单最新期次视图
--   存储过程 4 个
--     sp_sync_comment_reply_count  重算评论回复数（替代同表触发器）
--     sp_refresh_rating_stat       按明细重算评分统计（对账/修复用）
--     sp_generate_ranking          按指标生成某榜单某期次的名次快照
--     sp_get_ranking               查询榜单某期次（不传期次取最新）
-- -------------------------------------------------------------------
-- 前置：先执行 C_互动与榜单_建表_v1.0_20261121.sql 与触发器脚本。
-- =====================================================================

SET NAMES utf8mb4;

-- =====================================================================
-- 一、视图
-- =====================================================================

-- 1. 评论展示视图：审核通过、状态正常，且父评论同样正常（父评论异常时回复不单独展示）
CREATE OR REPLACE VIEW v_comment_visible AS
SELECT
  c.comment_id,
  c.novel_id,
  c.chapter_id,
  c.user_id,
  c.parent_id,
  c.reply_to_user_id,
  c.content,
  c.like_count,
  c.reply_count,
  c.is_top,
  c.created_at,
  c.updated_at
FROM comment c
LEFT JOIN comment p ON p.comment_id = c.parent_id
WHERE c.audit_status = 1
  AND c.status = 1
  AND (c.parent_id IS NULL OR (p.audit_status = 1 AND p.status = 1));

-- 2. 小说评分汇总视图：以 novel_rating_stat 为唯一权威来源
CREATE OR REPLACE VIEW v_novel_rating_summary AS
SELECT
  n.novel_id,
  n.title,
  n.author_id,
  COALESCE(s.rating_count, 0)   AS rating_count,
  COALESCE(s.score_total, 0)    AS score_total,
  COALESCE(s.avg_score, 0.00)   AS avg_score
FROM novel n
LEFT JOIN novel_rating_stat s ON s.novel_id = n.novel_id;

-- 3. 榜单条目明细视图
CREATE OR REPLACE VIEW v_ranking_item_detail AS
SELECT
  i.item_id,
  c.ranking_id,
  c.ranking_code,
  c.ranking_name,
  c.ranking_type,
  c.period_type,
  i.period_no,
  i.rank_no,
  i.novel_id,
  n.title AS novel_title,
  i.score,
  i.metric_snapshot,
  i.created_at
FROM ranking_item i
JOIN ranking_config c ON c.ranking_id = i.ranking_id
JOIN novel n          ON n.novel_id  = i.novel_id;

-- 4. 榜单最新期次视图（period_no 需在同一榜单内保持统一格式，字典序即时间序）
CREATE OR REPLACE VIEW v_ranking_latest AS
SELECT d.*
FROM v_ranking_item_detail d
JOIN (
  SELECT ranking_id, MAX(period_no) AS period_no
  FROM ranking_item
  GROUP BY ranking_id
) m ON m.ranking_id = d.ranking_id AND m.period_no = d.period_no;

-- =====================================================================
-- 二、存储过程
-- =====================================================================

-- 1. 重算某条评论的直接回复数
--    口径：parent_id 指向该评论、status=1 且 audit_status=1 的回复。
--    说明：MySQL 禁止触发器更新触发表自身（ERROR 1442），故 reply_count
--          由本过程在同一事务内维护；插入/审核/删除评论后调用即可。
DROP PROCEDURE IF EXISTS sp_sync_comment_reply_count;
DELIMITER $$
CREATE PROCEDURE sp_sync_comment_reply_count(IN p_comment_id BIGINT UNSIGNED)
BEGIN
  UPDATE comment c
     SET c.reply_count = (
       SELECT t.cnt FROM (
         SELECT COUNT(*) AS cnt
         FROM comment r
         WHERE r.parent_id = p_comment_id
           AND r.status = 1
           AND r.audit_status = 1
       ) t
     )
   WHERE c.comment_id = p_comment_id;
END$$
DELIMITER ;

-- 2. 按明细重算某本小说的评分统计（用于对账、历史数据回填）
DROP PROCEDURE IF EXISTS sp_refresh_rating_stat;
DELIMITER $$
CREATE PROCEDURE sp_refresh_rating_stat(IN p_novel_id BIGINT UNSIGNED)
BEGIN
  DECLARE v_count INT DEFAULT 0;
  DECLARE v_total BIGINT DEFAULT 0;
  DECLARE v_avg   DECIMAL(3,2) DEFAULT 0.00;

  SELECT COUNT(*), COALESCE(SUM(score), 0)
    INTO v_count, v_total
    FROM novel_rating
   WHERE novel_id = p_novel_id
     AND status = 1;

  SET v_avg = IF(v_count = 0, 0.00, ROUND(v_total / v_count, 2));

  IF EXISTS (SELECT 1 FROM novel_rating_stat WHERE novel_id = p_novel_id) THEN
    UPDATE novel_rating_stat
       SET rating_count = v_count,
           score_total  = v_total,
           avg_score    = v_avg
     WHERE novel_id = p_novel_id;
  ELSE
    INSERT INTO novel_rating_stat (novel_id, rating_count, score_total, avg_score)
    VALUES (p_novel_id, v_count, v_total, v_avg);
  END IF;
END$$
DELIMITER ;

-- 3. 生成榜单期次快照
--    入参：p_ranking_id 榜单ID；p_period_no 期次号（如 202611）；
--          p_allow_history_overwrite 历史覆盖修复标记，常规生成必须为 0；
--          p_item_count 出参，写入条数。
--    口径：指标为生成时刻的累计值；score = 指标值 × weight_config 中对应权重（缺省 1.0）。
--          FAVORITE 取 user_bookshelf 收藏数；RATING 取 novel_rating_stat.avg_score；
--          TICKET 取 reward_tip 成功打赏金额；SALE 取 chapter_subscription 有效订阅实付金额。
--          READ 缺少阅读流水数据源，显式报错（见 SQL 说明）。
--          仅收录 novel.status <> 2（未下架）的作品。
--    事务：同一期次的 DELETE + INSERT 必须同时成功或同时失败。
--    历史：已有历史期次默认禁止覆盖；仅显式传入修复标记 1 时允许历史修复。
DROP PROCEDURE IF EXISTS sp_generate_ranking;
DELIMITER $$
CREATE PROCEDURE sp_generate_ranking(
  IN  p_ranking_id               BIGINT UNSIGNED,
  IN  p_period_no                VARCHAR(20),
  IN  p_allow_history_overwrite  TINYINT,
  OUT p_item_count               INT
)
BEGIN
  DECLARE v_type            VARCHAR(30) DEFAULT NULL;
  DECLARE v_period_type     VARCHAR(10) DEFAULT NULL;
  DECLARE v_active          TINYINT DEFAULT NULL;
  DECLARE v_top_n           INT DEFAULT 100;
  DECLARE v_weight          DECIMAL(10,4) DEFAULT 1.0000;
  DECLARE v_current_period  VARCHAR(20) DEFAULT NULL;
  DECLARE v_snapshot_exists INT DEFAULT 0;

  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    DROP TEMPORARY TABLE IF EXISTS tmp_ranking_metric;
    RESIGNAL;
  END;

  SET p_item_count = 0;

  IF p_period_no IS NULL OR TRIM(p_period_no) = '' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '期次号 period_no 不能为空';
  END IF;
  IF p_allow_history_overwrite IS NULL OR p_allow_history_overwrite NOT IN (0, 1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '历史覆盖修复标记必须为 0 或 1';
  END IF;

  SELECT ranking_type,
         period_type,
         is_active,
         top_n,
         COALESCE(CAST(JSON_EXTRACT(weight_config,
                  CONCAT('$.', LOWER(ranking_type))) AS DECIMAL(10,4)), 1.0000)
    INTO v_type, v_period_type, v_active, v_top_n, v_weight
    FROM ranking_config
   WHERE ranking_id = p_ranking_id;

  IF v_type IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '榜单不存在';
  END IF;
  IF v_active <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '榜单已停用，不能生成新期次';
  END IF;

  SET v_current_period = CASE v_period_type
    WHEN 'DAY' THEN DATE_FORMAT(NOW(), '%Y%m%d')
    WHEN 'WEEK' THEN DATE_FORMAT(NOW(), '%x-W%v')
    WHEN 'MONTH' THEN DATE_FORMAT(NOW(), '%Y%m')
    ELSE 'CURRENT'
  END;

  SELECT EXISTS (
    SELECT 1
      FROM ranking_item
     WHERE ranking_id = p_ranking_id
       AND period_no = p_period_no
  ) INTO v_snapshot_exists;

  IF v_snapshot_exists = 1
     AND v_period_type <> 'CURRENT'
     AND p_period_no < v_current_period
     AND p_allow_history_overwrite = 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = '历史期次默认禁止覆盖，历史修复须显式传入覆盖标记 1';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS tmp_ranking_metric;
  CREATE TEMPORARY TABLE tmp_ranking_metric (
    novel_id     BIGINT UNSIGNED NOT NULL,
    metric_value DECIMAL(12,4)   NOT NULL,
    PRIMARY KEY (novel_id)
  ) ENGINE = InnoDB;

  IF v_type = 'FAVORITE' THEN
    INSERT INTO tmp_ranking_metric (novel_id, metric_value)
    SELECT b.novel_id, COUNT(*)
      FROM user_bookshelf b
     GROUP BY b.novel_id;
  ELSEIF v_type = 'RATING' THEN
    INSERT INTO tmp_ranking_metric (novel_id, metric_value)
    SELECT s.novel_id, s.avg_score
      FROM novel_rating_stat s
     WHERE s.rating_count > 0;
  ELSEIF v_type = 'TICKET' THEN
    INSERT INTO tmp_ranking_metric (novel_id, metric_value)
    SELECT t.novel_id, SUM(t.amount)
      FROM reward_tip t
     WHERE t.status = 'SUCCESS'
     GROUP BY t.novel_id;
  ELSEIF v_type = 'SALE' THEN
    INSERT INTO tmp_ranking_metric (novel_id, metric_value)
    SELECT cs.novel_id, SUM(cs.paid_amount)
      FROM chapter_subscription cs
     WHERE cs.status = 'ACTIVE'
     GROUP BY cs.novel_id;
  ELSE
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'READ 榜单缺少阅读流水数据源，暂不支持自动生成';
  END IF;

  -- 删除旧数据与插入新数据在同一事务中，任一失败即整体回滚。
  START TRANSACTION;

  DELETE FROM ranking_item
   WHERE ranking_id = p_ranking_id AND period_no = p_period_no;

  INSERT INTO ranking_item (ranking_id, period_no, novel_id, rank_no, score, metric_snapshot)
  SELECT p_ranking_id,
         p_period_no,
         t.novel_id,
         t.rn,
         t.score,
         JSON_OBJECT(LOWER(v_type), t.metric_value)
  FROM (
    SELECT x.novel_id,
           x.metric_value,
           ROUND(x.metric_value * v_weight, 4) AS score,
           ROW_NUMBER() OVER (ORDER BY x.metric_value * v_weight DESC, x.novel_id ASC) AS rn
      FROM tmp_ranking_metric x
      JOIN novel n ON n.novel_id = x.novel_id AND n.status <> 2
  ) t
  WHERE t.rn <= v_top_n;

  SET p_item_count = ROW_COUNT();

  COMMIT;

  DROP TEMPORARY TABLE IF EXISTS tmp_ranking_metric;
END$$
DELIMITER ;

-- 常规生成或当前期次重建：
-- CALL sp_generate_ranking(1, '202611', 0, @cnt);
-- 明确执行历史修复时才允许覆盖已有历史期次：
-- CALL sp_generate_ranking(1, '202601', 1, @cnt);

-- 4. 查询榜单
--    入参：p_ranking_code 榜单编码；p_period_no 期次（NULL/空 取最新）；p_limit 返回条数
DROP PROCEDURE IF EXISTS sp_get_ranking;
DELIMITER $$
CREATE PROCEDURE sp_get_ranking(
  IN p_ranking_code VARCHAR(50),
  IN p_period_no    VARCHAR(20),
  IN p_limit        INT
)
BEGIN
  DECLARE v_ranking_id BIGINT UNSIGNED DEFAULT NULL;
  DECLARE v_period     VARCHAR(20) DEFAULT NULL;
  DECLARE v_limit      INT DEFAULT 100;

  SELECT ranking_id INTO v_ranking_id
    FROM ranking_config
   WHERE ranking_code = p_ranking_code
     AND is_active = 1;

  IF v_ranking_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '榜单不存在或已停用';
  END IF;

  IF p_period_no IS NULL OR TRIM(p_period_no) = '' THEN
    SELECT MAX(period_no) INTO v_period
      FROM ranking_item
     WHERE ranking_id = v_ranking_id;
  ELSE
    SET v_period = p_period_no;
  END IF;

  IF v_period IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '该榜单暂无期次数据';
  END IF;

  SET v_limit = IF(p_limit IS NULL OR p_limit <= 0, 100, p_limit);

  SELECT i.rank_no,
         i.novel_id,
         n.title AS novel_title,
         i.score,
         i.metric_snapshot,
         i.period_no,
         i.created_at
    FROM ranking_item i
    JOIN novel n ON n.novel_id = i.novel_id
   WHERE i.ranking_id = v_ranking_id
     AND i.period_no = v_period
   ORDER BY i.rank_no
   LIMIT v_limit;
END$$
DELIMITER ;
