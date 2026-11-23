-- =====================================================================
-- 网络文学创作与阅读平台 · C 互动与榜单  视图与存储过程脚本
-- 文件：C_互动与榜单_视图与存储过程_v1.0_20261121.sql
-- 模块：C（互动与榜单）        负责人：C        版本：v1.0 / 2026-11-21
-- 依据：docs/current/D_数据字典_v1.0_20261120.xlsx（冻结基线）
-- 数据库：MySQL 8.0.16+
-- -------------------------------------------------------------------
-- 对象清单：
--   视图 4 个
--     v_comment_visible        评论展示视图（自身及一级父评论审核通过、状态正常）
--     v_novel_rating_summary   小说评分汇总视图（唯一权威评分统计）
--     v_ranking_item_detail    榜单条目明细视图（联表书名与榜单元信息）
--     v_ranking_latest         榜单最新期次视图
--   存储过程 9 个
--     sp_sync_comment_reply_count  回复数全量重算（仅对账/修复）
--     sp_apply_comment_reply_delta 回复自身可见性迁移的私有增量辅助过程
--     sp_create_comment            新增评论/回复（初始待审核）
--     sp_edit_comment_content      修改评论内容并自动重新送审
--     sp_audit_comment             审核状态变更并自动维护回复数
--     sp_set_comment_status        正常/隐藏/恢复/软删除并自动维护回复数
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

-- 1. 评论展示视图：仅支持一级评论和直接回复。
--    评论自身须审核通过、状态正常；直接回复的一级父评论也须满足相同条件。
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
  AND (
    c.parent_id IS NULL
    OR (p.parent_id IS NULL AND p.audit_status = 1 AND p.status = 1)
  );

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

-- 1. 全量重算某条评论的直接回复数（仅后台对账/修复）
--    口径：parent_id 指向该评论、status=1 且 audit_status=1 的回复。
--    正常业务不得显式调用本过程；各评论写入过程会自动、同事务维护计数。
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

-- 2. 回复自身可见性迁移的私有增量辅助过程。
--    父评论创建后不再变更，仅处理当前回复对其一级父评论的贡献。
DROP PROCEDURE IF EXISTS sp_apply_comment_reply_delta;
DELIMITER $$
CREATE PROCEDURE sp_apply_comment_reply_delta(
  IN p_parent_id  BIGINT UNSIGNED,
  IN p_old_status TINYINT,
  IN p_old_audit  TINYINT,
  IN p_new_status TINYINT,
  IN p_new_audit  TINYINT
)
BEGIN
  DECLARE v_old_visible BOOLEAN DEFAULT FALSE;
  DECLARE v_new_visible BOOLEAN DEFAULT FALSE;
  DECLARE v_lock_id     BIGINT UNSIGNED DEFAULT NULL;

  SET v_old_visible = p_parent_id IS NOT NULL AND p_old_status = 1 AND p_old_audit = 1;
  SET v_new_visible = p_parent_id IS NOT NULL AND p_new_status = 1 AND p_new_audit = 1;

  -- 计数更新与评论操作由调用方放在同一事务中。
  IF v_old_visible AND NOT v_new_visible THEN
    SELECT comment_id INTO v_lock_id FROM comment WHERE comment_id = p_parent_id FOR UPDATE;
    UPDATE comment
       SET reply_count = reply_count - 1
     WHERE comment_id = p_parent_id;
  ELSEIF v_new_visible AND NOT v_old_visible THEN
    SELECT comment_id INTO v_lock_id FROM comment WHERE comment_id = p_parent_id FOR UPDATE;
    UPDATE comment
       SET reply_count = reply_count + 1
     WHERE comment_id = p_parent_id;
  END IF;
END$$
DELIMITER ;

-- 3. 新增评论/回复：仅允许一级评论和直接回复，统一初始待审核。
--    小说、章节及父子归属在插入前校验；事务由调用方管理。
DROP PROCEDURE IF EXISTS sp_create_comment;
DELIMITER $$
CREATE PROCEDURE sp_create_comment(
  IN  p_novel_id        BIGINT UNSIGNED,
  IN  p_chapter_id      BIGINT UNSIGNED,
  IN  p_user_id         BIGINT UNSIGNED,
  IN  p_parent_id       BIGINT UNSIGNED,
  IN  p_reply_to_user_id BIGINT UNSIGNED,
  IN  p_content         VARCHAR(1000),
  OUT p_comment_id      BIGINT UNSIGNED
)
BEGIN
  DECLARE v_parent_id          BIGINT UNSIGNED DEFAULT NULL;
  DECLARE v_parent_parent_id   BIGINT UNSIGNED DEFAULT NULL;
  DECLARE v_parent_novel_id    BIGINT UNSIGNED DEFAULT NULL;
  DECLARE v_parent_chapter_id  BIGINT UNSIGNED DEFAULT NULL;
  DECLARE v_chapter_novel_id   BIGINT UNSIGNED DEFAULT NULL;

  SET p_comment_id = NULL;

  IF p_chapter_id IS NOT NULL THEN
    SELECT novel_id INTO v_chapter_novel_id
      FROM chapter
     WHERE chapter_id = p_chapter_id;
    IF v_chapter_novel_id IS NULL THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '章节不存在';
    END IF;
    IF v_chapter_novel_id <> p_novel_id THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '章节必须属于评论对应的小说';
    END IF;
  END IF;

  IF p_parent_id IS NOT NULL THEN
    SELECT comment_id, parent_id, novel_id, chapter_id
      INTO v_parent_id, v_parent_parent_id, v_parent_novel_id, v_parent_chapter_id
      FROM comment
     WHERE comment_id = p_parent_id
     FOR UPDATE;
    IF v_parent_id IS NULL THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '父评论不存在';
    END IF;
    IF v_parent_parent_id IS NOT NULL THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '仅允许一级评论和直接回复，不允许继续嵌套';
    END IF;
    IF v_parent_novel_id <> p_novel_id
       OR NOT (v_parent_chapter_id <=> p_chapter_id) THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '回复与父评论的小说、章节必须严格一致';
    END IF;
  END IF;

  INSERT INTO comment (
    novel_id, chapter_id, user_id, parent_id, reply_to_user_id,
    content, status, audit_status
  ) VALUES (
    p_novel_id, p_chapter_id, p_user_id, p_parent_id, p_reply_to_user_id,
    p_content, 1, 0
  );
  SET p_comment_id = LAST_INSERT_ID();
END$$
DELIMITER ;

-- 4. 修改内容：UPDATE 后重读实际状态，兼容 trg_comment_bu 的重新送审逻辑。
DROP PROCEDURE IF EXISTS sp_edit_comment_content;
DELIMITER $$
CREATE PROCEDURE sp_edit_comment_content(
  IN p_comment_id BIGINT UNSIGNED,
  IN p_content    VARCHAR(1000)
)
BEGIN
  DECLARE v_parent_id  BIGINT UNSIGNED DEFAULT NULL;
  DECLARE v_old_status TINYINT DEFAULT NULL;
  DECLARE v_old_audit  TINYINT DEFAULT NULL;
  DECLARE v_new_status TINYINT DEFAULT NULL;
  DECLARE v_new_audit  TINYINT DEFAULT NULL;

  SELECT parent_id, status, audit_status
    INTO v_parent_id, v_old_status, v_old_audit
    FROM comment
   WHERE comment_id = p_comment_id
   FOR UPDATE;
  IF v_old_status IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '评论不存在';
  END IF;

  UPDATE comment SET content = p_content WHERE comment_id = p_comment_id;
  SELECT status, audit_status
    INTO v_new_status, v_new_audit
    FROM comment
   WHERE comment_id = p_comment_id;

  CALL sp_apply_comment_reply_delta(
    v_parent_id, v_old_status, v_old_audit, v_new_status, v_new_audit
  );
END$$
DELIMITER ;

-- 5. 审核状态变更：0 待审核、1 通过、2 驳回；重复设置不重复计数。
--    管理员鉴权及 audit_log 追加由应用层在同一事务中完成。
DROP PROCEDURE IF EXISTS sp_audit_comment;
DELIMITER $$
CREATE PROCEDURE sp_audit_comment(
  IN p_comment_id  BIGINT UNSIGNED,
  IN p_audit_status TINYINT
)
BEGIN
  DECLARE v_parent_id  BIGINT UNSIGNED DEFAULT NULL;
  DECLARE v_old_status TINYINT DEFAULT NULL;
  DECLARE v_old_audit  TINYINT DEFAULT NULL;
  DECLARE v_new_status TINYINT DEFAULT NULL;
  DECLARE v_new_audit  TINYINT DEFAULT NULL;

  IF p_audit_status IS NULL OR p_audit_status NOT IN (0, 1, 2) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '评论审核状态必须为 0、1 或 2';
  END IF;

  SELECT parent_id, status, audit_status
    INTO v_parent_id, v_old_status, v_old_audit
    FROM comment
   WHERE comment_id = p_comment_id
   FOR UPDATE;
  IF v_old_status IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '评论不存在';
  END IF;

  UPDATE comment SET audit_status = p_audit_status WHERE comment_id = p_comment_id;
  SELECT status, audit_status
    INTO v_new_status, v_new_audit
    FROM comment
   WHERE comment_id = p_comment_id;

  CALL sp_apply_comment_reply_delta(
    v_parent_id, v_old_status, v_old_audit, v_new_status, v_new_audit
  );
END$$
DELIMITER ;

-- 6. 状态变更：1 正常、2 隐藏、3 软删除；父评论状态不级联改子回复计数。
DROP PROCEDURE IF EXISTS sp_set_comment_status;
DELIMITER $$
CREATE PROCEDURE sp_set_comment_status(
  IN p_comment_id BIGINT UNSIGNED,
  IN p_status     TINYINT
)
BEGIN
  DECLARE v_parent_id  BIGINT UNSIGNED DEFAULT NULL;
  DECLARE v_old_status TINYINT DEFAULT NULL;
  DECLARE v_old_audit  TINYINT DEFAULT NULL;
  DECLARE v_new_status TINYINT DEFAULT NULL;
  DECLARE v_new_audit  TINYINT DEFAULT NULL;

  IF p_status IS NULL OR p_status NOT IN (1, 2, 3) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '评论状态必须为 1、2 或 3';
  END IF;

  SELECT parent_id, status, audit_status
    INTO v_parent_id, v_old_status, v_old_audit
    FROM comment
   WHERE comment_id = p_comment_id
   FOR UPDATE;
  IF v_old_status IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '评论不存在';
  END IF;

  UPDATE comment SET status = p_status WHERE comment_id = p_comment_id;
  SELECT status, audit_status
    INTO v_new_status, v_new_audit
    FROM comment
   WHERE comment_id = p_comment_id;

  CALL sp_apply_comment_reply_delta(
    v_parent_id, v_old_status, v_old_audit, v_new_status, v_new_audit
  );
END$$
DELIMITER ;

-- 7. 父评论移动功能已移除；清理旧版本可能已部署的对象。
DROP PROCEDURE IF EXISTS sp_move_comment_parent;
-- 8. 按明细重算某本小说的评分统计（用于对账、历史数据回填）
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

-- 9. 生成榜单期次快照
--    入参：p_ranking_id 榜单ID；p_period_no 期次号；
--          p_allow_history_overwrite 历史覆盖修复标记，常规生成必须为 0；
--          p_item_count 出参，写入条数。
--    期次：DAILY=yyyymmdd；WEEKLY=yyyyWww（如 2026W47）；MONTHLY=yyyymm；ALL 固定为 ALL。
--    口径：指标为生成时刻的累计值；score = 指标值 × weight_config 中对应权重（缺省 1.0）。
--          FAVORITE 取 user_bookshelf 收藏数；RATING 取 novel_rating_stat.avg_score；
--          TICKET 取 reward_tip 成功打赏金额；SALE 取 chapter_subscription 有效订阅实付金额。
--          仅收录 novel.status <> 2（未下架）的作品。
--    事务：由调用方管理，同一期次的 DELETE + INSERT 须在同一事务中执行。
--    重复：当前期次及 ALL 可重复生成；非 ALL 的未来期次禁止生成。
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
  DECLARE v_period_type     VARCHAR(20) DEFAULT NULL;
  DECLARE v_active          TINYINT DEFAULT NULL;
  DECLARE v_top_n           INT DEFAULT 100;
  DECLARE v_weight          DECIMAL(10,4) DEFAULT 1.0000;
  DECLARE v_current_period  VARCHAR(20) DEFAULT NULL;
  DECLARE v_snapshot_exists INT DEFAULT 0;

  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
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
    WHEN 'DAILY' THEN DATE_FORMAT(NOW(), '%Y%m%d')
    WHEN 'WEEKLY' THEN DATE_FORMAT(NOW(), '%xW%v')
    WHEN 'MONTHLY' THEN DATE_FORMAT(NOW(), '%Y%m')
    WHEN 'ALL' THEN 'ALL'
    ELSE NULL
  END;

  IF v_current_period IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '榜单周期类型无效';
  END IF;

  -- 固定格式并验证真实日期/ISO 周/月份，避免非法期次破坏字典序比较。
  IF (v_period_type = 'DAILY' AND COALESCE(
        DATE_FORMAT(STR_TO_DATE(p_period_no, '%Y%m%d'), '%Y%m%d') = p_period_no,
        FALSE
      ) = 0)
     OR (v_period_type = 'WEEKLY' AND (
        REGEXP_LIKE(p_period_no, '^[0-9]{4}W[0-9]{2}$', 'c') = 0
        OR COALESCE(
          CAST(RIGHT(p_period_no, 2) AS UNSIGNED)
            BETWEEN 1 AND CAST(
              DATE_FORMAT(STR_TO_DATE(CONCAT(LEFT(p_period_no, 4), '1228'), '%Y%m%d'), '%v')
              AS UNSIGNED
            ),
          FALSE
        ) = 0
     ))
     OR (v_period_type = 'MONTHLY' AND REGEXP_LIKE(p_period_no, '^[0-9]{4}(0[1-9]|1[0-2])$', 'c') = 0)
     OR (v_period_type = 'ALL' AND BINARY p_period_no <> BINARY 'ALL') THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = '期次错误：DAILY 须为真实 yyyymmdd，WEEKLY 须为真实 ISO yyyyWww，MONTHLY 月份须为 01～12，ALL=ALL';
  END IF;

  IF v_period_type <> 'ALL' AND p_period_no > v_current_period THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '不允许生成未来期次';
  END IF;

  SELECT EXISTS (
    SELECT 1
      FROM ranking_item
     WHERE ranking_id = p_ranking_id
       AND period_no = p_period_no
  ) INTO v_snapshot_exists;

  -- 当前期次和 ALL 可重复生成；已有历史期次只有明确修复时才允许覆盖。
  IF v_snapshot_exists = 1
     AND v_period_type <> 'ALL'
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
      SET MESSAGE_TEXT = '不支持的榜单类型';
  END IF;

  -- 删除旧数据与插入新数据由调用方放在同一事务中，任一失败时整体回滚。
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

  DROP TEMPORARY TABLE IF EXISTS tmp_ranking_metric;
END$$
DELIMITER ;

-- 常规生成或当前期次重建（月榜示例）：
-- CALL sp_generate_ranking(1, '202611', 0, @cnt);
-- 周榜期次使用 2026W47，不使用 2026-W47；ALL 榜单固定传入 ALL：
-- CALL sp_generate_ranking(2, 'ALL', 0, @cnt);
-- 明确执行历史修复时才允许覆盖已有历史期次：
-- CALL sp_generate_ranking(1, '202601', 1, @cnt);

-- 10. 查询榜单
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
