-- =====================================================================
-- 网络文学创作与阅读平台 · C 互动与榜单  触发器脚本
-- 文件：C_互动与榜单_触发器_v1.0_20261121.sql
-- 模块：C（互动与榜单）        负责人：C        版本：v1.0 / 2026-11-21
-- 依据：docs/current/D_数据字典_v1.0_20261120.xlsx（冻结基线）
-- 数据库：MySQL 8.0.16+   引擎：InnoDB
-- -------------------------------------------------------------------
-- 触发器清单（共 8 个）：
--   1) trg_comment_like_ai     点赞后  comment.like_count + 1
--   2) trg_comment_like_ad     取消点赞 comment.like_count - 1（下限 0）
--   3) trg_comment_bu          评论内容变更后 audit_status 重新置 0
--   4) trg_novel_rating_bi     新增短评：'' 统一存 NULL，并置待审核 0
--   5) trg_novel_rating_bu     修改短评：'' 统一存 NULL；内容变更后重新置 0
--   6) trg_novel_rating_ai     新增评分 → 增量维护 novel_rating_stat
--   7) trg_novel_rating_au     修改评分/状态 → 增量维护 novel_rating_stat
--   8) trg_novel_rating_ad     删除评分 → 增量维护 novel_rating_stat
-- -------------------------------------------------------------------
-- 设计说明（重要）：
--   comment.reply_count 不能由 comment 表自身触发器维护。MySQL 禁止触发器
--   更新“触发该语句的同一张表”（ERROR 1442）。评论新增、内容修改、审核和
--   状态变更统一通过写入过程执行，由私有增量辅助过程维护回复数；事务由
--   调用方管理。sp_sync_comment_reply_count 仅用于后台对账/修复，详见
--   C_互动与榜单_视图与存储过程_v1.0_20261121.sql。
--   novel_rating_stat 的计数口径：仅统计 novel_rating.status = 1，
--   不按 review_audit_status 过滤（短评待审/驳回不影响评分统计）。
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1) 评论点赞 → comment.like_count
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_comment_like_ai;
DELIMITER $$
CREATE TRIGGER trg_comment_like_ai
AFTER INSERT ON comment_like
FOR EACH ROW
BEGIN
  UPDATE comment
     SET like_count = like_count + 1
   WHERE comment_id = NEW.comment_id;
END$$
DELIMITER ;

DROP TRIGGER IF EXISTS trg_comment_like_ad;
DELIMITER $$
CREATE TRIGGER trg_comment_like_ad
AFTER DELETE ON comment_like
FOR EACH ROW
BEGIN
  UPDATE comment
     SET like_count = GREATEST(like_count - 1, 0)
   WHERE comment_id = OLD.comment_id;
END$$
DELIMITER ;

-- ---------------------------------------------------------------------
-- 2) 评论内容修改 → 审核状态重新置 0（数据字典：评论内容修改后重新置0）
--    仅修改 NEW，不更新同表其他行，符合 MySQL 触发器限制。
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_comment_bu;
DELIMITER $$
CREATE TRIGGER trg_comment_bu
BEFORE UPDATE ON comment
FOR EACH ROW
BEGIN
  IF NOT (NEW.content <=> OLD.content) THEN
    SET NEW.audit_status = 0;
  END IF;
END$$
DELIMITER ;

-- ---------------------------------------------------------------------
-- 3) 评分短评规范化与重新送审
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_novel_rating_bi;
DELIMITER $$
CREATE TRIGGER trg_novel_rating_bi
BEFORE INSERT ON novel_rating
FOR EACH ROW
BEGIN
  IF NEW.review_content = '' THEN
    SET NEW.review_content = NULL;
  END IF;
  IF NEW.review_content IS NOT NULL THEN
    SET NEW.review_audit_status = 0;   -- 新增短评一律待审核
  ELSE
    SET NEW.review_audit_status = NULL; -- 无短评
  END IF;
END$$
DELIMITER ;

DROP TRIGGER IF EXISTS trg_novel_rating_bu;
DELIMITER $$
CREATE TRIGGER trg_novel_rating_bu
BEFORE UPDATE ON novel_rating
FOR EACH ROW
BEGIN
  IF NOT (NEW.review_content <=> OLD.review_content) THEN
    IF NEW.review_content = '' THEN
      SET NEW.review_content = NULL;
    END IF;
    IF NEW.review_content IS NULL THEN
      SET NEW.review_audit_status = NULL;
    ELSE
      SET NEW.review_audit_status = 0;  -- 短评修改后重新置 0
    END IF;
  END IF;
END$$
DELIMITER ;

-- ---------------------------------------------------------------------
-- 4) 评分统计增量维护（仅 status = 1 计入）
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_novel_rating_ai;
DELIMITER $$
CREATE TRIGGER trg_novel_rating_ai
AFTER INSERT ON novel_rating
FOR EACH ROW
BEGIN
  IF NEW.status = 1 THEN
    INSERT INTO novel_rating_stat (novel_id, rating_count, score_total, avg_score)
    VALUES (NEW.novel_id, 1, NEW.score, NEW.score)
    ON DUPLICATE KEY UPDATE
      rating_count = rating_count + 1,
      score_total  = score_total + NEW.score,
      avg_score    = ROUND(score_total / rating_count, 2);
  END IF;
END$$
DELIMITER ;

DROP TRIGGER IF EXISTS trg_novel_rating_au;
DELIMITER $$
CREATE TRIGGER trg_novel_rating_au
AFTER UPDATE ON novel_rating
FOR EACH ROW
BEGIN
  -- 先扣减旧贡献（旧记录为有效评分时）
  IF OLD.status = 1 THEN
    UPDATE novel_rating_stat
       SET rating_count = GREATEST(rating_count - 1, 0),
           score_total  = GREATEST(score_total - OLD.score, 0),
           avg_score    = CASE WHEN rating_count = 0 THEN 0.00
                               ELSE ROUND(score_total / rating_count, 2) END
     WHERE novel_id = OLD.novel_id;
  END IF;

  -- 再累加新贡献（新记录为有效评分时）
  IF NEW.status = 1 THEN
    INSERT INTO novel_rating_stat (novel_id, rating_count, score_total, avg_score)
    VALUES (NEW.novel_id, 1, NEW.score, NEW.score)
    ON DUPLICATE KEY UPDATE
      rating_count = rating_count + 1,
      score_total  = score_total + NEW.score,
      avg_score    = ROUND(score_total / rating_count, 2);
  END IF;
END$$
DELIMITER ;

DROP TRIGGER IF EXISTS trg_novel_rating_ad;
DELIMITER $$
CREATE TRIGGER trg_novel_rating_ad
AFTER DELETE ON novel_rating
FOR EACH ROW
BEGIN
  IF OLD.status = 1 THEN
    UPDATE novel_rating_stat
       SET rating_count = GREATEST(rating_count - 1, 0),
           score_total  = GREATEST(score_total - OLD.score, 0),
           avg_score    = CASE WHEN rating_count = 0 THEN 0.00
                               ELSE ROUND(score_total / rating_count, 2) END
     WHERE novel_id = OLD.novel_id;
  END IF;
END$$
DELIMITER ;
