-- =====================================================================
-- B 作品与内容 · 存储过程冒烟测试 v1.0
-- 版本：v1.0 / 2026-11-23
-- 前置（按顺序执行）：
--   1) sql/ddl/A_用户交易_建表_v1.1_20261123.sql
--   2) sql/ddl/B_作品与内容_建表_v1.0_20261123.sql
--   3) sql/objects/B_作品与内容_存储过程_v1.0_20261123.sql
-- 说明：本文件仅用于本地/测试库验证，不参与正式对象部署；
--       反向用例默认注释，请逐条取消注释执行。
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
