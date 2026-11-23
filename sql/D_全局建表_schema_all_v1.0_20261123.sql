-- ============================================================================
-- 网络文学创作与阅读平台 · 全局数据库初始化脚本
-- 文件：D_全局建表_schema_all_v1.0_20261123.sql
-- 版本：v1.0 / 2026-11-23
-- 用途：全新数据库首次初始化；不承诺重复执行。
-- 依据：当前 develop 工作区已测试通过的 A/B/C/D 模块 SQL 文件。
-- 数据库：MySQL 8.0.16+，InnoDB，utf8mb4 / utf8mb4_0900_ai_ci
-- 不包含：测试数据、冒烟测试、测试过程调用、额外索引部署。
-- 约束：不关闭 FOREIGN_KEY_CHECKS，不修改任何冻结表结构或对象定义。
--
-- 建表顺序：
--   1. novel_platform 数据库
--   2. A 基础表：app_user、user_account
--   3. B 全部表：novel、chapter、tag、novel_tag
--   4. A 剩余表：user_bookshelf、chapter_subscription、reward_tip、account_transaction
--   5. C 全部表
--   6. D 全部表
--
-- 对象顺序：A 视图/过程 → B 过程 → C 触发器 → C 视图/过程。
-- 对象数量：5 个视图、15 个存储过程、8 个触发器，无函数。
-- ============================================================================

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS `novel_platform`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE `novel_platform`;
SET FOREIGN_KEY_CHECKS = 1;
-- ============================================================================
-- 第一部分：A 基础表
-- 来源：sql/ddl/A_用户交易_建表_v1.1_20261123.sql（第 1～2 表）
-- ============================================================================

-- ============================================================================
-- 1. 用户表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `app_user` (
    `user_id`       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户 ID',
    `username`      VARCHAR(50)     NOT NULL COMMENT '登录名，全局唯一',
    `password_hash` VARCHAR(255)    NOT NULL COMMENT '密码哈希（bcrypt/Argon2），禁止存明文',
    `email`         VARCHAR(254)    DEFAULT NULL COMMENT '邮箱，可空；唯一索引允许多个 NULL',
    `phone`         VARCHAR(20)     DEFAULT NULL COMMENT '手机号，可空；唯一索引允许多个 NULL',
    `nickname`      VARCHAR(50)     NOT NULL COMMENT '展示昵称',
    `avatar_url`    VARCHAR(500)    DEFAULT NULL COMMENT '头像地址',
    `role`          ENUM('READER','AUTHOR','ADMIN') NOT NULL DEFAULT 'READER'
                                    COMMENT '角色：读者/作者/管理员',
    `status`        ENUM('ACTIVE','LOCKED','DISABLED') NOT NULL DEFAULT 'ACTIVE'
                                    COMMENT '账号状态',
    `last_login_at` DATETIME(6)     DEFAULT NULL COMMENT '最近登录时间',
    `created_at`    DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`    DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                    ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_app_user_username` (`username`),
    UNIQUE KEY `uk_app_user_email` (`email`),
    UNIQUE KEY `uk_app_user_phone` (`phone`),
    CONSTRAINT `ck_user_username_not_blank` CHECK (CHAR_LENGTH(TRIM(`username`)) > 0),
    CONSTRAINT `ck_user_password_hash_len` CHECK (CHAR_LENGTH(`password_hash`) >= 20)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '用户表：注册登录、角色与账号状态';


-- ============================================================================
-- 2. 用户账户余额表
--    与 app_user 一对一。注册时必须在同一事务中插入一行，初始余额 0。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `user_account` (
    `user_id`    BIGINT UNSIGNED NOT NULL COMMENT '用户 ID，一对一主键',
    `balance`    DECIMAL(12,2)   NOT NULL DEFAULT 0.00 COMMENT '可用余额（元）',
    `created_at` DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at` DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                 ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`user_id`),
    CONSTRAINT `fk_account_user`
        FOREIGN KEY (`user_id`) REFERENCES `app_user` (`user_id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `ck_account_balance_non_negative` CHECK (`balance` >= 0.00)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '用户账户余额表：余额只允许由事务/存储过程变更';
-- ============================================================================
-- 第二部分：B 全部表
-- 来源：sql/ddl/B_作品与内容_建表_v1.0_20261123.sql（完整建表定义）
-- ============================================================================

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
  cover_url         VARCHAR(255)    NULL                    COMMENT '封面URL；对象存储地址，不存二进制',
  description       VARCHAR(1000)   NULL                    COMMENT '作品简介；不用 TEXT，保证主行窄',
  status            TINYINT         NOT NULL DEFAULT 0      COMMENT '连载状态；0 连载中 1 已完结 2 已下架；下架不物理删除',
  audit_status      TINYINT         NOT NULL DEFAULT 0      COMMENT '审核状态；0 待审核 1 通过 2 驳回；书名/简介修改后重新置 0',
  total_words       INT UNSIGNED    NOT NULL DEFAULT 0      COMMENT '总字数；冗余，仅统计已发布章节，由发布/下架存储过程维护',
  latest_chapter_id BIGINT UNSIGNED NULL                    COMMENT '最新已发布章节ID；冗余；与 chapter 循环依赖，不建外键',
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
  publish_time DATETIME        NULL                    COMMENT '发布时间；NULL 表示尚未发布',
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
-- ============================================================================
-- 第三部分：A 剩余表
-- 来源：sql/ddl/A_用户交易_建表_v1.1_20261123.sql（第 3～6 表）
-- ============================================================================

-- ============================================================================
-- 3. 书架表
--    用户与小说 M:N；联合主键天然防止同一本书重复加入书架。
--    同时保存最近阅读进度和更新提醒开关，是书架/收藏的唯一数据源。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `user_bookshelf` (
    `user_id`              BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
    `novel_id`             BIGINT UNSIGNED NOT NULL COMMENT '小说 ID（物理外键 novel.novel_id）',
    `last_read_chapter_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '最近阅读章节 ID（物理外键 chapter.chapter_id）',
    `last_read_at`         DATETIME(6)     DEFAULT NULL COMMENT '最近阅读时间',
    `is_top`               TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否置顶：0 否，1 是',
    `is_notify`            TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否接收更新提醒：1 是，0 否',
    `created_at`           DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '收藏/加入书架时间',
    `updated_at`           DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                           ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`user_id`, `novel_id`),
    KEY `idx_shelf_user_top_time` (`user_id`, `is_top`, `updated_at`),
    KEY `idx_shelf_novel` (`novel_id`),
    KEY `idx_shelf_last_chapter` (`last_read_chapter_id`),
    CONSTRAINT `fk_shelf_user`
        FOREIGN KEY (`user_id`) REFERENCES `app_user` (`user_id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_shelf_novel`
        FOREIGN KEY (`novel_id`) REFERENCES `novel` (`novel_id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_shelf_last_chapter`
        FOREIGN KEY (`last_read_chapter_id`) REFERENCES `chapter` (`chapter_id`)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `ck_shelf_is_top` CHECK (`is_top` IN (0, 1)),
    CONSTRAINT `ck_shelf_is_notify` CHECK (`is_notify` IN (0, 1))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '书架表：用户与小说 M:N，含置顶、阅读进度和追更提醒';


-- ============================================================================
-- 4. 章节订阅记录表
--    联合主键 (user_id, chapter_id) 是防重复扣费的第一道数据库约束；
--    txn_no / request_no 唯一索引用于审计与接口幂等。
--    chapter_id / novel_id 已按总字典建立物理外键。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `chapter_subscription` (
    `user_id`       BIGINT UNSIGNED NOT NULL COMMENT '订阅用户 ID',
    `chapter_id`    BIGINT UNSIGNED NOT NULL COMMENT '章节 ID（物理外键 chapter.chapter_id）',
    `novel_id`      BIGINT UNSIGNED NOT NULL COMMENT '小说 ID（物理外键 novel.novel_id，冗余便于按书查询）',
    `unit_price`    DECIMAL(12,2)   NOT NULL COMMENT '订阅时章节 VIP 标价（元）',
    `paid_amount`   DECIMAL(12,2)   NOT NULL COMMENT '实际支付金额（元）',
    `txn_no`        CHAR(32)        NOT NULL COMMENT '对应账户流水号',
    `request_no`    VARCHAR(64)     NOT NULL COMMENT '订阅请求号，接口幂等键',
    `status`        ENUM('ACTIVE','REFUNDED') NOT NULL DEFAULT 'ACTIVE'
                                     COMMENT '订阅状态',
    `subscribed_at` DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `refunded_at`   DATETIME(6)     DEFAULT NULL,
    PRIMARY KEY (`user_id`, `chapter_id`),
    UNIQUE KEY `uk_chapter_subscription_txn_no` (`txn_no`),
    UNIQUE KEY `uk_chapter_subscription_request_no` (`request_no`),
    KEY `idx_sub_user_time` (`user_id`, `subscribed_at`),
    KEY `idx_sub_novel` (`novel_id`),
    KEY `idx_sub_chapter` (`chapter_id`),
    CONSTRAINT `fk_sub_user`
        FOREIGN KEY (`user_id`) REFERENCES `app_user` (`user_id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_sub_chapter`
        FOREIGN KEY (`chapter_id`) REFERENCES `chapter` (`chapter_id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_sub_novel`
        FOREIGN KEY (`novel_id`) REFERENCES `novel` (`novel_id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `ck_sub_price` CHECK (
        `unit_price` > 0.00
        AND `paid_amount` > 0.00
        AND `paid_amount` <= `unit_price`
    ),
    CONSTRAINT `ck_sub_refund_time` CHECK (
        (`status` = 'ACTIVE' AND `refunded_at` IS NULL)
        OR (`status` = 'REFUNDED' AND `refunded_at` IS NOT NULL)
    )
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '章节订阅记录：联合主键防同一用户重复购买同一章节';


-- ============================================================================
-- 5. 打赏表
--    一次打赏对应两条流水：打赏者 TIP_OUT、作者 TIP_IN。
--    request_no 唯一，保证同一请求最多生成一条打赏记录。
--    novel_id / chapter_id 已按总字典建立物理外键。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `reward_tip` (
    `tip_id`       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '打赏 ID',
    `tip_no`       CHAR(32)        NOT NULL COMMENT '打赏业务单号（UUID 去横线）',
    `request_no`   VARCHAR(64)     NOT NULL COMMENT '打赏请求号，接口幂等键',
    `from_user_id` BIGINT UNSIGNED NOT NULL COMMENT '打赏者用户 ID',
    `to_user_id`   BIGINT UNSIGNED NOT NULL COMMENT '作者用户 ID（收款方）',
    `novel_id`     BIGINT UNSIGNED NOT NULL COMMENT '小说 ID（物理外键 novel.novel_id）',
    `chapter_id`   BIGINT UNSIGNED DEFAULT NULL COMMENT '章节 ID，可空（整本书打赏）；物理外键 chapter.chapter_id',
    `amount`       DECIMAL(12,2)   NOT NULL COMMENT '打赏金额（元）',
    `message`      VARCHAR(200)    NOT NULL DEFAULT '' COMMENT '打赏留言',
    `status`       ENUM('SUCCESS','REFUNDED') NOT NULL DEFAULT 'SUCCESS'
                                   COMMENT '打赏状态',
    `out_txn_no`   CHAR(32)        NOT NULL COMMENT '打赏者支出流水号',
    `in_txn_no`    CHAR(32)        NOT NULL COMMENT '作者收入流水号',
    `created_at`   DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `refunded_at`  DATETIME(6)     DEFAULT NULL,
    PRIMARY KEY (`tip_id`),
    UNIQUE KEY `uk_reward_tip_tip_no` (`tip_no`),
    UNIQUE KEY `uk_reward_tip_request_no` (`request_no`),
    UNIQUE KEY `uk_reward_tip_out_txn_no` (`out_txn_no`),
    UNIQUE KEY `uk_reward_tip_in_txn_no` (`in_txn_no`),
    KEY `idx_tip_from_time` (`from_user_id`, `created_at`),
    KEY `idx_tip_to_time` (`to_user_id`, `created_at`),
    KEY `idx_tip_novel` (`novel_id`),
    KEY `idx_tip_chapter` (`chapter_id`),
    CONSTRAINT `fk_tip_from_user`
        FOREIGN KEY (`from_user_id`) REFERENCES `app_user` (`user_id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_tip_to_user`
        FOREIGN KEY (`to_user_id`) REFERENCES `app_user` (`user_id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_tip_novel`
        FOREIGN KEY (`novel_id`) REFERENCES `novel` (`novel_id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_tip_chapter`
        FOREIGN KEY (`chapter_id`) REFERENCES `chapter` (`chapter_id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `ck_tip_amount` CHECK (`amount` > 0.00),
    -- 不在 from_user_id / to_user_id / chapter_id 上写 CHECK：
    -- MySQL 不允许对带外键动作的列再加 CHECK；“不能给自己打赏”
    -- 和“收款人必须是作者”由 sp_tip_author 校验。
    CONSTRAINT `ck_tip_refund_time` CHECK (
        (`status` = 'SUCCESS' AND `refunded_at` IS NULL)
        OR (`status` = 'REFUNDED' AND `refunded_at` IS NOT NULL)
    )
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '打赏记录：一次打赏对应打赏者支出与作者收入两条流水';


-- ============================================================================
-- 6. 账户流水表
--    只追加、不修改。biz_key 唯一，保证同一业务最多记一次账。
--    balance_before / balance_after 同时保存，便于逐笔对账。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `account_transaction` (
    `txn_id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '流水 ID',
    `txn_no`         CHAR(32)        NOT NULL COMMENT '流水号（UUID 去横线）',
    `user_id`        BIGINT UNSIGNED NOT NULL COMMENT '账户所属用户 ID',
    `biz_type`       ENUM('RECHARGE','SUBSCRIBE','TIP_OUT','TIP_IN',
                          'REFUND','WITHDRAW','ADJUST_IN','ADJUST_OUT')
                                     NOT NULL COMMENT '业务类型',
    `direction`      ENUM('IN','OUT') NOT NULL COMMENT '资金方向：收入/支出',
    `amount`         DECIMAL(12,2)   NOT NULL COMMENT '发生额（正数）',
    `balance_before` DECIMAL(12,2)   NOT NULL COMMENT '本次记账前余额',
    `balance_after`  DECIMAL(12,2)   NOT NULL COMMENT '本次记账后余额',
    `biz_key`        VARCHAR(96)     NOT NULL COMMENT '业务唯一键，幂等防重',
    `related_type`   ENUM('CHAPTER','TIP','RECHARGE_ORDER','WITHDRAW_ORDER','SYSTEM')
                                     NOT NULL DEFAULT 'SYSTEM' COMMENT '关联业务类型',
    `related_id`     BIGINT UNSIGNED DEFAULT NULL COMMENT '关联业务 ID',
    `request_no`     VARCHAR(64)     DEFAULT NULL COMMENT '上游请求号，便于追踪',
    `remark`         VARCHAR(255)    NOT NULL DEFAULT '' COMMENT '备注',
    `created_at`     DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`txn_id`),
    UNIQUE KEY `uk_account_transaction_txn_no` (`txn_no`),
    UNIQUE KEY `uk_account_transaction_biz_key` (`biz_key`),
    KEY `idx_txn_user_time` (`user_id`, `created_at`),
    KEY `idx_txn_request_no` (`request_no`),
    KEY `idx_txn_type_ref` (`related_type`, `related_id`),
    CONSTRAINT `fk_txn_user`
        FOREIGN KEY (`user_id`) REFERENCES `app_user` (`user_id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `ck_txn_amount_positive` CHECK (`amount` > 0.00),
    CONSTRAINT `ck_txn_biz_direction` CHECK (
        (`biz_type` = 'REFUND')
        OR (`biz_type` IN ('RECHARGE','TIP_IN','ADJUST_IN') AND `direction` = 'IN')
        OR (`biz_type` IN ('SUBSCRIBE','TIP_OUT','WITHDRAW','ADJUST_OUT')
            AND `direction` = 'OUT')
    ),
    CONSTRAINT `ck_txn_balance_non_negative` CHECK (
        `balance_before` >= 0.00 AND `balance_after` >= 0.00
    ),
    CONSTRAINT `ck_txn_balance_math` CHECK (
        (`direction` = 'IN'  AND `balance_after` = `balance_before` + `amount`)
        OR
        (`direction` = 'OUT' AND `balance_after` = `balance_before` - `amount`)
    )
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '账户流水：余额变更的唯一审计依据，按业务键幂等';
-- ============================================================================
-- 第四部分：C 全部表
-- 来源：sql/ddl/C_互动与榜单_建表_v1.0_20261121.sql
-- ============================================================================

-- =====================================================================
-- 网络文学创作与阅读平台 · C 互动与榜单  建表脚本
-- 文件：C_互动与榜单_建表_v1.0_20261121.sql
-- 模块：C（互动与榜单）        负责人：C        版本：v1.0 / 2026-11-21
-- 依据：docs/current/D_数据字典_v1.0_20261120.xlsx（冻结基线）
--       docs/current/er/全局_ER图_v1.0_20261120.drawio
-- 数据库：MySQL 8.0.16+   引擎：InnoDB   字符集：utf8mb4
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
  ranking_type  VARCHAR(30)     NOT NULL                COMMENT '榜单指标类型；FAVORITE/RATING/TICKET/SALE',
  period_type   VARCHAR(20)     NOT NULL                COMMENT '统计周期；DAILY/WEEKLY/MONTHLY/ALL',
  top_n         INT             NOT NULL DEFAULT 100    COMMENT '展示条数上限；榜单最多展示 N 条',
  weight_config JSON            NULL DEFAULT NULL       COMMENT '权重配置；如 {"favorite":0.4,"rating":0.6}；rating 采用 0.00~5.00',
  is_active     TINYINT         NOT NULL DEFAULT 1      COMMENT '是否启用；1 启用 0 停用；停用后不再生成新期次',
  created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP                 COMMENT '创建时间',
  updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (ranking_id),
  UNIQUE KEY uk_ranking_config_code (ranking_code),
  CONSTRAINT ck_ranking_config_type   CHECK (ranking_type IN ('FAVORITE','RATING','TICKET','SALE')),
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
-- ============================================================================
-- 第五部分：D 全部表
-- 来源：sql/ddl/D_运营后台_建表_v1.0_20261121.sql
-- ============================================================================

-- 网络文学创作与阅读平台 D 模块（运营后台）建表脚本
-- 适用版本：MySQL 8.0.16+
-- 设计依据：D_数据字典_v1.0_20261120.xlsx、全局_ER图_v1.0_20261120.drawio
-- 前置依赖：A 模块 app_user 表已创建，user_id 类型为 BIGINT UNSIGNED
-- 存储引擎：InnoDB；字符集：utf8mb4

SET NAMES utf8mb4;

-- 审核日志只追加，不更新、不物理删除。
-- target_type + target_id 为 NOVEL/CHAPTER/COMMENT/RATING 多态 XOR 逻辑关联，
-- 不建立普通外键；目标存在性及类型匹配由应用层或存储过程校验。
CREATE TABLE `audit_log` (
    `audit_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `admin_id` BIGINT UNSIGNED NOT NULL,
    `target_type` ENUM('NOVEL', 'CHAPTER', 'COMMENT', 'RATING') NOT NULL,
    `target_id` BIGINT UNSIGNED NOT NULL COMMENT '多态审核对象ID，不建立普通外键',
    `audit_action` TINYINT NOT NULL COMMENT '1通过，2驳回',
    `audit_reason` VARCHAR(255) NULL DEFAULT NULL COMMENT '通过时可选，驳回时必须填写非空原因',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`audit_id`),
    KEY `idx_audit_log_admin_id` (`admin_id`),
    KEY `idx_audit_log_target` (`target_type`, `target_id`, `created_at`),
    KEY `idx_audit_log_created_at` (`created_at`),
    CONSTRAINT `fk_audit_log_admin`
        FOREIGN KEY (`admin_id`) REFERENCES `app_user` (`user_id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `chk_audit_log_action`
        CHECK (`audit_action` IN (1, 2)),
    CONSTRAINT `chk_audit_log_reason`
        CHECK (
            `audit_action` = 1
            OR (
                `audit_action` = 2
                AND `audit_reason` IS NOT NULL
                AND CHAR_LENGTH(TRIM(`audit_reason`)) > 0
            )
        )
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '审核日志表，保存小说、章节、评论及评分短评的审核历史';

CREATE TABLE `admin_operation` (
    `op_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `admin_id` BIGINT UNSIGNED NOT NULL,
    `op_module` VARCHAR(50) NOT NULL,
    `op_action` VARCHAR(50) NOT NULL,
    `op_detail` TEXT NULL,
    `ip_address` VARCHAR(50) NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`op_id`),
    KEY `idx_admin_op_admin_id` (`admin_id`),
    KEY `idx_admin_op_module` (`op_module`),
    KEY `idx_admin_op_create_time` (`create_time`),
    CONSTRAINT `fk_admin_operation_admin`
        FOREIGN KEY (`admin_id`) REFERENCES `app_user` (`user_id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '后台操作表';

CREATE TABLE `sys_dict` (
    `dict_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `dict_type` VARCHAR(50) NOT NULL,
    `dict_label` VARCHAR(50) NOT NULL,
    `dict_value` VARCHAR(50) NOT NULL,
    `sort_order` INT NOT NULL DEFAULT 0,
    `is_active` TINYINT NOT NULL DEFAULT 1 COMMENT '1启用，0停用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`dict_id`),
    KEY `idx_sys_dict_type` (`dict_type`),
    UNIQUE KEY `uk_sys_dict_type_value` (`dict_type`, `dict_value`),
    CONSTRAINT `chk_sys_dict_is_active`
        CHECK (`is_active` IN (0, 1))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '系统字典表';
-- ============================================================================
-- 第六部分：A 视图与存储过程
-- 来源：A_用户交易_余额对账视图_v1.1_20261123.sql
-- 来源：A_订阅扣费存储过程_v1.1_20261123.sql
-- ============================================================================

-- ============================================================================
-- 文件：sql/objects/A_用户交易_余额对账视图_v1.1_20261123.sql
-- 模块：成员 A —— 用户与交易模块
-- 依据：docs/current/D_数据字典_v1.0_20261120.xlsx
-- 目标数据库：MySQL 8.0.16+
-- 依赖：先执行 sql/ddl/A_用户交易_建表_v1.1_20261123.sql
--
-- 说明：
--   * 本文件只包含余额对账视图，不含建表、存储过程。
--   * 视图对每个账户核对：
--       (1) 账户当前余额 = 全部流水净额；
--       (2) 每笔流水 balance_after = balance_before ± amount；
--       (3) 流水链连续：本笔 balance_before = 上一笔 balance_after（首笔为 0）。
--   * reconcile_status：MATCH 全部通过；MISMATCH 任一项不一致。
-- ============================================================================

USE `novel_platform`;

CREATE OR REPLACE VIEW `v_user_balance_reconciliation` AS
SELECT
    a.`user_id`                                      AS `user_id`,
    u.`username`                                     AS `username`,
    u.`role`                                         AS `role`,
    u.`status`                                       AS `user_status`,
    a.`balance`                                      AS `account_balance`,
    COALESCE(x.`ledger_net`, 0.00)                   AS `ledger_net`,
    a.`balance` - COALESCE(x.`ledger_net`, 0.00)     AS `difference`,
    COALESCE(x.`txn_count`, 0)                       AS `txn_count`,
    COALESCE(x.`bad_txn_count`, 0)                   AS `bad_txn_count`,
    x.`last_txn_at`                                  AS `last_txn_at`,
    CASE
        WHEN a.`balance` = COALESCE(x.`ledger_net`, 0.00)
         AND COALESCE(x.`bad_txn_count`, 0) = 0
        THEN 'MATCH'
        ELSE 'MISMATCH'
    END                                              AS `reconcile_status`,
    CURRENT_TIMESTAMP(6)                             AS `checked_at`
FROM `user_account` AS a
JOIN `app_user` AS u
  ON u.`user_id` = a.`user_id`
LEFT JOIN (
    SELECT
        s.`user_id`,
        SUM(s.`signed_amount`) AS `ledger_net`,
        COUNT(*)               AS `txn_count`,
        SUM(
            CASE
                WHEN s.`balance_after` <> s.`balance_before` + s.`signed_amount`
                  OR s.`balance_before` <> COALESCE(s.`prev_balance_after`, 0.00)
                THEN 1
                ELSE 0
            END
        ) AS `bad_txn_count`,
        MAX(s.`created_at`) AS `last_txn_at`
    FROM (
        SELECT
            t.`txn_id`,
            t.`user_id`,
            t.`amount`,
            t.`balance_before`,
            t.`balance_after`,
            t.`created_at`,
            CASE t.`direction`
                WHEN 'IN'  THEN t.`amount`
                WHEN 'OUT' THEN -t.`amount`
            END AS `signed_amount`,
            LAG(t.`balance_after`) OVER (
                PARTITION BY t.`user_id`
                ORDER BY t.`txn_id`
            ) AS `prev_balance_after`
        FROM `account_transaction` AS t
    ) AS s
    GROUP BY s.`user_id`
) AS x
  ON x.`user_id` = a.`user_id`;


-- ============================================================================
-- 最小验收检查（应返回 0 行；返回行即 MISMATCH）
-- ============================================================================
-- SELECT *
-- FROM v_user_balance_reconciliation
-- WHERE reconcile_status <> 'MATCH';
-- ============================================================================

-- ============================================================================
-- 文件：sql/objects/A_订阅扣费存储过程_v1.1_20261123.sql
-- 模块：成员 A —— 用户与交易模块
-- 依据：docs/current/D_数据字典_v1.0_20261120.xlsx
-- 目标数据库：MySQL 8.0.16+
-- 依赖：先执行 sql/ddl/A_用户交易_建表_v1.1_20261123.sql
--       余额对账视图见 sql/objects/A_用户交易_余额对账视图_v1.1_20261123.sql
--
-- 本文件包含：
--   1. sp_subscribe_chapter —— 章节订阅扣费（事务 + 账户行级锁 + chapter.vip_price 价格快照）
--   2. sp_tip_author        —— 打赏（事务 + 双方账户行级锁 + 作者校验 + 两条流水）
--
-- 重要约定：
--   * 两个过程都自行开启/提交事务，调用方不要再包一层事务。
--   * 订阅价格直接读 B 的 chapter.vip_price，不再由调用方传入。
--   * 打赏收款人必须是 novel.author_id（章节打赏按 chapter.novel_id 找作者）。
--   * 账户行锁顺序固定，降低并发打赏死锁概率。
--   * 余额不足、参数非法等业务错误使用 SQLSTATE '45000'，由后端统一转成
--     400/409 响应；唯一键冲突（1062）表示并发重复请求，需按 request_no
--     查询已有结果并返回，不应自动重试扣款。
-- ============================================================================

USE `novel_platform`;

DELIMITER $$


-- ============================================================================
-- 1. 章节订阅扣费
--
-- 价格来源：直接读取 B 的 chapter.vip_price，不接受调用方传价格；
--          同时校验 is_vip=1、status=1（已发布）、audit_status=1（审核通过）。
-- 并发安全：
--   (1) 锁 app_user（共享）和 user_account（排他），同一用户订阅串行化；
--   (2) 拿到账户锁后再检查 chapter_subscription，避免“检查后插入”竞态；
--   (3) 对 chapter 行加共享锁，保证本次扣费期间价格不被改价；
--   (4) chapter_subscription 联合主键 + account_transaction.biz_key 兜底。
--
-- 输出：
--   p_result_code   SUCCESS / ALREADY_SUBSCRIBED
--   p_message       结果说明
--   p_txn_no        本次或已有订阅对应的流水号
--   p_balance_after 处理后余额
-- ============================================================================
DROP PROCEDURE IF EXISTS `sp_subscribe_chapter` $$
CREATE PROCEDURE `sp_subscribe_chapter`(
    IN  p_user_id    BIGINT UNSIGNED,
    IN  p_chapter_id BIGINT UNSIGNED,
    IN  p_request_no VARCHAR(64),
    OUT p_result_code   VARCHAR(32),
    OUT p_message       VARCHAR(255),
    OUT p_txn_no        CHAR(32),
    OUT p_balance_after DECIMAL(12,2)
)
    MODIFIES SQL DATA
    SQL SECURITY INVOKER
    COMMENT '章节订阅扣费：事务+账户行锁+章节价格快照+联合主键幂等'
proc: BEGIN
    DECLARE v_user_status       VARCHAR(16);
    DECLARE v_balance_before    DECIMAL(12,2);
    DECLARE v_balance_after     DECIMAL(12,2);
    DECLARE v_existing_txn_no   CHAR(32);
    DECLARE v_txn_no            CHAR(32);
    DECLARE v_request_no        VARCHAR(64);
    DECLARE v_biz_key           VARCHAR(96);
    DECLARE v_novel_id          BIGINT UNSIGNED;
    DECLARE v_is_vip            TINYINT;
    DECLARE v_chapter_price     DECIMAL(12,2);
    DECLARE v_chapter_status    TINYINT;
    DECLARE v_audit_status      TINYINT;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET p_result_code = 'FAILED';
    SET p_message = '';
    SET p_txn_no = NULL;
    SET p_balance_after = NULL;

    -- ---------- 参数校验 ----------
    IF p_user_id IS NULL OR p_user_id = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'user_id 不能为空';
    END IF;

    IF p_chapter_id IS NULL OR p_chapter_id = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'chapter_id 不能为空';
    END IF;

    SET v_request_no = COALESCE(
        NULLIF(TRIM(p_request_no), ''),
        CONCAT('SUB-', REPLACE(UUID(), '-', ''))
    );

    IF CHAR_LENGTH(v_request_no) > 64 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'request_no 长度不能超过 64';
    END IF;

    -- ---------- 事务开始 ----------
    START TRANSACTION;

    -- 锁定用户行，防止事务期间被禁用。
    SELECT `status`
      INTO v_user_status
      FROM `app_user`
     WHERE `user_id` = p_user_id
     FOR SHARE;

    IF v_user_status IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '用户不存在';
    END IF;

    IF v_user_status <> 'ACTIVE' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '用户状态不可用，禁止订阅';
    END IF;

    -- 账户排他锁：同一用户的余额变更串行化。
    SELECT `balance`
      INTO v_balance_before
      FROM `user_account`
     WHERE `user_id` = p_user_id
     FOR UPDATE;

    IF v_balance_before IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '用户账户不存在';
    END IF;

    -- 必须在拿到账户锁之后再检查订阅，否则两个并发请求都可能读到“未订阅”。
    SET v_existing_txn_no = NULL;

    SELECT `txn_no`
      INTO v_existing_txn_no
      FROM `chapter_subscription`
     WHERE `user_id` = p_user_id
       AND `chapter_id` = p_chapter_id
     FOR UPDATE;

    IF v_existing_txn_no IS NOT NULL THEN
        SET p_result_code = 'ALREADY_SUBSCRIBED';
        SET p_message = '该章节已订阅，本次未重复扣费';
        SET p_txn_no = v_existing_txn_no;
        SET p_balance_after = v_balance_before;
        COMMIT;
        LEAVE proc;
    END IF;

    -- 价格和章节状态必须在数据库内读取；共享锁避免扣费期间改价。
    SELECT c.`novel_id`, c.`is_vip`, c.`vip_price`, c.`status`, c.`audit_status`
      INTO v_novel_id, v_is_vip, v_chapter_price, v_chapter_status, v_audit_status
      FROM `chapter` AS c
     WHERE c.`chapter_id` = p_chapter_id
     FOR SHARE;

    IF v_novel_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '章节不存在';
    END IF;

    IF v_is_vip <> 1 OR v_chapter_price IS NULL OR v_chapter_price <= 0.00 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '该章节不是可订阅的 VIP 章节';
    END IF;

    IF v_chapter_status <> 1 OR v_audit_status IS NULL OR v_audit_status <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '章节未发布或未通过审核，禁止订阅';
    END IF;

    IF v_balance_before < v_chapter_price THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '账户余额不足';
    END IF;

    SET v_balance_after = v_balance_before - v_chapter_price;
    SET v_txn_no = REPLACE(UUID(), '-', '');
    SET v_biz_key = CONCAT('SUB:', p_user_id, ':', p_chapter_id);

    -- 扣减余额。账户行已被排他锁锁定，直接写入计算后的余额。
    UPDATE `user_account`
       SET `balance` = v_balance_after,
           `updated_at` = CURRENT_TIMESTAMP(6)
     WHERE `user_id` = p_user_id;

    -- 保存订阅时的章节价格快照；联合主键 + request_no 唯一兜底。
    INSERT INTO `chapter_subscription` (
        `user_id`,
        `chapter_id`,
        `novel_id`,
        `unit_price`,
        `paid_amount`,
        `txn_no`,
        `request_no`,
        `status`,
        `subscribed_at`
    ) VALUES (
        p_user_id,
        p_chapter_id,
        v_novel_id,
        v_chapter_price,
        v_chapter_price,
        v_txn_no,
        v_request_no,
        'ACTIVE',
        CURRENT_TIMESTAMP(6)
    );

    -- 流水与余额变更在同一事务内提交。
    INSERT INTO `account_transaction` (
        `txn_no`,
        `user_id`,
        `biz_type`,
        `direction`,
        `amount`,
        `balance_before`,
        `balance_after`,
        `biz_key`,
        `related_type`,
        `related_id`,
        `request_no`,
        `remark`
    ) VALUES (
        v_txn_no,
        p_user_id,
        'SUBSCRIBE',
        'OUT',
        v_chapter_price,
        v_balance_before,
        v_balance_after,
        v_biz_key,
        'CHAPTER',
        p_chapter_id,
        v_request_no,
        CONCAT('订阅章节 ', p_chapter_id)
    );

    SET p_result_code = 'SUCCESS';
    SET p_message = '订阅扣费成功';
    SET p_txn_no = v_txn_no;
    SET p_balance_after = v_balance_after;

    COMMIT;
END proc $$


-- ============================================================================
-- 2. 打赏
--
-- 并发安全设计：
--   (1) request_no 唯一索引保证同一请求只生成一条打赏；
--   (2) 双方账户按 user_id 从小到大加排他锁，避免 A→B / B→A 交叉死锁；
--   (3) 打赏者扣款、作者入账、两条流水在同一事务内完成；
--   (4) TIP_OUT / TIP_IN 分别以 biz_key 唯一，防止重复记账；
--   (5) 数据库侧校验 p_to_user_id 必须是 novel.author_id。
--
-- 输出：
--   p_result_code       SUCCESS / DUPLICATE_REQUEST
--   p_result_message    结果说明
--   p_tip_id            打赏 ID
--   p_tip_no            打赏单号
--   p_from_balance_after 打赏者余额
--   p_to_balance_after   作者余额
-- ============================================================================
DROP PROCEDURE IF EXISTS `sp_tip_author` $$
CREATE PROCEDURE `sp_tip_author`(
    IN  p_from_user_id BIGINT UNSIGNED,
    IN  p_to_user_id   BIGINT UNSIGNED,
    IN  p_novel_id     BIGINT UNSIGNED,
    IN  p_chapter_id   BIGINT UNSIGNED,
    IN  p_amount       DECIMAL(12,2),
    IN  p_message      VARCHAR(200),
    IN  p_request_no   VARCHAR(64),
    OUT p_result_code       VARCHAR(32),
    OUT p_result_message    VARCHAR(255),
    OUT p_tip_id            BIGINT UNSIGNED,
    OUT p_tip_no            CHAR(32),
    OUT p_from_balance_after DECIMAL(12,2),
    OUT p_to_balance_after   DECIMAL(12,2)
)
    MODIFIES SQL DATA
    SQL SECURITY INVOKER
    COMMENT '作者打赏：事务+双方账户按序加锁+作者归属校验+双流水'
proc: BEGIN
    DECLARE v_from_status       VARCHAR(16);
    DECLARE v_to_status         VARCHAR(16);
    DECLARE v_from_balance      DECIMAL(12,2);
    DECLARE v_to_balance        DECIMAL(12,2);
    DECLARE v_from_after        DECIMAL(12,2);
    DECLARE v_to_after          DECIMAL(12,2);
    DECLARE v_tip_no            CHAR(32);
    DECLARE v_out_txn_no        CHAR(32);
    DECLARE v_in_txn_no         CHAR(32);
    DECLARE v_request_no        VARCHAR(64);
    DECLARE v_message           VARCHAR(200);
    DECLARE v_author_id         BIGINT UNSIGNED;

    DECLARE v_existing_tip_id   BIGINT UNSIGNED;
    DECLARE v_existing_tip_no   CHAR(32);
    DECLARE v_existing_from     BIGINT UNSIGNED;
    DECLARE v_existing_to       BIGINT UNSIGNED;
    DECLARE v_existing_novel    BIGINT UNSIGNED;
    DECLARE v_existing_chapter  BIGINT UNSIGNED;
    DECLARE v_existing_amount   DECIMAL(12,2);
    DECLARE v_existing_status   VARCHAR(16);

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET p_result_code = 'FAILED';
    SET p_result_message = '';
    SET p_tip_id = NULL;
    SET p_tip_no = NULL;
    SET p_from_balance_after = NULL;
    SET p_to_balance_after = NULL;

    -- ---------- 参数校验 ----------
    IF p_from_user_id IS NULL OR p_from_user_id = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '打赏者 user_id 不能为空';
    END IF;

    IF p_to_user_id IS NULL OR p_to_user_id = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '作者 user_id 不能为空';
    END IF;

    IF p_from_user_id = p_to_user_id THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '不能给自己打赏';
    END IF;

    IF p_novel_id IS NULL OR p_novel_id = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'novel_id 不能为空';
    END IF;

    IF p_chapter_id IS NOT NULL AND p_chapter_id = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'chapter_id 非法';
    END IF;

    IF p_amount IS NULL OR p_amount <= 0.00 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '打赏金额必须大于 0';
    END IF;

    SET v_message = COALESCE(TRIM(p_message), '');

    IF CHAR_LENGTH(v_message) > 200 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '打赏留言不能超过 200 字';
    END IF;

    SET v_request_no = COALESCE(
        NULLIF(TRIM(p_request_no), ''),
        CONCAT('TIP-', REPLACE(UUID(), '-', ''))
    );

    IF CHAR_LENGTH(v_request_no) > 64 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'request_no 长度不能超过 64';
    END IF;

    -- ---------- 事务开始 ----------
    START TRANSACTION;

    -- 幂等快速路径：同一 request_no 已成功处理，直接返回原结果，不再扣款。
    SELECT `tip_id`, `tip_no`, `from_user_id`, `to_user_id`,
           `novel_id`, `chapter_id`, `amount`, `status`
      INTO v_existing_tip_id, v_existing_tip_no, v_existing_from,
           v_existing_to, v_existing_novel, v_existing_chapter,
           v_existing_amount, v_existing_status
      FROM `reward_tip`
     WHERE `request_no` = v_request_no;

    IF v_existing_tip_id IS NOT NULL THEN
        IF v_existing_from <> p_from_user_id
           OR v_existing_to <> p_to_user_id
           OR v_existing_novel <> p_novel_id
           OR NOT (v_existing_chapter <=> p_chapter_id)
           OR v_existing_amount <> p_amount THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '同一 request_no 的参数与已有打赏不一致';
        END IF;

        SELECT `balance` INTO p_from_balance_after
          FROM `user_account` WHERE `user_id` = p_from_user_id;
        SELECT `balance` INTO p_to_balance_after
          FROM `user_account` WHERE `user_id` = p_to_user_id;

        SET p_result_code = 'DUPLICATE_REQUEST';
        SET p_result_message = CONCAT('该请求已处理，打赏状态：', v_existing_status);
        SET p_tip_id = v_existing_tip_id;
        SET p_tip_no = v_existing_tip_no;
        COMMIT;
        LEAVE proc;
    END IF;

    -- 校验双方账号状态。
    SELECT `status` INTO v_from_status
      FROM `app_user` WHERE `user_id` = p_from_user_id;

    SELECT `status` INTO v_to_status
      FROM `app_user` WHERE `user_id` = p_to_user_id;

    IF v_from_status IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '打赏者不存在';
    END IF;

    IF v_to_status IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '作者不存在';
    END IF;

    IF v_from_status <> 'ACTIVE' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '打赏者状态不可用';
    END IF;

    IF v_to_status <> 'ACTIVE' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '作者状态不可用';
    END IF;

    -- 数据库侧校验收款人必须是作品作者：
    --   章节打赏：chapter.novel_id 必须匹配 p_novel_id，且 novel.author_id = p_to_user_id；
    --   整本书打赏：chapter_id 为 NULL，novel.author_id = p_to_user_id。
    -- 不能只依赖 API，否则可能把钱打给非作者账户。
    IF p_chapter_id IS NULL THEN
        SELECT `author_id`
          INTO v_author_id
          FROM `novel`
         WHERE `novel_id` = p_novel_id;
    ELSE
        SELECT n.`author_id`
          INTO v_author_id
          FROM `chapter` AS c
          JOIN `novel` AS n ON n.`novel_id` = c.`novel_id`
         WHERE c.`chapter_id` = p_chapter_id
           AND c.`novel_id` = p_novel_id;
    END IF;

    IF v_author_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '作品或章节不存在，或章节不属于该小说';
    END IF;

    IF v_author_id <> p_to_user_id THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '收款人必须是该作品/章节的作者';
    END IF;

    SET v_tip_no = REPLACE(UUID(), '-', '');
    SET v_out_txn_no = REPLACE(UUID(), '-', '');
    SET v_in_txn_no = REPLACE(UUID(), '-', '');

    -- 先占用 request_no 唯一键，再锁账户；并发同请求会在唯一键上排队。
    INSERT INTO `reward_tip` (
        `tip_no`,
        `request_no`,
        `from_user_id`,
        `to_user_id`,
        `novel_id`,
        `chapter_id`,
        `amount`,
        `message`,
        `status`,
        `out_txn_no`,
        `in_txn_no`,
        `created_at`
    ) VALUES (
        v_tip_no,
        v_request_no,
        p_from_user_id,
        p_to_user_id,
        p_novel_id,
        p_chapter_id,
        p_amount,
        v_message,
        'SUCCESS',
        v_out_txn_no,
        v_in_txn_no,
        CURRENT_TIMESTAMP(6)
    );

    SET p_tip_id = LAST_INSERT_ID();

    -- 固定加锁顺序：user_id 小的先锁。A→B 与 B→A 并发时不会交叉等待。
    IF p_from_user_id < p_to_user_id THEN
        SELECT `balance` INTO v_from_balance
          FROM `user_account` WHERE `user_id` = p_from_user_id
          FOR UPDATE;

        SELECT `balance` INTO v_to_balance
          FROM `user_account` WHERE `user_id` = p_to_user_id
          FOR UPDATE;
    ELSE
        SELECT `balance` INTO v_to_balance
          FROM `user_account` WHERE `user_id` = p_to_user_id
          FOR UPDATE;

        SELECT `balance` INTO v_from_balance
          FROM `user_account` WHERE `user_id` = p_from_user_id
          FOR UPDATE;
    END IF;

    IF v_from_balance IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '打赏者账户不存在';
    END IF;

    IF v_to_balance IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '作者账户不存在';
    END IF;

    IF v_from_balance < p_amount THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '账户余额不足';
    END IF;

    SET v_from_after = v_from_balance - p_amount;
    SET v_to_after = v_to_balance + p_amount;

    UPDATE `user_account`
       SET `balance` = v_from_after,
           `updated_at` = CURRENT_TIMESTAMP(6)
     WHERE `user_id` = p_from_user_id;

    UPDATE `user_account`
       SET `balance` = v_to_after,
           `updated_at` = CURRENT_TIMESTAMP(6)
     WHERE `user_id` = p_to_user_id;

    -- 打赏者支出流水
    INSERT INTO `account_transaction` (
        `txn_no`,
        `user_id`,
        `biz_type`,
        `direction`,
        `amount`,
        `balance_before`,
        `balance_after`,
        `biz_key`,
        `related_type`,
        `related_id`,
        `request_no`,
        `remark`
    ) VALUES (
        v_out_txn_no,
        p_from_user_id,
        'TIP_OUT',
        'OUT',
        p_amount,
        v_from_balance,
        v_from_after,
        CONCAT('TIP_OUT:', p_tip_id),
        'TIP',
        p_tip_id,
        v_request_no,
        CONCAT('打赏作者 ', p_to_user_id)
    );

    -- 作者收入流水
    INSERT INTO `account_transaction` (
        `txn_no`,
        `user_id`,
        `biz_type`,
        `direction`,
        `amount`,
        `balance_before`,
        `balance_after`,
        `biz_key`,
        `related_type`,
        `related_id`,
        `request_no`,
        `remark`
    ) VALUES (
        v_in_txn_no,
        p_to_user_id,
        'TIP_IN',
        'IN',
        p_amount,
        v_to_balance,
        v_to_after,
        CONCAT('TIP_IN:', p_tip_id),
        'TIP',
        p_tip_id,
        v_request_no,
        CONCAT('收到用户 ', p_from_user_id, ' 的打赏')
    );

    SET p_result_code = 'SUCCESS';
    SET p_result_message = '打赏成功';
    SET p_tip_no = v_tip_no;
    SET p_from_balance_after = v_from_after;
    SET p_to_balance_after = v_to_after;

    COMMIT;
END proc $$


DELIMITER ;


-- ============================================================================
-- 3. 最小验收调用示例
--
-- 订阅（OUT 参数需用会话变量接收）：
--   SET @code = NULL; SET @msg = NULL; SET @txn = NULL; SET @bal = NULL;
--   CALL sp_subscribe_chapter(
--       1, 10001, 'sub-req-0001',
--       @code, @msg, @txn, @bal
--   );
--   SELECT @code, @msg, @txn, @bal;
--
-- 打赏：
--   SET @code = NULL; SET @msg = NULL; SET @tip_id = NULL;
--   SET @tip_no = NULL; SET @from_bal = NULL; SET @to_bal = NULL;
--   CALL sp_tip_author(
--       1, 2, 1001, 10001, 5.00, '写得好', 'tip-req-0001',
--       @code, @msg, @tip_id, @tip_no, @from_bal, @to_bal
--   );
--   SELECT @code, @msg, @tip_id, @tip_no, @from_bal, @to_bal;
--
-- 对账（应返回 0 行；返回行即 MISMATCH）：
--   SELECT * FROM v_user_balance_reconciliation
--    WHERE reconcile_status <> 'MATCH';
-- ============================================================================
-- ============================================================================
-- 第七部分：B 存储过程
-- 来源：sql/objects/B_作品与内容_存储过程_v1.0_20261123.sql
-- 说明：B 索引已随建表创建，不整合仅含核验/EXPLAIN 的索引脚本。
-- ============================================================================

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
-- ============================================================================
-- 第八部分：C 触发器
-- 来源：sql/objects/C_互动与榜单_触发器_v1.0_20261121.sql
-- ============================================================================

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
-- ============================================================================
-- 第九部分：C 视图与存储过程
-- 来源：sql/objects/C_互动与榜单_视图与存储过程_v1.0_20261121.sql
-- ============================================================================

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
-- ============================================================================
-- 全局初始化结束
-- 预期：19 表、5 视图、15 存储过程、8 触发器、0 函数。
-- 本文件仅用于全新数据库首次初始化，不承诺重复执行。
-- ============================================================================
