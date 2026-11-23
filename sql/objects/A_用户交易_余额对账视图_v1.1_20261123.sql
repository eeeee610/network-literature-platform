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
