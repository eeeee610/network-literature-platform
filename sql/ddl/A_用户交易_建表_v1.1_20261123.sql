-- ============================================================================
-- 文件：sql/ddl/A_用户交易_建表_v1.1_20261123.sql
-- 模块：成员 A —— 用户与交易模块
-- 依据：docs/current/D_数据字典_v1.0_20261120.xlsx（A v0.1 冻结字段）
-- 目标数据库：MySQL 8.0.16+（需要 CHECK 约束）
-- 字符集：utf8mb4 / utf8mb4_0900_ai_ci
--
-- 本文件范围：
--   1. 用户表、用户账户余额表
--   2. 书架表（用户与小说的 M:N，含追更提醒）
--   3. 章节订阅记录表（联合主键防重复扣费）
--   4. 打赏表
--   5. 账户流水表
--
--   * 余额对账视图不在本文件，见
--     sql/objects/A_用户交易_余额对账视图_v1.1_20261123.sql
--
-- 与 B 的依赖（重要）：
--   * novel_id / chapter_id 已按总字典建为物理外键，引用 B 的 novel / chapter。
--   * B 的 novel 又引用 app_user，因此不能把本文件整体放在 B 之前执行。
--     从空库初始化的正确顺序：
--       1) app_user、user_account（本文件）
--       2) novel、chapter（B 的 DDL）
--       3) user_bookshelf、chapter_subscription、reward_tip、account_transaction（本文件）
--     实际由 D 的 schema_all 合并并按上述顺序执行；单独执行本文件前，B 表必须已存在。
--   * 本文件不创建 novel / chapter 表，不写章节发布、榜单计算。
--   * 所有金额均为人民币元，A 模块统一 DECIMAL(12,2)。
--
-- 建议执行顺序（B 表已存在时）：
--   mysql -u<user> -p < sql/ddl/A_用户交易_建表_v1.1_20261123.sql
--   mysql -u<user> -p < sql/objects/A_用户交易_余额对账视图_v1.1_20261123.sql
--   mysql -u<user> -p < sql/objects/A_订阅扣费存储过程_v1.1_20261123.sql
-- ============================================================================

SET NAMES utf8mb4;

-- 组内如已约定其它库名，请同步修改下面两行。
CREATE DATABASE IF NOT EXISTS `novel_platform`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `novel_platform`;


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
-- 7. B 表依赖与执行顺序
--
--    本文件中的 novel_id / chapter_id 已经是物理外键，不再使用注释版 ALTER。
--    因为 novel.author_id 反向引用 app_user，空库初始化时必须拆开执行：
--      1) app_user、user_account
--      2) B 的 novel、chapter
--      3) user_bookshelf、chapter_subscription、reward_tip、account_transaction
--    D 的 schema_all 应按此顺序合并；若手工执行，先确认 novel/chapter 已存在。
-- ============================================================================


-- ============================================================================
-- 8. 最小验收检查
-- ============================================================================
-- 注册用户时，应用层必须在同一事务中执行：
--   INSERT INTO app_user (...) VALUES (...);
--   INSERT INTO user_account (user_id) VALUES (LAST_INSERT_ID());
--
-- 检查表结构：
--   SHOW CREATE TABLE user_bookshelf;
--   SHOW CREATE TABLE chapter_subscription;
--   SHOW CREATE TABLE reward_tip;
--   SHOW CREATE TABLE account_transaction;
--
-- 余额对账视图执行 sql/objects/A_用户交易_余额对账视图_v1.1_20261123.sql
-- 后检查（应全部为 MATCH）：
--   SELECT * FROM v_user_balance_reconciliation
--   WHERE reconcile_status <> 'MATCH';
-- ============================================================================
