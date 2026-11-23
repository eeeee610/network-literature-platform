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
