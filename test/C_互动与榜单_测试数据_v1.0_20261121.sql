-- =====================================================================
-- 网络文学创作与阅读平台 · C 互动与榜单  测试数据与验证脚本
-- 文件：C_互动与榜单_测试数据_v1.0_20261121.sql
-- 用途：少量 INSERT + 触发器/视图/存储过程验证，可在测试库反复执行（先跑测试前置）
-- 执行顺序：
--   1) test/C_互动与榜单_测试前置_v1.0_20261121.sql
--   2) sql/ddl/C_互动与榜单_建表_v1.0_20261121.sql
--   3) sql/objects/C_互动与榜单_触发器_v1.0_20261121.sql
--   4) sql/objects/C_互动与榜单_视图与存储过程_v1.0_20261121.sql
--   5) 本脚本
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
SELECT '=== 3. 评论数据与 reply_count 维护 ===' AS step;
-- ---------------------------------------------------------------------
INSERT INTO comment (comment_id, novel_id, chapter_id, user_id, parent_id, reply_to_user_id,
                     content, audit_status, status) VALUES
  (5001, 101, 1001, 1, NULL, NULL, '第一章节奏很好，期待后续。', 1, 1),
  (5002, 101, 1001, 2, 5001, 1,    '同感，作者文笔在线。',       0, 1),
  (5003, 101, 1001, 3, 5001, 1,    '已追更。',                   1, 1),
  (5004, 101, NULL, 2, 5001, 1,    '这条被隐藏了。',             1, 2);

CALL sp_sync_comment_reply_count(5001);
SELECT '回复数：待审核与隐藏回复不计入，期望 1' AS note, comment_id, reply_count
  FROM comment WHERE comment_id = 5001;

UPDATE comment SET audit_status = 1 WHERE comment_id = 5002;
CALL sp_sync_comment_reply_count(5001);
SELECT '回复数：审核通过后重新统计，期望 2' AS note, comment_id, reply_count
  FROM comment WHERE comment_id = 5001;

UPDATE comment SET content = '同感，作者文笔很稳。' WHERE comment_id = 5002;
SELECT '内容修改后审核状态应重新置 0' AS note, comment_id, audit_status
  FROM comment WHERE comment_id = 5002;

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
INSERT INTO novel_rating (user_id, novel_id, score, status) VALUES
  (1, 101, 5, 1), (2, 101, 4, 1), (3, 101, 3, 1);
SELECT '新增 3 条评分：count=3 total=12 avg=4.00' AS note, novel_id, rating_count, score_total, avg_score
  FROM novel_rating_stat WHERE novel_id = 101;

UPDATE novel_rating SET score = 1 WHERE user_id = 3 AND novel_id = 101;
SELECT '改分后：count=3 total=10 avg=3.33' AS note, novel_id, rating_count, score_total, avg_score
  FROM novel_rating_stat WHERE novel_id = 101;

UPDATE novel_rating SET status = 2 WHERE user_id = 2 AND novel_id = 101;
SELECT '隐藏 1 条评分后：count=2 total=6 avg=3.00' AS note, novel_id, rating_count, score_total, avg_score
  FROM novel_rating_stat WHERE novel_id = 101;

DELETE FROM novel_rating WHERE user_id = 1 AND novel_id = 101;
SELECT '删除 1 条评分后：count=1 total=1 avg=1.00' AS note, novel_id, rating_count, score_total, avg_score
  FROM novel_rating_stat WHERE novel_id = 101;

CALL sp_refresh_rating_stat(101);
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

-- ---------------------------------------------------------------------
SELECT '=== 6. 榜单配置与生成 ===' AS step;
-- ---------------------------------------------------------------------
INSERT INTO ranking_config
  (ranking_id, ranking_code, ranking_name, ranking_type, period_type, top_n, weight_config) VALUES
  (1, 'FAVORITE_MONTH', '月收藏榜', 'FAVORITE', 'MONTHLY', 3, '{"favorite":1.0}'),
  (2, 'RATING_ALL',     '总评分榜', 'RATING',   'ALL',     3, '{"rating":1.0}');

CALL sp_generate_ranking(1, '202611', @cnt_fav);
SELECT '收藏榜生成条数（下架作品排除），期望 2' AS note, @cnt_fav AS item_count;

CALL sp_generate_ranking(2, '202611', @cnt_rate);
SELECT '评分榜生成条数，期望 2' AS note, @cnt_rate AS item_count;

SELECT '收藏榜最新期次' AS note, ranking_code, period_no, rank_no, novel_title, score, metric_snapshot
  FROM v_ranking_latest
 WHERE ranking_code = 'FAVORITE_MONTH'
 ORDER BY rank_no;

SELECT '评分榜指定期次' AS note, ranking_code, period_no, rank_no, novel_title, score, metric_snapshot
  FROM v_ranking_item_detail
 WHERE ranking_code = 'RATING_ALL' AND period_no = '202611'
 ORDER BY rank_no;

CALL sp_get_ranking('FAVORITE_MONTH', NULL, 10);

-- ---------------------------------------------------------------------
SELECT '=== 7. 视图验证 ===' AS step;
-- ---------------------------------------------------------------------
SELECT '可见评论：仅审核通过且状态正常' AS note, comment_id, parent_id, content
  FROM v_comment_visible WHERE novel_id = 101 ORDER BY comment_id;

UPDATE comment SET status = 2 WHERE comment_id = 5001;
SELECT '父评论隐藏后，可见回复数应为 0' AS note, COUNT(*) AS visible_replies
  FROM v_comment_visible WHERE novel_id = 101 AND parent_id IS NOT NULL;
UPDATE comment SET status = 1 WHERE comment_id = 5001;

SELECT '评分汇总视图' AS note, novel_id, title, rating_count, score_total, avg_score
  FROM v_novel_rating_summary WHERE novel_id IN (101, 102) ORDER BY novel_id;

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
