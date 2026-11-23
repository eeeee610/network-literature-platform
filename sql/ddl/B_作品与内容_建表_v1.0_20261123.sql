-- =====================================================================
-- 网络文学创作与阅读平台 · B 作品与内容  建表脚本
-- 文件：B_作品与内容_建表_v1.0_20261123.sql
-- 模块：B（作品与内容）      负责人：B      版本：v1.0 / 2026-11-23
-- 依据：docs/achieve/B_作品与内容_字段清单_v0.2_20261120.xlsx
--       docs/current/D_数据字典_v1.0_20261120.xlsx（冻结基线）
-- 数据库：MySQL 8.0.16+   引擎：InnoDB   字符集：utf8mb4
-- ---------------------------------------------------------------------
-- 前置依赖（由 A 模块提供，本脚本不创建）：
--   app_user (user_id)        ← A 用户与交易
-- 本模块共 4 张表：
--   novel / chapter / tag / novel_tag
-- 执行顺序（空库初始化，因 novel.author_id 与 A 各表互相引用）：
--   1) A 的 app_user、user_account
--   2) 本脚本（novel、chapter、tag、novel_tag）
--   3) A 的 user_bookshelf、chapter_subscription、reward_tip、account_transaction
--   4) C / D 各表
-- 说明：
--   * 评分统计由 C.novel_rating_stat 唯一维护，本模块不保存 avg_score/rating_count。
--   * 小说/章节仅下架（status=2），禁止物理删除，外键一律 ON DELETE RESTRICT。
--   * 金额字段 vip_price 统一 DECIMAL(10,2)，单位：元，供 A 订阅扣费取用。
-- =====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------
-- 1. novel  小说表（冷数据，主行尽量窄；不存正文、不存评分冗余）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS novel (
  novel_id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '小说ID；自增主键',
  author_id         BIGINT UNSIGNED NOT NULL                COMMENT '作者ID；引用 app_user.user_id（A）',
  title             VARCHAR(120)    NOT NULL                COMMENT '书名；前缀/精确检索',
  cover_url         VARCHAR(255)    NULL DEFAULT NULL       COMMENT '封面URL；对象存储地址，不存二进制',
  description       VARCHAR(1000)   NULL DEFAULT NULL       COMMENT '作品简介；不用 TEXT，保证主行窄',
  status            TINYINT         NOT NULL DEFAULT 0      COMMENT '连载状态；0 连载中 1 已完结 2 已下架；下架不物理删除',
  audit_status      TINYINT         NOT NULL DEFAULT 0      COMMENT '审核状态；0 待审核 1 通过 2 驳回；书名/简介修改后重新置 0',
  total_words       INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '总字数；冗余，仅统计已发布章节，由发布/下架存储过程维护',
  latest_chapter_id BIGINT UNSIGNED NULL DEFAULT NULL       COMMENT '最新已发布章节ID；冗余；与 chapter 循环依赖，不建外键',
  created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP                 COMMENT '创建时间',
  updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间；列表排序列',
  PRIMARY KEY (novel_id),
  KEY idx_title          (title),
  KEY idx_status_update  (status, updated_at),
  KEY idx_author_update  (author_id, updated_at),
  KEY idx_novel_audit    (audit_status, updated_at),
  CONSTRAINT fk_novel_author FOREIGN KEY (author_id) REFERENCES app_user (user_id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT ck_novel_status CHECK (status IN (0, 1, 2)),
  CONSTRAINT ck_novel_audit  CHECK (audit_status IN (0, 1, 2)),
  CONSTRAINT ck_novel_title  CHECK (CHAR_LENGTH(TRIM(title)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小说表';

-- ---------------------------------------------------------------------
-- 2. chapter  章节表（热数据；content 为 MEDIUMTEXT，行外溢出页存储）
--    列表查询一律不得 SELECT content。
--    审核与发布分离：审核通过仅改 audit_status；作者手动发布才置
--    status=1 并写 publish_time。
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS chapter (
  chapter_id   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '章节ID；自增主键',
  novel_id     BIGINT UNSIGNED NOT NULL                COMMENT '所属小说ID；引用 novel.novel_id',
  chapter_no   INT UNSIGNED    NOT NULL                COMMENT '章节序号；同书内唯一，从 1 递增',
  title        VARCHAR(120)    NOT NULL                COMMENT '章节标题',
  content      MEDIUMTEXT      NOT NULL                COMMENT '章节正文；上限 16MB，行外溢出页存储',
  is_vip       TINYINT         NOT NULL DEFAULT 0      COMMENT '是否VIP；0 否 1 是',
  vip_price    DECIMAL(10,2)   NOT NULL DEFAULT 0.00   COMMENT 'VIP价格（元）；非VIP=0.00，VIP>0.00；供 A 订阅扣费',
  word_count   INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '本章字数；由存储过程按正文字符数计算',
  status       TINYINT         NOT NULL DEFAULT 0      COMMENT '发布状态；0 草稿/未发布 1 已发布 2 已下架；下架不物理删除',
  audit_status TINYINT         NULL DEFAULT NULL       COMMENT '审核状态；NULL 未提交（草稿）0 待审核 1 通过 2 驳回；正文修改后重新置 0',
  publish_time DATETIME        NULL DEFAULT NULL       COMMENT '发布时间；NULL 表示尚未发布',
  created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP                 COMMENT '创建时间',
  updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (chapter_id),
  UNIQUE KEY uk_novel_chapter (novel_id, chapter_no),
  KEY idx_catalog       (novel_id, status, audit_status, chapter_no),
  KEY idx_chapter_audit (audit_status, updated_at),
  CONSTRAINT fk_chapter_novel FOREIGN KEY (novel_id) REFERENCES novel (novel_id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT ck_chapter_status CHECK (status IN (0, 1, 2)),
  CONSTRAINT ck_chapter_audit  CHECK (audit_status IS NULL OR audit_status IN (0, 1, 2)),
  CONSTRAINT ck_chapter_is_vip CHECK (is_vip IN (0, 1)),
  CONSTRAINT ck_chapter_price  CHECK ((is_vip = 0 AND vip_price = 0.00) OR (is_vip = 1 AND vip_price > 0.00)),
  CONSTRAINT ck_chapter_title  CHECK (CHAR_LENGTH(TRIM(title)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='章节表';

-- ---------------------------------------------------------------------
-- 3. tag  标签表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tag (
  tag_id     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '标签ID；自增主键',
  tag_name   VARCHAR(32)     NOT NULL                COMMENT '标签名；唯一，防重复建同名标签',
  is_active  TINYINT         NOT NULL DEFAULT 1      COMMENT '是否启用；1 启用 0 停用',
  created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (tag_id),
  UNIQUE KEY uk_tag_name (tag_name),
  CONSTRAINT ck_tag_active CHECK (is_active IN (0, 1)),
  CONSTRAINT ck_tag_name   CHECK (CHAR_LENGTH(TRIM(tag_name)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='标签表';

-- ---------------------------------------------------------------------
-- 4. novel_tag  小说-标签关联表（M:N 拆解）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS novel_tag (
  novel_id   BIGINT UNSIGNED  NOT NULL                COMMENT '小说ID；联合主键之一；引用 novel.novel_id',
  tag_id     BIGINT UNSIGNED  NOT NULL                COMMENT '标签ID；联合主键之一；引用 tag.tag_id',
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0     COMMENT '展示顺序；越小越靠前',
  created_at DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '打标时间',
  PRIMARY KEY (novel_id, tag_id),
  KEY idx_tag_reverse (tag_id, novel_id),
  CONSTRAINT fk_nt_novel FOREIGN KEY (novel_id) REFERENCES novel (novel_id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_nt_tag   FOREIGN KEY (tag_id)   REFERENCES tag   (tag_id)   ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小说-标签关联表';

-- ---------------------------------------------------------------------
-- 最小验收：
--   SHOW TABLES;                       -- 应见 novel/chapter/tag/novel_tag
--   SHOW INDEX FROM chapter;           -- uk_novel_chapter 应 Non_unique=0
--   SELECT * FROM information_schema.TABLE_CONSTRAINTS
--    WHERE TABLE_SCHEMA='novel_platform' AND TABLE_NAME='novel';
-- =====================================================================
