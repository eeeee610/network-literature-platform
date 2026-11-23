-- =====================================================================
-- 网络文学创作与阅读平台 · B 作品与内容  索引脚本（核验 + EXPLAIN 验证）
-- 文件：B_作品与内容_索引_v1.0_20261123.sql
-- 模块：B（作品与内容）      版本：v1.0 / 2026-11-23
-- ---------------------------------------------------------------------
-- 说明：
--   1. 与 C/D 保持一致，全部索引已随《B_作品与内容_建表_v1.0_20261123.sql》
--      在建表时一并创建（见下方"索引清单核验"）。本脚本用于：
--        a) 核验索引是否齐全、唯一约束是否生效；
--        b) 提供旧版（v0.1）到 v0.2 的增量升级语句（默认注释，按需启用）；
--        c) 提供 7 个 EXPLAIN 验证用例（小数据量可能全表扫描，
--           需先执行造数脚本，W14 性能测试以本文件用例为准）。
--   2. 索引设计要点：
--      idx_author_update(author_id,updated_at) 左前缀兼作 fk_novel_author
--      外键索引；uk_novel_chapter(novel_id,chapter_no) 兼作 fk_chapter_novel
--      外键索引；列表查询一律不 SELECT chapter.content。
-- =====================================================================

SET NAMES utf8mb4;
USE novel_platform;

-- ---------------------------------------------------------------------
-- 一、索引清单核验（应对照字段清单 v0.2「索引清单」13 条）
-- ---------------------------------------------------------------------
SELECT TABLE_NAME, INDEX_NAME, NON_UNIQUE, SEQ_IN_INDEX, COLUMN_NAME
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'novel_platform'
  AND TABLE_NAME IN ('novel','chapter','tag','novel_tag')
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

-- 唯一约束必须 Non_unique = 0
SHOW INDEX FROM chapter WHERE Key_name = 'uk_novel_chapter';
SHOW INDEX FROM tag     WHERE Key_name = 'uk_tag_name';

-- ---------------------------------------------------------------------
-- 二、v0.1 → v0.2 增量升级（仅当库中存在 v0.1 旧表时取消注释执行；
--     全新建库请忽略本段）
-- ---------------------------------------------------------------------
-- ALTER TABLE novel  ADD COLUMN audit_status TINYINT NOT NULL DEFAULT 0
--   COMMENT '审核状态；0 待审核 1 通过 2 驳回' AFTER status;
-- ALTER TABLE chapter ADD COLUMN audit_status TINYINT NULL DEFAULT NULL
--   COMMENT '审核状态；NULL 未提交 0 待审核 1 通过 2 驳回' AFTER status;
-- ALTER TABLE novel  DROP COLUMN avg_score, DROP COLUMN rating_count;
-- ALTER TABLE tag     MODIFY COLUMN tag_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT;
-- ALTER TABLE novel_tag MODIFY COLUMN tag_id BIGINT UNSIGNED NOT NULL;
-- ALTER TABLE novel  DROP INDEX idx_author,
--   ADD KEY idx_author_update (author_id, updated_at),
--   ADD KEY idx_novel_audit (audit_status, updated_at);
-- ALTER TABLE chapter DROP INDEX idx_catalog,
--   ADD KEY idx_catalog (novel_id, status, audit_status, chapter_no),
--   ADD KEY idx_chapter_audit (audit_status, updated_at);

-- ---------------------------------------------------------------------
-- 三、统计信息更新（造数后执行，保证 EXPLAIN 反映真实基数）
-- ---------------------------------------------------------------------
-- ANALYZE TABLE novel, chapter, tag, novel_tag;

-- ---------------------------------------------------------------------
-- 四、EXPLAIN 验证用例（参数为示例，替换为造数后的真实值）
-- ---------------------------------------------------------------------

-- T01 书城连载列表：按最近更新排序，期望 key=idx_status_update，无 filesort
EXPLAIN
SELECT novel_id, title, cover_url, total_words, latest_chapter_id, updated_at
FROM novel
WHERE status = 0 AND audit_status = 1
ORDER BY updated_at DESC
LIMIT 20;

-- T02 书名前缀搜索：期望 key=idx_title，type=range
EXPLAIN
SELECT novel_id, title, cover_url
FROM novel
WHERE title LIKE '斗破%' AND audit_status = 1 AND status IN (0,1)
ORDER BY title
LIMIT 20;

-- T03 作者创作台列表：期望 key=idx_author_update，无 filesort
EXPLAIN
SELECT novel_id, title, status, audit_status, total_words, updated_at
FROM novel
WHERE author_id = 10001
ORDER BY updated_at DESC
LIMIT 20;

-- T04 目录页（只列已发布且审核通过章节，不取 content）：
--     期望 key=idx_catalog，覆盖查询条件与排序
EXPLAIN
SELECT chapter_id, chapter_no, title, is_vip, vip_price, word_count, publish_time
FROM chapter
WHERE novel_id = 1 AND status = 1 AND audit_status = 1
ORDER BY chapter_no
LIMIT 50;

-- T05 标签反查作品：期望 novel_tag 走 idx_tag_reverse，novel 走 PRIMARY
EXPLAIN
SELECT n.novel_id, n.title, n.cover_url
FROM novel_tag nt
JOIN novel n ON n.novel_id = nt.novel_id
WHERE nt.tag_id = 1 AND n.audit_status = 1 AND n.status IN (0,1)
ORDER BY nt.sort_order
LIMIT 20;

-- T06 后台待审小说列表：期望 key=idx_novel_audit
EXPLAIN
SELECT novel_id, title, author_id, updated_at
FROM novel
WHERE audit_status = 0
ORDER BY updated_at
LIMIT 20;

-- T07 后台待审章节列表：期望 key=idx_chapter_audit（NULL 草稿不进结果）
EXPLAIN
SELECT chapter_id, novel_id, chapter_no, title, updated_at
FROM chapter
WHERE audit_status = 0
ORDER BY updated_at
LIMIT 20;

-- ---------------------------------------------------------------------
-- 五、阅读页取正文（主键点查，content 仅在单行详情读取）
-- ---------------------------------------------------------------------
EXPLAIN
SELECT chapter_id, title, content, is_vip, vip_price, word_count
FROM chapter
WHERE chapter_id = 1;
-- =====================================================================
