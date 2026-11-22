-- =====================================================================
-- 网络文学创作与阅读平台 · C 互动与榜单  建表脚本
-- 文件：C_互动与榜单_建表_v1.0_20261121.sql
-- 模块：C（互动与榜单）        负责人：C        版本：v1.0 / 2026-11-21
-- 依据：docs/current/D_数据字典_v1.0_20261120.xlsx（冻结基线）
--       docs/current/er/全局_ER图_v1.0_20261120.drawio
-- 数据库：MySQL 8.0+   引擎：InnoDB   字符集：utf8mb4
-- -------------------------------------------------------------------
-- 前置依赖（由 A / B 模块提供，本脚本不创建）：
--   app_user (user_id)        ← A 用户与交易
--   novel    (novel_id)       ← B 作品与内容
--   chapter  (chapter_id)     ← B 作品与内容
-- 本模块共 6 张表、50 个字段、20 个索引、12 个物理外键：
--   comment / comment_like / novel_rating / novel_rating_stat
--   ranking_config / ranking_item
-- 说明：收藏(书架)唯一数据源为 A.user_bookshelf，本模块不建 novel_favorite。
-- 执行顺序：先执行 A、B 建表脚本，再执行本脚本。
-- =====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------
-- 1. comment  评论表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS comment (
  comment_id       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT      COMMENT '评论ID；自增主键',
  novel_id         BIGINT UNSIGNED NOT NULL                     COMMENT '小说ID；引用 novel.novel_id（B）',
  chapter_id       BIGINT UNSIGNED NULL DEFAULT NULL            COMMENT '章节ID；引用 chapter.chapter_id（B）；NULL 表示整本书评',
  user_id          BIGINT UNSIGNED NOT NULL                     COMMENT '评论用户ID；引用 app_user.user_id（A）',
  parent_id        BIGINT UNSIGNED NULL DEFAULT NULL            COMMENT '父评论ID；自关联 comment.comment_id；NULL 为一级评论',
  reply_to_user_id BIGINT UNSIGNED NULL DEFAULT NULL            COMMENT '被回复用户ID；引用 app_user.user_id；楼中楼回复对象',
  content          VARCHAR(1000)   NOT NULL                     COMMENT '评论内容；本章说/书评正文，去首尾空格后非空',
  like_count       INT             NOT NULL DEFAULT 0           COMMENT '点赞数；冗余计数，由 comment_like 维护',
  reply_count      INT             NOT NULL DEFAULT 0           COMMENT '回复数；冗余计数，仅统计审核通过且状态正常的直接回复',
  is_top           TINYINT         NOT NULL DEFAULT 0           COMMENT '是否置顶；0 否 1 是',
  status           TINYINT         NOT NULL DEFAULT 1           COMMENT '评论状态；1 正常 2 隐藏 3 已删除',
  audit_status     TINYINT         NOT NULL DEFAULT 0           COMMENT '审核状态；0 待审核 1 通过 2 驳回；展示须 audit_status=1 AND status=1',
  created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP                 COMMENT '评论发表时间',
  updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (comment_id),
  KEY idx_comment_novel   (novel_id, status, audit_status, created_at),
  KEY idx_comment_chapter (chapter_id, status, audit_status, created_at),
  KEY idx_comment_user    (user_id, created_at),
  KEY idx_comment_parent  (parent_id),
  KEY idx_comment_audit   (audit_status, updated_at),
  CONSTRAINT fk_comment_novel      FOREIGN KEY (novel_id)         REFERENCES novel    (novel_id)   ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_comment_chapter    FOREIGN KEY (chapter_id)       REFERENCES chapter  (chapter_id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_comment_user       FOREIGN KEY (user_id)          REFERENCES app_user (user_id)    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_comment_parent     FOREIGN KEY (parent_id)        REFERENCES comment  (comment_id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_comment_reply_user FOREIGN KEY (reply_to_user_id) REFERENCES app_user (user_id)    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT ck_comment_status     CHECK (status IN (1, 2, 3)),
  CONSTRAINT ck_comment_audit      CHECK (audit_status IN (0, 1, 2)),
  CONSTRAINT ck_comment_is_top     CHECK (is_top IN (0, 1)),
  CONSTRAINT ck_comment_content    CHECK (CHAR_LENGTH(TRIM(content)) > 0),
  CONSTRAINT ck_comment_counts     CHECK (like_count >= 0 AND reply_count >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评论表（整本书评、本章说与楼中楼回复）';

-- ---------------------------------------------------------------------
-- 2. comment_like  评论点赞表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS comment_like (
  like_id    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '点赞ID；自增主键',
  comment_id BIGINT UNSIGNED NOT NULL                COMMENT '评论ID；引用 comment.comment_id',
  user_id    BIGINT UNSIGNED NOT NULL                COMMENT '点赞用户ID；引用 app_user.user_id',
  created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间；取消点赞物理删除',
  PRIMARY KEY (like_id),
  UNIQUE KEY uk_comment_like (comment_id, user_id),
  KEY idx_comment_like_user (user_id, created_at),
  CONSTRAINT fk_comment_like_comment FOREIGN KEY (comment_id) REFERENCES comment  (comment_id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_comment_like_user    FOREIGN KEY (user_id)    REFERENCES app_user (user_id)    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评论点赞表（同一用户同一评论仅一条）';

-- ---------------------------------------------------------------------
-- 3. novel_rating  小说评分表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS novel_rating (
  rating_id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '评分ID；自增主键',
  user_id             BIGINT UNSIGNED NOT NULL                COMMENT '评分用户ID；引用 app_user.user_id',
  novel_id            BIGINT UNSIGNED NOT NULL                COMMENT '小说ID；引用 novel.novel_id（B）',
  score               TINYINT         NOT NULL                COMMENT '评分；1~5 星；数值评分不参与短评审核',
  review_content      VARCHAR(500)    NULL DEFAULT NULL       COMMENT '评分短评；可空，空字符串统一存 NULL',
  status              TINYINT         NOT NULL DEFAULT 1      COMMENT '评分状态；1 正常 2 隐藏；短评审核驳回不得修改此字段',
  review_audit_status TINYINT         NULL DEFAULT NULL       COMMENT '短评审核状态；NULL 无短评 0 待审核 1 通过 2 驳回；仅审核 review_content',
  created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP                 COMMENT '首次评分时间',
  updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间；改分时更新',
  PRIMARY KEY (rating_id),
  UNIQUE KEY uk_novel_rating_user (user_id, novel_id),
  KEY idx_novel_rating_novel (novel_id, status, score),
  KEY idx_rating_review_audit (review_audit_status, updated_at),
  CONSTRAINT fk_novel_rating_user  FOREIGN KEY (user_id)  REFERENCES app_user (user_id)  ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_novel_rating_novel FOREIGN KEY (novel_id) REFERENCES novel    (novel_id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT ck_novel_rating_score    CHECK (score BETWEEN 1 AND 5),
  CONSTRAINT ck_novel_rating_status   CHECK (status IN (1, 2)),
  CONSTRAINT ck_novel_rating_review   CHECK (review_audit_status IS NULL OR review_audit_status IN (0, 1, 2)),
  CONSTRAINT ck_novel_rating_review_null CHECK (review_content IS NOT NULL OR review_audit_status IS NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小说评分表（一人一书仅一条评分）';

-- ---------------------------------------------------------------------
-- 4. novel_rating_stat  小说评分统计表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS novel_rating_stat (
  novel_id     BIGINT UNSIGNED NOT NULL              COMMENT '小说ID；与 novel 一对一；引用 novel.novel_id',
  rating_count INT             NOT NULL DEFAULT 0    COMMENT '有效评分人数；仅统计 status=1，不按 review_audit_status 过滤',
  score_total  BIGINT          NOT NULL DEFAULT 0    COMMENT '有效评分总分；触发器增量维护',
  avg_score    DECIMAL(3,2)    NOT NULL DEFAULT 0.00 COMMENT '平均分；0.00~5.00；无评分为 0.00；唯一权威评分统计',
  updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '统计更新时间',
  PRIMARY KEY (novel_id),
  CONSTRAINT fk_rating_stat_novel FOREIGN KEY (novel_id) REFERENCES novel (novel_id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT ck_rating_stat_count CHECK (rating_count >= 0 AND score_total >= 0),
  CONSTRAINT ck_rating_stat_avg   CHECK (avg_score BETWEEN 0.00 AND 5.00)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小说评分统计表（唯一权威评分统计）';

-- ---------------------------------------------------------------------
-- 5. ranking_config  榜单配置表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ranking_config (
  ranking_id    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '榜单ID；自增主键',
  ranking_code  VARCHAR(50)     NOT NULL                COMMENT '榜单编码；唯一，如 FAVORITE_MONTH、RATING_ALL',
  ranking_name  VARCHAR(100)    NOT NULL                COMMENT '榜单名称；展示名称，如“月收藏榜”',
  ranking_type  VARCHAR(30)     NOT NULL                COMMENT '榜单指标类型；READ/FAVORITE/RATING/TICKET/SALE',
  period_type   VARCHAR(20)     NOT NULL                COMMENT '统计周期；DAILY/WEEKLY/MONTHLY/ALL',
  top_n         INT             NOT NULL DEFAULT 100    COMMENT '展示条数上限；榜单最多展示 N 条',
  weight_config JSON            NULL DEFAULT NULL       COMMENT '权重配置；如 {"read":0.4,"favorite":0.3,"rating":0.3}；rating 采用 0.00~5.00',
  is_active     TINYINT         NOT NULL DEFAULT 1      COMMENT '是否启用；1 启用 0 停用；停用后不再生成新期次',
  created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP                 COMMENT '创建时间',
  updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (ranking_id),
  UNIQUE KEY uk_ranking_config_code (ranking_code),
  CONSTRAINT ck_ranking_config_type   CHECK (ranking_type IN ('READ','FAVORITE','RATING','TICKET','SALE')),
  CONSTRAINT ck_ranking_config_period CHECK (period_type IN ('DAILY','WEEKLY','MONTHLY','ALL')),
  CONSTRAINT ck_ranking_config_topn   CHECK (top_n > 0),
  CONSTRAINT ck_ranking_config_active CHECK (is_active IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='榜单配置表（指标、周期、展示上限与权重）';

-- ---------------------------------------------------------------------
-- 6. ranking_item  榜单条目表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ranking_item (
  item_id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '条目ID；自增主键',
  ranking_id      BIGINT UNSIGNED NOT NULL                COMMENT '榜单ID；引用 ranking_config.ranking_id',
  period_no       VARCHAR(20)     NOT NULL                COMMENT '期次号；如 202611（月）、2026W47（周）、20261127（日）',
  novel_id        BIGINT UNSIGNED NOT NULL                COMMENT '小说ID；引用 novel.novel_id（B）',
  rank_no         INT             NOT NULL                COMMENT '名次；从 1 开始，同一期唯一',
  score           DECIMAL(12,4)   NOT NULL DEFAULT 0      COMMENT '综合得分；按权重计算',
  metric_snapshot JSON            NULL DEFAULT NULL       COMMENT '指标快照；如 {"favorite":678}；rating 采用 0.00~5.00',
  created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '快照生成时间',
  PRIMARY KEY (item_id),
  UNIQUE KEY uk_ranking_item_period_novel (ranking_id, period_no, novel_id),
  UNIQUE KEY uk_ranking_item_period_rank  (ranking_id, period_no, rank_no),
  KEY idx_ranking_item_novel (novel_id, ranking_id),
  CONSTRAINT fk_ranking_item_config FOREIGN KEY (ranking_id) REFERENCES ranking_config (ranking_id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_ranking_item_novel  FOREIGN KEY (novel_id)   REFERENCES novel          (novel_id)   ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT ck_ranking_item_rank  CHECK (rank_no >= 1),
  CONSTRAINT ck_ranking_item_score CHECK (score >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='榜单条目表（各期次名次与指标快照）';
