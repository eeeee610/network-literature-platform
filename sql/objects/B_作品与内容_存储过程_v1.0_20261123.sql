-- =====================================================================
-- 网络文学创作与阅读平台 · B 作品与内容  存储过程
-- 文件：B_作品与内容_存储过程_v1.0_20261123.sql
-- 模块：B（作品与内容）      版本：v1.0 / 2026-11-23
-- ---------------------------------------------------------------------
-- 过程清单（4 个）：
--   sp_create_novel         建书（校验作者身份与标签，写 novel + novel_tag）
--   sp_save_chapter_draft   新建章节草稿 / 修改章节（改稿重算字数；
--                           已发布章节改稿自动下线并重新待审）
--   sp_publish_chapter      发布章节（须 audit_status=1；累加 total_words、
--                           刷新 latest_chapter_id、写 publish_time）
--   sp_offline_chapter      下架章节（status=2；回减统计；禁止物理删除）
-- 约定：
--   * 锁顺序统一：先 novel 行 FOR UPDATE，再 chapter 行，避免死锁。
--   * 业务校验失败：SIGNAL SQLSTATE '45000'（错误号 1644，后端映射 409）。
--   * 唯一键冲突 1062：回滚后转为友好提示；其它异常 ROLLBACK + RESIGNAL。
--   * 过程内自行管理事务，调用方（ProcRepository）不得再包 @Transactional。
--   * total_words 仅统计 status=1 的已发布章节。
-- =====================================================================

SET NAMES utf8mb4;
USE novel_platform;

-- ---------------------------------------------------------------------
-- 1. sp_create_novel
--    p_tag_ids 为 JSON 数组，如 JSON_ARRAY(1,2)；元素须为已启用标签。
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_create_novel;
DELIMITER $$
CREATE PROCEDURE sp_create_novel(
  IN  p_author_id   BIGINT UNSIGNED,
  IN  p_title       VARCHAR(120),
  IN  p_cover_url   VARCHAR(255),
  IN  p_description VARCHAR(1000),
  IN  p_tag_ids     JSON,
  OUT p_novel_id    BIGINT UNSIGNED
)
BEGIN
  DECLARE v_cnt      INT DEFAULT 0;
  DECLARE v_len      INT DEFAULT 0;
  DECLARE v_i        INT DEFAULT 0;
  DECLARE v_tag_id   BIGINT UNSIGNED;
  DECLARE EXIT HANDLER FOR 1062
  BEGIN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '标签参数重复或存在数据冲突';
  END;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  START TRANSACTION;

  IF p_title IS NULL OR CHAR_LENGTH(TRIM(p_title)) = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '书名不能为空';
  END IF;

  -- 作者必须存在、角色为 AUTHOR 且账号正常
  SELECT COUNT(*) INTO v_cnt
  FROM app_user
  WHERE user_id = p_author_id AND role = 'AUTHOR' AND status = 'ACTIVE';
  IF v_cnt = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '作者不存在或账号无创作权限（非作者/已锁定/已禁用）';
  END IF;

  INSERT INTO novel (author_id, title, cover_url, description)
  VALUES (p_author_id, TRIM(p_title), p_cover_url, p_description);
  SET p_novel_id = LAST_INSERT_ID();

  -- 逐个校验并写入标签，sort_order 按数组顺序
  IF p_tag_ids IS NOT NULL THEN
    SET v_len = JSON_LENGTH(p_tag_ids);
    WHILE v_i < v_len DO
      SET v_tag_id = CAST(JSON_EXTRACT(p_tag_ids, CONCAT('$[', v_i, ']')) AS UNSIGNED);
      SELECT COUNT(*) INTO v_cnt FROM tag WHERE tag_id = v_tag_id AND is_active = 1;
      IF v_cnt = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '标签不存在或已停用';
      END IF;
      INSERT INTO novel_tag (novel_id, tag_id, sort_order)
      VALUES (p_novel_id, v_tag_id, v_i);
      SET v_i = v_i + 1;
    END WHILE;
  END IF;

  COMMIT;
END$$
DELIMITER ;

-- ---------------------------------------------------------------------
-- 2. sp_save_chapter_draft
--    p_chapter_id 为 NULL 表示新建草稿（自动分配 chapter_no）；
--    否则为修改。已发布章节修改会下线（status 1→0）并回减统计，
--    审核状态重新置 0；驳回章节修改后重新待审；已下架章节禁止编辑。
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_save_chapter_draft;
DELIMITER $$
CREATE PROCEDURE sp_save_chapter_draft(
  IN  p_chapter_id   BIGINT UNSIGNED,
  IN  p_novel_id     BIGINT UNSIGNED,
  IN  p_title        VARCHAR(120),
  IN  p_content      MEDIUMTEXT,
  IN  p_is_vip       TINYINT,
  IN  p_vip_price    DECIMAL(10,2),
  OUT p_chapter_id_out BIGINT UNSIGNED
)
BEGIN
  DECLARE v_novel_id   BIGINT UNSIGNED;
  DECLARE v_old_status TINYINT;
  DECLARE v_old_audit  TINYINT;
  DECLARE v_old_words  INT UNSIGNED;
  DECLARE v_words      INT UNSIGNED;
  DECLARE v_chapter_no INT UNSIGNED;
  DECLARE v_cnt        INT DEFAULT 0;
  DECLARE v_lock       BIGINT UNSIGNED;
  DECLARE EXIT HANDLER FOR 1062
  BEGIN
    ROLLBACK;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '章节序号冲突，请重试';
  END;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  START TRANSACTION;

  IF p_title IS NULL OR CHAR_LENGTH(TRIM(p_title)) = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '章节标题不能为空';
  END IF;
  IF p_is_vip NOT IN (0, 1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'VIP 标记只能为 0 或 1';
  END IF;
  IF (p_is_vip = 0 AND p_vip_price <> 0.00) OR (p_is_vip = 1 AND p_vip_price <= 0.00) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'VIP 价格与 VIP 标记不一致：非 VIP 须为 0.00，VIP 须大于 0.00';
  END IF;

  SET v_words = CHAR_LENGTH(p_content);

  IF p_chapter_id IS NULL THEN
    -- 新建：锁 novel 行，保证同书并发分配 chapter_no 不冲突
    SELECT COUNT(*) INTO v_cnt FROM novel WHERE novel_id = p_novel_id;
    IF v_cnt = 0 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '小说不存在';
    END IF;
    SELECT novel_id INTO v_novel_id FROM novel WHERE novel_id = p_novel_id FOR UPDATE;

    SELECT IFNULL(MAX(chapter_no), 0) + 1 INTO v_chapter_no
    FROM chapter WHERE novel_id = p_novel_id;

    INSERT INTO chapter (novel_id, chapter_no, title, content, is_vip, vip_price,
                         word_count, status, audit_status)
    VALUES (p_novel_id, v_chapter_no, p_title, p_content, p_is_vip, p_vip_price,
            v_words, 0, NULL);
    SET p_chapter_id_out = LAST_INSERT_ID();
  ELSE
    -- 修改：先定位 novel_id，再按 novel → chapter 顺序加锁
    SET v_novel_id = NULL;
    SELECT novel_id INTO v_novel_id FROM chapter WHERE chapter_id = p_chapter_id;
    IF v_novel_id IS NULL THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '章节不存在';
    END IF;
    SELECT novel_id INTO v_lock FROM novel WHERE novel_id = v_novel_id FOR UPDATE;
    SELECT status, audit_status, word_count
      INTO v_old_status, v_old_audit, v_old_words
    FROM chapter WHERE chapter_id = p_chapter_id FOR UPDATE;

    IF v_old_status = 2 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '已下架章节不可编辑，请新建章节';
    END IF;

    -- 已发布章节改稿：先回减统计并下线
    IF v_old_status = 1 THEN
      UPDATE chapter SET status = 0 WHERE chapter_id = p_chapter_id;
      UPDATE novel
      SET total_words = GREATEST(total_words - v_old_words, 0),
          latest_chapter_id = (
            SELECT t.cid FROM (
              SELECT chapter_id AS cid FROM chapter
              WHERE novel_id = v_novel_id AND status = 1
              ORDER BY chapter_no DESC LIMIT 1
            ) t
          )
      WHERE novel_id = v_novel_id;
    END IF;

    UPDATE chapter
    SET title        = p_title,
        content      = p_content,
        is_vip       = p_is_vip,
        vip_price    = p_vip_price,
        word_count   = v_words,
        audit_status = IF(v_old_audit IS NULL, NULL, 0)
    WHERE chapter_id = p_chapter_id;
    SET p_chapter_id_out = p_chapter_id;
  END IF;

  COMMIT;
END$$
DELIMITER ;

-- ---------------------------------------------------------------------
-- 3. sp_publish_chapter  发布（审核与发布分离：仅审核通过可发布）
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_publish_chapter;
DELIMITER $$
CREATE PROCEDURE sp_publish_chapter(IN p_chapter_id BIGINT UNSIGNED)
BEGIN
  DECLARE v_novel_id BIGINT UNSIGNED;
  DECLARE v_status   TINYINT;
  DECLARE v_audit    TINYINT;
  DECLARE v_words    INT UNSIGNED;
  DECLARE v_lock     BIGINT UNSIGNED;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  START TRANSACTION;

  SET v_novel_id = NULL;
  SELECT novel_id INTO v_novel_id FROM chapter WHERE chapter_id = p_chapter_id;
  IF v_novel_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '章节不存在';
  END IF;

  SELECT novel_id INTO v_lock FROM novel WHERE novel_id = v_novel_id FOR UPDATE;
  SELECT status, audit_status, word_count INTO v_status, v_audit, v_words
  FROM chapter WHERE chapter_id = p_chapter_id FOR UPDATE;

  IF v_audit IS NULL OR v_audit <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '章节未通过审核，不能发布';
  END IF;
  IF v_status = 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '章节已发布，请勿重复发布';
  END IF;

  UPDATE chapter SET status = 1, publish_time = NOW() WHERE chapter_id = p_chapter_id;

  UPDATE novel
  SET total_words = total_words + v_words,
      latest_chapter_id = (
        SELECT t.cid FROM (
          SELECT chapter_id AS cid FROM chapter
          WHERE novel_id = v_novel_id AND status = 1
          ORDER BY chapter_no DESC LIMIT 1
        ) t
      )
  WHERE novel_id = v_novel_id;

  COMMIT;
END$$
DELIMITER ;

-- ---------------------------------------------------------------------
-- 4. sp_offline_chapter  下架（不物理删除；已发布章节回减统计）
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_offline_chapter;
DELIMITER $$
CREATE PROCEDURE sp_offline_chapter(IN p_chapter_id BIGINT UNSIGNED)
BEGIN
  DECLARE v_novel_id BIGINT UNSIGNED;
  DECLARE v_status   TINYINT;
  DECLARE v_words    INT UNSIGNED;
  DECLARE v_lock     BIGINT UNSIGNED;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  START TRANSACTION;

  SET v_novel_id = NULL;
  SELECT novel_id INTO v_novel_id FROM chapter WHERE chapter_id = p_chapter_id;
  IF v_novel_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '章节不存在';
  END IF;

  SELECT novel_id INTO v_lock FROM novel WHERE novel_id = v_novel_id FOR UPDATE;
  SELECT status, word_count INTO v_status, v_words
  FROM chapter WHERE chapter_id = p_chapter_id FOR UPDATE;

  IF v_status = 2 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '章节已处于下架状态';
  END IF;

  UPDATE chapter SET status = 2 WHERE chapter_id = p_chapter_id;

  IF v_status = 1 THEN
    UPDATE novel
    SET total_words = GREATEST(total_words - v_words, 0),
        latest_chapter_id = (
          SELECT t.cid FROM (
            SELECT chapter_id AS cid FROM chapter
            WHERE novel_id = v_novel_id AND status = 1
            ORDER BY chapter_no DESC LIMIT 1
          ) t
        )
    WHERE novel_id = v_novel_id;
  END IF;

  COMMIT;
END$$
DELIMITER ;

-- =====================================================================
-- 冒烟自测（P0 验证）
-- 前置：已按顺序执行 A 的 app_user/user_account、B 建表脚本。
-- 反向用例（预期报错）默认注释，请单独取消注释逐条执行验证。
-- =====================================================================

-- 准备：1 个作者 + 2 个标签
INSERT INTO app_user (user_id, username, password_hash, nickname, role)
VALUES (10001, 'author_b_test', '$2a$10$b_smoke_test_hash_0123456789', 'B冒烟作者', 'AUTHOR');
INSERT INTO tag (tag_name) VALUES ('玄幻'), ('都市');
SET @t1 = (SELECT tag_id FROM tag WHERE tag_name = '玄幻');
SET @t2 = (SELECT tag_id FROM tag WHERE tag_name = '都市');

-- S01 建书（含 2 个标签）
CALL sp_create_novel(10001, 'B冒烟测试书', NULL, '用于P0验证', JSON_ARRAY(@t1, @t2), @nid);
SELECT @nid AS novel_id;
SELECT * FROM novel_tag WHERE novel_id = @nid;   -- 应 2 行，sort_order 0/1

-- S02 新建两章草稿
CALL sp_save_chapter_draft(NULL, @nid, '第1章', '第一章正文内容', 0, 0.00, @c1);
CALL sp_save_chapter_draft(NULL, @nid, '第2章', '第二章正文内容更长一些', 1, 0.50, @c2);
SELECT chapter_id, chapter_no, status, audit_status, word_count
FROM chapter WHERE novel_id = @nid;              -- 草稿：status=0，audit_status=NULL

-- 【反向】R01 未审核直接发布，应报错“章节未通过审核，不能发布”：
-- CALL sp_publish_chapter(@c1);

-- S03 模拟 D 审核通过后发布
UPDATE chapter SET audit_status = 1 WHERE chapter_id IN (@c1, @c2);
CALL sp_publish_chapter(@c1);
CALL sp_publish_chapter(@c2);
SELECT total_words, latest_chapter_id FROM novel WHERE novel_id = @nid;
-- 期望：total_words = 两章字数和，latest_chapter_id = @c2

-- 【反向】R02 重复发布，应报错“请勿重复发布”：
-- CALL sp_publish_chapter(@c1);

-- S04 下架第 2 章，统计应回减，latest_chapter_id 应回到 @c1
CALL sp_offline_chapter(@c2);
SELECT total_words, latest_chapter_id FROM novel WHERE novel_id = @nid;

-- S05 已发布章节改稿：自动下线、字数回减、重新待审
CALL sp_save_chapter_draft(@c1, @nid, '第1章（修订）', '第一章正文内容修订版', 0, 0.00, @c1b);
SELECT chapter_id, status, audit_status, word_count FROM chapter WHERE chapter_id = @c1;
SELECT total_words, latest_chapter_id FROM novel WHERE novel_id = @nid;
-- 期望：status=0，audit_status=0，total_words=0，latest_chapter_id=NULL

-- 【反向】R03 重复章节序号（绕过过程直接 INSERT），应触发 1062：
-- INSERT INTO chapter (novel_id, chapter_no, title, content)
-- VALUES (@nid, 1, '冲突章', 'x');

-- 【反向】R04 非法标签建书，应报错且回滚、不产生 novel：
-- CALL sp_create_novel(10001, '坏书', NULL, NULL, JSON_ARRAY(999999), @bad);

-- 【反向】R05 非作者建书（role 非 AUTHOR），应报错：
-- INSERT INTO app_user (user_id, username, password_hash, nickname, role)
-- VALUES (10002, 'reader_b_test', '$2a$10$b_smoke_test_hash_0123456789', 'B冒烟读者', 'READER');
-- CALL sp_create_novel(10002, '读者的书', NULL, NULL, NULL, @bad2);

-- 【反向】R06 编辑已下架章节，应报错：
-- CALL sp_save_chapter_draft(@c2, @nid, 'x', 'x', 0, 0.00, @tmp);

-- 收尾核对：4 个过程应存在
SELECT ROUTINE_NAME FROM information_schema.ROUTINES
WHERE ROUTINE_SCHEMA = 'novel_platform' AND ROUTINE_TYPE = 'PROCEDURE'
  AND ROUTINE_NAME LIKE 'sp\_%';
-- =====================================================================
