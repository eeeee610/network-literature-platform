-- ============================================================================
-- 全局数据库跨模块集成测试（仅限独立测试库）
-- 实例：127.0.0.1:33307；账号：novel_test；禁止用于 novel_platform。
-- A/B 过程自行管理事务；C 公共过程由本脚本显式管理事务。
-- 脚本末尾仅清理本次测试数据，不删除测试数据库、不修改业务对象。
-- ============================================================================
SET NAMES utf8mb4;
USE `novel_platform_it_20261123_01`;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TEMPORARY TABLE it_results (
  result_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  case_id VARCHAR(20) NOT NULL,
  case_name VARCHAR(120) NOT NULL,
  status VARCHAR(8) NOT NULL,
  detail VARCHAR(500) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (result_id)
) ENGINE = MEMORY;

DROP PROCEDURE IF EXISTS sp_it_assert;
DELIMITER $$
CREATE PROCEDURE sp_it_assert(
  IN p_case_id VARCHAR(20),
  IN p_case_name VARCHAR(120),
  IN p_condition BOOLEAN,
  IN p_detail VARCHAR(500)
)
BEGIN
  IF p_condition IS NULL OR p_condition = 0 THEN
    INSERT INTO it_results(case_id, case_name, status, detail)
    VALUES (p_case_id, p_case_name, 'FAIL', p_detail);
    SELECT CONCAT('FAIL|', p_case_id, '|', p_case_name, '|', p_detail) AS test_result;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = p_case_id;
  ELSE
    INSERT INTO it_results(case_id, case_name, status, detail)
    VALUES (p_case_id, p_case_name, 'PASS', p_detail);
    SELECT CONCAT('PASS|', p_case_id, '|', p_case_name, '|', p_detail) AS test_result;
  END IF;
END$$
DELIMITER ;

CALL sp_it_assert('ENV01', '独立实例与账号', @@port = 33307
  AND DATABASE() = 'novel_platform_it_20261123_01'
  AND CURRENT_USER() = 'novel_test@127.0.0.1',
  '端口、数据库及执行账号必须完全匹配');
CALL sp_it_assert('ENV02', '部署对象基线',
  (SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE') = 19
  AND (SELECT COUNT(*) FROM information_schema.ROUTINES
    WHERE ROUTINE_SCHEMA = DATABASE() AND ROUTINE_TYPE = 'PROCEDURE'
      AND ROUTINE_NAME NOT LIKE 'sp\_it\_%') = 15
  AND (SELECT COUNT(*) FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA = DATABASE()) = 8,
  '19 表、15 业务过程、8 触发器');

SET @it_author  = 910000000000000001;
SET @it_reader1 = 910000000000000002;
SET @it_reader2 = 910000000000000003;
SET @it_admin   = 910000000000000004;
SET @it_reader3 = 910000000000000005;
SET @it_poor    = 910000000000000006;

CALL sp_it_assert('ENV03', '测试标识不存在',
  (SELECT COUNT(*) FROM app_user WHERE user_id BETWEEN @it_author AND @it_poor) = 0
  AND (SELECT COUNT(*) FROM novel WHERE title LIKE 'IT跨模块测试%') = 0
  AND (SELECT COUNT(*) FROM ranking_config WHERE ranking_code LIKE 'IT\_%') = 0,
  '防止覆盖既有测试或其他数据');

-- ============================================================================
-- 1. 用户与账户：直接 SQL 模拟应用层注册事务及充值测试夹具。
-- 实际注册接口、密码哈希和支付充值仍待后端联调。
-- ============================================================================
START TRANSACTION;
INSERT INTO app_user(user_id, username, password_hash, email, nickname, role) VALUES
(@it_author,  'it_author_20261123',  '$2a$10$integration_test_hash_20261123', 'it_author_20261123@example.test',  'IT作者', 'AUTHOR'),
(@it_reader1, 'it_reader1_20261123', '$2a$10$integration_test_hash_20261123', 'it_reader1_20261123@example.test', 'IT读者一', 'READER'),
(@it_reader2, 'it_reader2_20261123', '$2a$10$integration_test_hash_20261123', 'it_reader2_20261123@example.test', 'IT读者二', 'READER'),
(@it_admin,   'it_admin_20261123',   '$2a$10$integration_test_hash_20261123', 'it_admin_20261123@example.test',   'IT管理员', 'ADMIN'),
(@it_reader3, 'it_reader3_20261123', '$2a$10$integration_test_hash_20261123', 'it_reader3_20261123@example.test', 'IT读者三', 'READER'),
(@it_poor,    'it_poor_20261123',    '$2a$10$integration_test_hash_20261123', 'it_poor_20261123@example.test',    'IT低余额', 'READER');
INSERT INTO user_account(user_id, balance) VALUES
(@it_author, 0.00), (@it_reader1, 100.00), (@it_reader2, 50.00),
(@it_admin, 0.00), (@it_reader3, 20.00), (@it_poor, 1.00);
COMMIT;

CALL sp_it_assert('U01', '用户账户一对一关联',
  (SELECT COUNT(*) FROM app_user WHERE user_id BETWEEN @it_author AND @it_poor) = 6
  AND (SELECT COUNT(*) FROM user_account WHERE user_id BETWEEN @it_author AND @it_poor) = 6
  AND (SELECT COUNT(*) FROM app_user u LEFT JOIN user_account a ON a.user_id=u.user_id
       WHERE u.user_id BETWEEN @it_author AND @it_poor AND a.user_id IS NULL) = 0,
  '6 个测试用户均有唯一账户');

-- 充值流水仅用于构造可追溯的初始余额，不代表充值接口测试。
INSERT INTO account_transaction
(txn_no,user_id,biz_type,direction,amount,balance_before,balance_after,biz_key,related_type,related_id,request_no,remark)
SELECT REPLACE(UUID(),'-',''), x.user_id, 'RECHARGE','IN',x.balance,0.00,x.balance,
       CONCAT('RECHARGE:',x.user_id),'RECHARGE_ORDER',x.user_id,
       CONCAT('IT-RECHARGE-',x.user_id),'跨模块测试初始余额夹具'
FROM user_account x
WHERE x.user_id IN (@it_reader1,@it_reader2,@it_reader3,@it_poor);

DROP PROCEDURE IF EXISTS sp_it_user_duplicate;
DELIMITER $$
CREATE PROCEDURE sp_it_user_duplicate()
BEGIN
  DECLARE v_error TINYINT DEFAULT 0;
  START TRANSACTION;
  BEGIN
    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION SET v_error = 1;
    INSERT INTO app_user(user_id,username,password_hash,nickname)
    VALUES(@it_author + 100,'it_author_20261123','$2a$10$integration_test_hash_20261123','重复用户名');
  END;
  ROLLBACK;
  CALL sp_it_assert('U02', '重复用户名回滚', v_error = 1
    AND (SELECT COUNT(*) FROM app_user WHERE user_id = @it_author + 100) = 0
    AND (SELECT COUNT(*) FROM user_account WHERE user_id = @it_author + 100) = 0,
    '唯一键冲突被拒绝，用户及账户均无残留');
END$$
DELIMITER ;
CALL sp_it_user_duplicate();

-- ============================================================================
-- 2. 作品发布：B 过程自行管理事务；D 审核通过 SQL 与日志显式同事务。
-- ============================================================================
INSERT INTO tag(tag_name) VALUES ('IT玄幻_20261123'),('IT都市_20261123');
SET @it_tag1 = (SELECT tag_id FROM tag WHERE tag_name='IT玄幻_20261123');
SET @it_tag2 = (SELECT tag_id FROM tag WHERE tag_name='IT都市_20261123');

CALL sp_create_novel(@it_author,'IT跨模块测试小说_20261123',NULL,'跨模块集成测试作品',JSON_ARRAY(@it_tag1,@it_tag2),@it_novel);
CALL sp_it_assert('B01', '作者创建小说与标签',
  @it_novel IS NOT NULL
  AND (SELECT COUNT(*) FROM novel WHERE novel_id=@it_novel AND author_id=@it_author AND audit_status=0) = 1
  AND (SELECT COUNT(*) FROM novel_tag WHERE novel_id=@it_novel) = 2,
  '小说创建成功，两个标签按顺序关联');

DROP PROCEDURE IF EXISTS sp_it_b_invalid_novel;
DROP PROCEDURE IF EXISTS sp_it_b_publish_guard;
DELIMITER $$
CREATE PROCEDURE sp_it_b_invalid_novel(IN p_author BIGINT UNSIGNED)
BEGIN
  DECLARE v_error TINYINT DEFAULT 0;
  DECLARE v_before INT;
  DECLARE v_novel BIGINT UNSIGNED DEFAULT NULL;
  SELECT COUNT(*) INTO v_before FROM novel WHERE title LIKE 'IT跨模块测试%';
  BEGIN
    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION SET v_error = 1;
    CALL sp_create_novel(p_author,'IT跨模块测试_非法标签应回滚',NULL,NULL,JSON_ARRAY(999999999),v_novel);
  END;
  CALL sp_it_assert('B02', '非法标签建书回滚', v_error = 1 AND v_novel IS NULL
    AND (SELECT COUNT(*) FROM novel WHERE title LIKE 'IT跨模块测试%') = v_before
    AND (SELECT COUNT(*) FROM novel_tag nt JOIN novel n ON n.novel_id=nt.novel_id
         WHERE n.title='IT跨模块测试_非法标签应回滚') = 0,
    'B 过程自身回滚，不产生小说或标签关联');
END$$
CREATE PROCEDURE sp_it_b_publish_guard(
  IN p_chapter BIGINT UNSIGNED, IN p_case_id VARCHAR(20), IN p_case_name VARCHAR(120)
)
BEGIN
  DECLARE v_error TINYINT DEFAULT 0;
  DECLARE v_before_status TINYINT;
  DECLARE v_before_words INT UNSIGNED;
  DECLARE v_before_latest BIGINT UNSIGNED;
  DECLARE v_novel BIGINT UNSIGNED;
  SELECT novel_id,status INTO v_novel,v_before_status FROM chapter WHERE chapter_id=p_chapter;
  SELECT total_words,latest_chapter_id INTO v_before_words,v_before_latest FROM novel WHERE novel_id=v_novel;
  BEGIN
    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION SET v_error = 1;
    CALL sp_publish_chapter(p_chapter);
  END;
  CALL sp_it_assert(p_case_id,p_case_name,v_error = 1
    AND (SELECT status FROM chapter WHERE chapter_id=p_chapter) = v_before_status
    AND (SELECT total_words FROM novel WHERE novel_id=v_novel) = v_before_words
    AND (SELECT latest_chapter_id FROM novel WHERE novel_id=v_novel) <=> v_before_latest,
    '发布失败由 B 过程回滚，章节及小说统计保持不变');
END$$
DELIMITER ;
CALL sp_it_b_invalid_novel(@it_author);

CALL sp_save_chapter_draft(NULL,@it_novel,'第一章 免费正文','第一章跨模块正文',0,0.00,@it_chapter1);
CALL sp_save_chapter_draft(NULL,@it_novel,'第二章 VIP正文','第二章跨模块VIP正文',1,2.50,@it_chapter2);
CALL sp_it_assert('B03', '创建章节草稿',
  (SELECT COUNT(*) FROM chapter WHERE novel_id=@it_novel AND status=0 AND audit_status IS NULL) = 2
  AND (SELECT chapter_no FROM chapter WHERE chapter_id=@it_chapter1) = 1
  AND (SELECT chapter_no FROM chapter WHERE chapter_id=@it_chapter2) = 2
  AND (SELECT vip_price FROM chapter WHERE chapter_id=@it_chapter2) = 2.50,
  '两章草稿、序号及 VIP 价格正确');
CALL sp_it_b_publish_guard(@it_chapter2,'B04','未审核章节禁止发布');

START TRANSACTION;
UPDATE novel SET audit_status=1 WHERE novel_id=@it_novel;
INSERT INTO audit_log(admin_id,target_type,target_id,audit_action,audit_reason)
VALUES(@it_admin,'NOVEL',@it_novel,1,'跨模块小说审核通过');
COMMIT;

UPDATE chapter SET audit_status=0 WHERE chapter_id IN (@it_chapter1,@it_chapter2);
START TRANSACTION;
UPDATE chapter SET audit_status=1 WHERE chapter_id=@it_chapter1;
INSERT INTO audit_log(admin_id,target_type,target_id,audit_action,audit_reason)
VALUES(@it_admin,'CHAPTER',@it_chapter1,1,'跨模块第一章审核通过');
UPDATE chapter SET audit_status=1 WHERE chapter_id=@it_chapter2;
INSERT INTO audit_log(admin_id,target_type,target_id,audit_action,audit_reason)
VALUES(@it_admin,'CHAPTER',@it_chapter2,1,'跨模块第二章审核通过');
COMMIT;
CALL sp_it_assert('D01', '小说章节审核与日志',
  (SELECT audit_status FROM novel WHERE novel_id=@it_novel) = 1
  AND (SELECT COUNT(*) FROM chapter WHERE chapter_id IN (@it_chapter1,@it_chapter2) AND audit_status=1) = 2
  AND (SELECT COUNT(*) FROM audit_log WHERE admin_id=@it_admin AND target_type='NOVEL' AND target_id=@it_novel AND audit_action=1) = 1
  AND (SELECT COUNT(*) FROM audit_log WHERE admin_id=@it_admin AND target_type='CHAPTER' AND target_id IN (@it_chapter1,@it_chapter2)) = 2,
  '小说及两章审核通过，3 条审核日志匹配');

CALL sp_publish_chapter(@it_chapter1);
CALL sp_publish_chapter(@it_chapter2);
CALL sp_it_assert('B05', '审核后发布章节',
  (SELECT COUNT(*) FROM chapter WHERE chapter_id IN (@it_chapter1,@it_chapter2)
    AND status=1 AND audit_status=1 AND publish_time IS NOT NULL) = 2
  AND (SELECT total_words FROM novel WHERE novel_id=@it_novel)
      = (SELECT SUM(word_count) FROM chapter WHERE chapter_id IN (@it_chapter1,@it_chapter2))
  AND (SELECT latest_chapter_id FROM novel WHERE novel_id=@it_novel) = @it_chapter2,
  '两章发布成功，总字数及最新章节统计一致');
CALL sp_it_b_publish_guard(@it_chapter2,'B06','重复发布被拒绝且统计不重复');

-- ============================================================================
-- 3. 章节订阅：不得在调用 A 过程外再开启事务。
-- ============================================================================
CALL sp_subscribe_chapter(@it_reader1,@it_chapter2,'IT-SUB-20261123-001',@sub_code,@sub_msg,@sub_txn,@sub_balance);
CALL sp_it_assert('A01', '章节订阅扣款与流水',
  @sub_code='SUCCESS' AND @sub_balance=97.50
  AND (SELECT balance FROM user_account WHERE user_id=@it_reader1)=97.50
  AND (SELECT COUNT(*) FROM chapter_subscription WHERE user_id=@it_reader1 AND chapter_id=@it_chapter2
       AND novel_id=@it_novel AND unit_price=2.50 AND paid_amount=2.50 AND txn_no=@sub_txn AND status='ACTIVE')=1
  AND (SELECT COUNT(*) FROM account_transaction WHERE txn_no=@sub_txn AND user_id=@it_reader1
       AND biz_type='SUBSCRIBE' AND direction='OUT' AND amount=2.50
       AND balance_before=100.00 AND balance_after=97.50 AND related_type='CHAPTER' AND related_id=@it_chapter2)=1,
  '订阅记录、97.50 余额和扣款流水一致');

CALL sp_subscribe_chapter(@it_reader1,@it_chapter2,'IT-SUB-20261123-001',@sub_code2,@sub_msg2,@sub_txn2,@sub_balance2);
CALL sp_it_assert('A02', '重复订阅幂等',
  @sub_code2='ALREADY_SUBSCRIBED' AND @sub_txn2=@sub_txn AND @sub_balance2=97.50
  AND (SELECT COUNT(*) FROM chapter_subscription WHERE user_id=@it_reader1 AND chapter_id=@it_chapter2)=1
  AND (SELECT COUNT(*) FROM account_transaction WHERE user_id=@it_reader1 AND biz_type='SUBSCRIBE')=1,
  '重复订阅返回原流水，不重复扣款');

DROP PROCEDURE IF EXISTS sp_it_a_failed_subscription;
DROP PROCEDURE IF EXISTS sp_it_a_tip_errors;
DELIMITER $$
CREATE PROCEDURE sp_it_a_failed_subscription(IN p_user BIGINT UNSIGNED,IN p_chapter BIGINT UNSIGNED)
BEGIN
  DECLARE v_error TINYINT DEFAULT 0;
  DECLARE v_balance DECIMAL(12,2);
  DECLARE v_code VARCHAR(32); DECLARE v_msg VARCHAR(255);
  DECLARE v_txn CHAR(32); DECLARE v_after DECIMAL(12,2);
  SELECT balance INTO v_balance FROM user_account WHERE user_id=p_user;
  BEGIN
    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION SET v_error=1;
    CALL sp_subscribe_chapter(p_user,p_chapter,'IT-SUB-INSUFFICIENT-20261123',v_code,v_msg,v_txn,v_after);
  END;
  CALL sp_it_assert('A03','余额不足订阅整体回滚',v_error=1
    AND (SELECT balance FROM user_account WHERE user_id=p_user)=v_balance
    AND (SELECT COUNT(*) FROM chapter_subscription WHERE user_id=p_user AND chapter_id=p_chapter)=0
    AND (SELECT COUNT(*) FROM account_transaction WHERE request_no='IT-SUB-INSUFFICIENT-20261123')=0,
    'A 过程回滚，余额、订阅和流水均无变化');
END$$
CREATE PROCEDURE sp_it_a_tip_errors(
  IN p_from BIGINT UNSIGNED,IN p_author BIGINT UNSIGNED,IN p_other BIGINT UNSIGNED,
  IN p_novel BIGINT UNSIGNED,IN p_chapter BIGINT UNSIGNED,IN p_request VARCHAR(64)
)
BEGIN
  DECLARE v_error1 TINYINT DEFAULT 0; DECLARE v_error2 TINYINT DEFAULT 0;
  DECLARE v_from_balance DECIMAL(12,2); DECLARE v_author_balance DECIMAL(12,2);
  DECLARE v_txn_count INT;
  DECLARE v_code VARCHAR(32); DECLARE v_msg VARCHAR(255);
  DECLARE v_tip BIGINT UNSIGNED; DECLARE v_tip_no CHAR(32);
  DECLARE v_from_after DECIMAL(12,2); DECLARE v_to_after DECIMAL(12,2);
  SELECT balance INTO v_from_balance FROM user_account WHERE user_id=p_from;
  SELECT balance INTO v_author_balance FROM user_account WHERE user_id=p_author;
  SELECT COUNT(*) INTO v_txn_count FROM account_transaction WHERE user_id IN (p_from,p_author,p_other);
  BEGIN
    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION SET v_error1=1;
    CALL sp_tip_author(p_from,p_author,p_novel,p_chapter,6.00,'参数篡改',p_request,
      v_code,v_msg,v_tip,v_tip_no,v_from_after,v_to_after);
  END;
  BEGIN
    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION SET v_error2=1;
    CALL sp_tip_author(p_from,p_other,p_novel,p_chapter,1.00,'错误收款人',CONCAT(p_request,'-WRONG'),
      v_code,v_msg,v_tip,v_tip_no,v_from_after,v_to_after);
  END;
  CALL sp_it_assert('A06','打赏参数冲突与错误收款人回滚',v_error1=1 AND v_error2=1
    AND (SELECT balance FROM user_account WHERE user_id=p_from)=v_from_balance
    AND (SELECT balance FROM user_account WHERE user_id=p_author)=v_author_balance
    AND (SELECT COUNT(*) FROM reward_tip WHERE request_no IN (p_request,CONCAT(p_request,'-WRONG')))=1
    AND (SELECT COUNT(*) FROM account_transaction WHERE user_id IN (p_from,p_author,p_other))=v_txn_count,
    '两种异常均未产生额外打赏、流水或余额变化');
END$$
DELIMITER ;
CALL sp_it_a_failed_subscription(@it_poor,@it_chapter2);

-- ============================================================================
-- 4. 作品打赏：A 过程自行提交，测试不包外层事务。
-- ============================================================================
CALL sp_tip_author(@it_reader1,@it_author,@it_novel,@it_chapter2,5.00,'跨模块打赏','IT-TIP-20261123-001',
  @tip_code,@tip_msg,@tip_id,@tip_no,@tip_from_balance,@tip_author_balance);
CALL sp_it_assert('A04', '作品打赏双向余额与流水',
  @tip_code='SUCCESS' AND @tip_from_balance=92.50 AND @tip_author_balance=5.00
  AND (SELECT COUNT(*) FROM reward_tip WHERE tip_id=@tip_id AND tip_no=@tip_no AND from_user_id=@it_reader1
       AND to_user_id=@it_author AND novel_id=@it_novel AND chapter_id=@it_chapter2 AND amount=5.00 AND status='SUCCESS')=1
  AND (SELECT COUNT(*) FROM account_transaction WHERE txn_no=(SELECT out_txn_no FROM reward_tip WHERE tip_id=@tip_id)
       AND biz_type='TIP_OUT' AND direction='OUT' AND balance_before=97.50 AND balance_after=92.50)=1
  AND (SELECT COUNT(*) FROM account_transaction WHERE txn_no=(SELECT in_txn_no FROM reward_tip WHERE tip_id=@tip_id)
       AND biz_type='TIP_IN' AND direction='IN' AND balance_before=0.00 AND balance_after=5.00)=1,
  '读者 92.50、作者 5.00，打赏及两条流水一致');

CALL sp_tip_author(@it_reader1,@it_author,@it_novel,@it_chapter2,5.00,'跨模块打赏','IT-TIP-20261123-001',
  @tip_code2,@tip_msg2,@tip_id2,@tip_no2,@tip_from_balance2,@tip_author_balance2);
CALL sp_it_assert('A05', '重复打赏请求幂等',
  @tip_code2='DUPLICATE_REQUEST' AND @tip_id2=@tip_id AND @tip_no2=@tip_no
  AND @tip_from_balance2=92.50 AND @tip_author_balance2=5.00
  AND (SELECT COUNT(*) FROM reward_tip WHERE request_no='IT-TIP-20261123-001')=1
  AND (SELECT COUNT(*) FROM account_transaction WHERE request_no='IT-TIP-20261123-001')=2,
  '重复请求返回原打赏，不重复记账');
CALL sp_it_a_tip_errors(@it_reader1,@it_author,@it_reader2,@it_novel,@it_chapter2,'IT-TIP-20261123-001');
CALL sp_it_assert('A07', '账户余额流水对账',
  (SELECT COUNT(*) FROM v_user_balance_reconciliation
   WHERE user_id BETWEEN @it_author AND @it_poor AND reconcile_status<>'MATCH')=0,
  '全部测试账户余额与累计流水一致');

-- ============================================================================
-- 5. 评论互动：C 公共过程均由调用方管理事务。
-- ============================================================================
START TRANSACTION;
CALL sp_create_comment(@it_novel,@it_chapter1,@it_reader1,NULL,NULL,'跨模块一级评论',@it_root_comment);
COMMIT;
CALL sp_it_assert('C01', '一级评论提交后待审核',
  @it_root_comment IS NOT NULL
  AND (SELECT COUNT(*) FROM comment WHERE comment_id=@it_root_comment AND audit_status=0 AND status=1)=1
  AND (SELECT COUNT(*) FROM v_comment_visible WHERE comment_id=@it_root_comment)=0,
  '待审核评论已保存但不展示');

START TRANSACTION;
CALL sp_audit_comment(@it_root_comment,1);
INSERT INTO audit_log(admin_id,target_type,target_id,audit_action,audit_reason)
VALUES(@it_admin,'COMMENT',@it_root_comment,1,'一级评论审核通过');
COMMIT;
CALL sp_it_assert('D02', '评论审核展示与日志',
  (SELECT COUNT(*) FROM v_comment_visible WHERE comment_id=@it_root_comment)=1
  AND (SELECT COUNT(*) FROM audit_log WHERE admin_id=@it_admin AND target_type='COMMENT' AND target_id=@it_root_comment)=1,
  '审核通过后展示，日志与状态同事务保存');

START TRANSACTION;
CALL sp_create_comment(@it_novel,@it_chapter1,@it_reader2,@it_root_comment,@it_reader1,'跨模块直接回复',@it_reply);
COMMIT;
CALL sp_it_assert('C02', '待审核回复不计数',
  @it_reply IS NOT NULL
  AND (SELECT reply_count FROM comment WHERE comment_id=@it_root_comment)=0
  AND (SELECT COUNT(*) FROM v_comment_visible WHERE comment_id=@it_reply)=0,
  '直接回复保存，父评论 reply_count=0');

START TRANSACTION;
CALL sp_audit_comment(@it_reply,1);
INSERT INTO audit_log(admin_id,target_type,target_id,audit_action,audit_reason)
VALUES(@it_admin,'COMMENT',@it_reply,1,'直接回复审核通过');
COMMIT;
START TRANSACTION;
CALL sp_audit_comment(@it_reply,1);
COMMIT;
CALL sp_it_assert('C03', '回复审核与重复审核计数',
  (SELECT reply_count FROM comment WHERE comment_id=@it_root_comment)=1
  AND (SELECT COUNT(*) FROM v_comment_visible WHERE comment_id=@it_reply)=1,
  '审核通过计数 +1，重复通过不重复计数');

START TRANSACTION;
CALL sp_set_comment_status(@it_reply,2);
COMMIT;
CALL sp_it_assert('C04', '隐藏回复及计数展示',
  (SELECT reply_count FROM comment WHERE comment_id=@it_root_comment)=0
  AND (SELECT COUNT(*) FROM v_comment_visible WHERE comment_id=@it_reply)=0,
  '隐藏回复计数 -1 且不展示');
START TRANSACTION;
CALL sp_set_comment_status(@it_reply,1);
CALL sp_edit_comment_content(@it_reply,'跨模块直接回复_已编辑');
COMMIT;
CALL sp_it_assert('C05', '回复编辑触发重新审核',
  (SELECT audit_status FROM comment WHERE comment_id=@it_reply)=0
  AND (SELECT reply_count FROM comment WHERE comment_id=@it_root_comment)=0
  AND (SELECT COUNT(*) FROM v_comment_visible WHERE comment_id=@it_reply)=0,
  '触发器将已见回复重新置待审，计数保持 0');
START TRANSACTION;
CALL sp_audit_comment(@it_reply,1);
INSERT INTO audit_log(admin_id,target_type,target_id,audit_action,audit_reason)
VALUES(@it_admin,'COMMENT',@it_reply,1,'编辑后回复复审通过');
COMMIT;

START TRANSACTION;
CALL sp_set_comment_status(@it_root_comment,2);
COMMIT;
CALL sp_it_assert('C06', '父评论隐藏时子回复不展示',
  (SELECT COUNT(*) FROM v_comment_visible WHERE comment_id=@it_root_comment OR parent_id=@it_root_comment)=0
  AND (SELECT reply_count FROM comment WHERE comment_id=@it_root_comment)=1,
  '父子均不展示，子回复记录及 reply_count 保留');
START TRANSACTION;
CALL sp_set_comment_status(@it_root_comment,1);
COMMIT;
CALL sp_it_assert('C07', '父评论恢复展示',
  (SELECT COUNT(*) FROM v_comment_visible WHERE comment_id=@it_root_comment OR parent_id=@it_root_comment)=2,
  '父子评论恢复展示');

DROP PROCEDURE IF EXISTS sp_it_c_nested;
DROP PROCEDURE IF EXISTS sp_it_c_transaction;
DROP PROCEDURE IF EXISTS sp_it_like_duplicate;
DELIMITER $$
CREATE PROCEDURE sp_it_c_nested(IN p_novel BIGINT UNSIGNED,IN p_chapter BIGINT UNSIGNED,IN p_parent_reply BIGINT UNSIGNED)
BEGIN
  DECLARE v_error TINYINT DEFAULT 0;
  DECLARE v_comment BIGINT UNSIGNED DEFAULT NULL;
  DECLARE v_before INT;
  SELECT COUNT(*) INTO v_before FROM comment WHERE novel_id=p_novel;
  START TRANSACTION;
  BEGIN
    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION SET v_error=1;
    CALL sp_create_comment(p_novel,p_chapter,@it_reader3,p_parent_reply,@it_reader2,'非法第三级回复',v_comment);
  END;
  ROLLBACK;
  CALL sp_it_assert('C08','禁止第三级回复且回滚',v_error=1 AND v_comment IS NULL
    AND (SELECT COUNT(*) FROM comment WHERE novel_id=p_novel)=v_before,
    '两级结构限制生效，无评论残留');
END$$
CREATE PROCEDURE sp_it_c_transaction(IN p_root BIGINT UNSIGNED,IN p_reply BIGINT UNSIGNED)
BEGIN
  DECLARE v_error TINYINT DEFAULT 0;
  DECLARE v_step TINYINT DEFAULT 0;
  DECLARE v_logs INT;
  START TRANSACTION;
  CALL sp_set_comment_status(p_reply,2);
  ROLLBACK;
  CALL sp_it_assert('C09','评论调用方事务回滚',
    (SELECT status FROM comment WHERE comment_id=p_reply)=1
    AND (SELECT reply_count FROM comment WHERE comment_id=p_root)=1,
    'C 过程未隐式提交，状态及计数均恢复');
  SELECT COUNT(*) INTO v_logs FROM audit_log WHERE target_type='COMMENT' AND target_id=p_reply;
  START TRANSACTION;
  BEGIN
    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION SET v_error=1;
    CALL sp_audit_comment(p_reply,2);
    SET v_step=1;
    INSERT INTO audit_log(admin_id,target_type,target_id,audit_action,audit_reason)
    VALUES(@it_admin,'COMMENT',p_reply,2,NULL);
  END;
  ROLLBACK;
  CALL sp_it_assert('D03','审核日志失败时评论审核整体回滚',v_error=1 AND v_step=1
    AND (SELECT audit_status FROM comment WHERE comment_id=p_reply)=1
    AND (SELECT reply_count FROM comment WHERE comment_id=p_root)=1
    AND (SELECT COUNT(*) FROM audit_log WHERE target_type='COMMENT' AND target_id=p_reply)=v_logs,
    '非法驳回日志触发 CHECK，评论状态、计数和日志全部回滚');
END$$
CREATE PROCEDURE sp_it_like_duplicate(IN p_comment BIGINT UNSIGNED)
BEGIN
  DECLARE v_error TINYINT DEFAULT 0;
  BEGIN
    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION SET v_error=1;
    INSERT INTO comment_like(comment_id,user_id) VALUES(p_comment,@it_reader2);
  END;
  CALL sp_it_assert('C11','重复点赞唯一约束与计数稳定',v_error=1
    AND (SELECT COUNT(*) FROM comment_like WHERE comment_id=p_comment AND user_id=@it_reader2)=1
    AND (SELECT like_count FROM comment WHERE comment_id=p_comment)=1,
    '重复点赞被拒绝，like_count 不重复增加');
END$$
DELIMITER ;
CALL sp_it_c_nested(@it_novel,@it_chapter1,@it_reply);
CALL sp_it_c_transaction(@it_root_comment,@it_reply);

INSERT INTO comment_like(comment_id,user_id) VALUES(@it_root_comment,@it_reader2);
CALL sp_it_assert('C10', '点赞触发器增加计数',
  (SELECT like_count FROM comment WHERE comment_id=@it_root_comment)=1,
  'comment_like INSERT 后 like_count=1');
CALL sp_it_like_duplicate(@it_root_comment);
DELETE FROM comment_like WHERE comment_id=@it_root_comment AND user_id=@it_reader2;
CALL sp_it_assert('C12', '取消点赞触发器减少计数',
  (SELECT like_count FROM comment WHERE comment_id=@it_root_comment)=0
  AND (SELECT COUNT(*) FROM comment_like WHERE comment_id=@it_root_comment)=0,
  'DELETE 后 like_count=0');

-- ============================================================================
-- 6. 小说评分：直接写入评分表以验证 5 个评分触发器。
-- ============================================================================
INSERT INTO novel_rating(user_id,novel_id,score,review_content) VALUES
(@it_reader1,@it_novel,5,'跨模块优秀短评'),
(@it_reader2,@it_novel,4,NULL),
(@it_reader3,@it_novel,3,'');
CALL sp_it_assert('R01', '评分新增及短评触发器',
  (SELECT COUNT(*) FROM novel_rating_stat WHERE novel_id=@it_novel AND rating_count=3 AND score_total=12 AND avg_score=4.00)=1
  AND (SELECT review_audit_status FROM novel_rating WHERE user_id=@it_reader1 AND novel_id=@it_novel)=0
  AND (SELECT review_content IS NULL AND review_audit_status IS NULL FROM novel_rating WHERE user_id=@it_reader3 AND novel_id=@it_novel)=1,
  '3 人共 12 分平均 4.00；短评待审，空串规范化 NULL');

DROP PROCEDURE IF EXISTS sp_it_rating_guards;
DELIMITER $$
CREATE PROCEDURE sp_it_rating_guards(IN p_novel BIGINT UNSIGNED)
BEGIN
  DECLARE v_error1 TINYINT DEFAULT 0; DECLARE v_error2 TINYINT DEFAULT 0;
  BEGIN
    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION SET v_error1=1;
    INSERT INTO novel_rating(user_id,novel_id,score) VALUES(@it_reader1,p_novel,4);
  END;
  BEGIN
    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION SET v_error2=1;
    INSERT INTO novel_rating(user_id,novel_id,score) VALUES(@it_poor,p_novel,0);
  END;
  CALL sp_it_assert('R02','重复评分与非法分数被拒绝',v_error1=1 AND v_error2=1
    AND (SELECT COUNT(*) FROM novel_rating WHERE novel_id=p_novel)=3
    AND (SELECT COUNT(*) FROM novel_rating_stat WHERE novel_id=p_novel AND rating_count=3 AND score_total=12 AND avg_score=4.00)=1,
    '唯一键和 CHECK 生效，统计未被异常操作改变');
END$$
DELIMITER ;
CALL sp_it_rating_guards(@it_novel);

UPDATE novel_rating SET score=5 WHERE user_id=@it_reader3 AND novel_id=@it_novel;
CALL sp_it_assert('R03', '修改评分更新统计',
  (SELECT COUNT(*) FROM novel_rating_stat WHERE novel_id=@it_novel AND rating_count=3 AND score_total=14 AND avg_score=4.67)=1,
  '改分后 3 人共 14 分平均 4.67');
UPDATE novel_rating SET status=2 WHERE user_id=@it_reader3 AND novel_id=@it_novel;
CALL sp_it_assert('R04', '隐藏评分扣减统计',
  (SELECT COUNT(*) FROM novel_rating_stat WHERE novel_id=@it_novel AND rating_count=2 AND score_total=9 AND avg_score=4.50)=1,
  '隐藏后仅统计两条有效评分');

START TRANSACTION;
UPDATE novel_rating SET review_audit_status=1 WHERE user_id=@it_reader1 AND novel_id=@it_novel;
INSERT INTO audit_log(admin_id,target_type,target_id,audit_action,audit_reason)
SELECT @it_admin,'RATING',rating_id,1,'评分短评审核通过'
FROM novel_rating WHERE user_id=@it_reader1 AND novel_id=@it_novel;
COMMIT;
UPDATE novel_rating SET review_content='跨模块优秀短评_修改' WHERE user_id=@it_reader1 AND novel_id=@it_novel;
CALL sp_it_assert('D04', '短评修改触发重新审核且不改分数',
  (SELECT review_audit_status FROM novel_rating WHERE user_id=@it_reader1 AND novel_id=@it_novel)=0
  AND (SELECT COUNT(*) FROM novel_rating_stat WHERE novel_id=@it_novel AND rating_count=2 AND score_total=9 AND avg_score=4.50)=1,
  '短评修改后待审，数值评分统计不变');
START TRANSACTION;
UPDATE novel_rating SET review_audit_status=2 WHERE user_id=@it_reader1 AND novel_id=@it_novel;
INSERT INTO audit_log(admin_id,target_type,target_id,audit_action,audit_reason)
SELECT @it_admin,'RATING',rating_id,2,'跨模块短评驳回测试'
FROM novel_rating WHERE user_id=@it_reader1 AND novel_id=@it_novel;
COMMIT;
CALL sp_it_assert('D05', '评分短评审核日志及统计独立',
  (SELECT COUNT(*) FROM audit_log l JOIN novel_rating r ON r.rating_id=l.target_id
   WHERE l.admin_id=@it_admin AND l.target_type='RATING' AND r.user_id=@it_reader1 AND r.novel_id=@it_novel)=2
  AND (SELECT COUNT(*) FROM novel_rating_stat WHERE novel_id=@it_novel AND rating_count=2 AND score_total=9 AND avg_score=4.50)=1,
  '通过、驳回两条日志存在，短评审核不改变评分统计');

DELETE FROM novel_rating WHERE user_id=@it_reader3 AND novel_id=@it_novel;
START TRANSACTION;
CALL sp_refresh_rating_stat(@it_novel);
COMMIT;
CALL sp_it_assert('R05', '删除评分及全量对账',
  (SELECT COUNT(*) FROM novel_rating_stat WHERE novel_id=@it_novel AND rating_count=2 AND score_total=9 AND avg_score=4.50)=1
  AND (SELECT COUNT(*) FROM v_novel_rating_summary WHERE novel_id=@it_novel AND rating_count=2 AND score_total=9 AND avg_score=4.50)=1,
  '删除触发器与全量重算结果一致，汇总视图匹配');
CALL sp_it_assert('D06', '四类内容审核日志齐全',
  (SELECT COUNT(DISTINCT target_type) FROM audit_log WHERE admin_id=@it_admin)=4
  AND (SELECT COUNT(*) FROM audit_log WHERE admin_id=@it_admin AND audit_action=2 AND (audit_reason IS NULL OR TRIM(audit_reason)=''))=0,
  'NOVEL/CHAPTER/COMMENT/RATING 均有日志，驳回原因非空');

-- ============================================================================
-- 7. 榜单：收藏、评分、打赏、订阅四类指标；C 由调用方管理事务。
-- ============================================================================
INSERT INTO user_bookshelf(user_id,novel_id,last_read_chapter_id) VALUES
(@it_reader1,@it_novel,@it_chapter2),(@it_reader2,@it_novel,@it_chapter1),(@it_reader3,@it_novel,@it_chapter1);
INSERT INTO ranking_config(ranking_code,ranking_name,ranking_type,period_type,top_n,weight_config) VALUES
('IT_FAVORITE_ALL','IT总收藏榜','FAVORITE','ALL',10,JSON_OBJECT('favorite',1.0)),
('IT_RATING_ALL','IT总评分榜','RATING','ALL',10,JSON_OBJECT('rating',1.0)),
('IT_TICKET_ALL','IT总打赏榜','TICKET','ALL',10,JSON_OBJECT('ticket',1.0)),
('IT_SALE_ALL','IT总订阅榜','SALE','ALL',10,JSON_OBJECT('sale',1.0));
SET @it_rank_fav=(SELECT ranking_id FROM ranking_config WHERE ranking_code='IT_FAVORITE_ALL');
SET @it_rank_rating=(SELECT ranking_id FROM ranking_config WHERE ranking_code='IT_RATING_ALL');
SET @it_rank_tip=(SELECT ranking_id FROM ranking_config WHERE ranking_code='IT_TICKET_ALL');
SET @it_rank_sale=(SELECT ranking_id FROM ranking_config WHERE ranking_code='IT_SALE_ALL');

START TRANSACTION; CALL sp_generate_ranking(@it_rank_fav,'ALL',0,@it_cnt_fav); COMMIT;
START TRANSACTION; CALL sp_generate_ranking(@it_rank_rating,'ALL',0,@it_cnt_rating); COMMIT;
START TRANSACTION; CALL sp_generate_ranking(@it_rank_tip,'ALL',0,@it_cnt_tip); COMMIT;
START TRANSACTION; CALL sp_generate_ranking(@it_rank_sale,'ALL',0,@it_cnt_sale); COMMIT;
CALL sp_it_assert('L01', '四类榜单生成与指标关联',
  @it_cnt_fav=1 AND @it_cnt_rating=1 AND @it_cnt_tip=1 AND @it_cnt_sale=1
  AND (SELECT score FROM ranking_item WHERE ranking_id=@it_rank_fav AND period_no='ALL' AND novel_id=@it_novel AND rank_no=1)=3.0000
  AND (SELECT score FROM ranking_item WHERE ranking_id=@it_rank_rating AND period_no='ALL' AND novel_id=@it_novel)=4.5000
  AND (SELECT score FROM ranking_item WHERE ranking_id=@it_rank_tip AND period_no='ALL' AND novel_id=@it_novel)=5.0000
  AND (SELECT score FROM ranking_item WHERE ranking_id=@it_rank_sale AND period_no='ALL' AND novel_id=@it_novel)=2.5000
  AND (SELECT COUNT(*) FROM v_ranking_item_detail WHERE ranking_id IN (@it_rank_fav,@it_rank_rating,@it_rank_tip,@it_rank_sale)
       AND novel_id=@it_novel AND novel_title='IT跨模块测试小说_20261123')=4,
  '收藏 3、评分 4.50、打赏 5.00、订阅 2.50，书名关联正确');

CALL sp_get_ranking('IT_FAVORITE_ALL',NULL,10);
CALL sp_get_ranking('IT_RATING_ALL','ALL',10);
CALL sp_get_ranking('IT_TICKET_ALL',NULL,10);
CALL sp_get_ranking('IT_SALE_ALL','ALL',10);
CALL sp_it_assert('L02', '榜单查询与最新期次视图',
  (SELECT COUNT(*) FROM v_ranking_latest WHERE ranking_id IN (@it_rank_fav,@it_rank_rating,@it_rank_tip,@it_rank_sale))=4,
  '四个 sp_get_ranking 调用成功，最新期次视图各一条');

START TRANSACTION; CALL sp_generate_ranking(@it_rank_fav,'ALL',0,@it_cnt_fav_repeat); COMMIT;
CALL sp_it_assert('L03', 'ALL 榜单重复生成不重复',
  @it_cnt_fav_repeat=1
  AND (SELECT COUNT(*) FROM ranking_item WHERE ranking_id=@it_rank_fav AND period_no='ALL')=1,
  '删除旧快照与插入新快照一致，无重复名次');

DROP PROCEDURE IF EXISTS sp_it_ranking_guard;
DELIMITER $$
CREATE PROCEDURE sp_it_ranking_guard(IN p_ranking BIGINT UNSIGNED,IN p_novel BIGINT UNSIGNED)
BEGIN
  DECLARE v_error TINYINT DEFAULT 0;
  DECLARE v_count INT;
  START TRANSACTION;
  INSERT INTO user_bookshelf(user_id,novel_id) VALUES(@it_poor,p_novel);
  CALL sp_generate_ranking(p_ranking,'ALL',0,v_count);
  ROLLBACK;
  CALL sp_it_assert('L04','榜单调用方事务回滚',
    (SELECT COUNT(*) FROM user_bookshelf WHERE user_id=@it_poor AND novel_id=p_novel)=0
    AND (SELECT COUNT(*) FROM ranking_item WHERE ranking_id=p_ranking AND period_no='ALL' AND score=3.0000)=1,
    '新增收藏及榜单重建均随外层回滚，旧快照保留');
  START TRANSACTION;
  BEGIN
    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION SET v_error=1;
    CALL sp_generate_ranking(p_ranking,'202613',0,v_count);
  END;
  ROLLBACK;
  CALL sp_it_assert('L05','非法榜单期次被拒绝且快照不变',v_error=1
    AND (SELECT COUNT(*) FROM ranking_item WHERE ranking_id=p_ranking)=1
    AND (SELECT COUNT(*) FROM ranking_item WHERE ranking_id=p_ranking AND period_no='202613')=0,
    'ALL 榜单拒绝非法期次，无错误快照残留');
END$$
DELIMITER ;
CALL sp_it_ranking_guard(@it_rank_fav,@it_novel);

-- ============================================================================
-- 8. 清理：先删除测试辅助过程，再按本次 ID/编码清理数据；不删数据库。
-- ============================================================================
DROP PROCEDURE IF EXISTS sp_it_user_duplicate;
DROP PROCEDURE IF EXISTS sp_it_b_invalid_novel;
DROP PROCEDURE IF EXISTS sp_it_b_publish_guard;
DROP PROCEDURE IF EXISTS sp_it_a_failed_subscription;
DROP PROCEDURE IF EXISTS sp_it_a_tip_errors;
DROP PROCEDURE IF EXISTS sp_it_c_nested;
DROP PROCEDURE IF EXISTS sp_it_c_transaction;
DROP PROCEDURE IF EXISTS sp_it_like_duplicate;
DROP PROCEDURE IF EXISTS sp_it_rating_guards;
DROP PROCEDURE IF EXISTS sp_it_ranking_guard;

DELETE i FROM ranking_item i JOIN ranking_config c ON c.ranking_id=i.ranking_id
WHERE c.ranking_code IN ('IT_FAVORITE_ALL','IT_RATING_ALL','IT_TICKET_ALL','IT_SALE_ALL');
DELETE FROM ranking_config WHERE ranking_code IN ('IT_FAVORITE_ALL','IT_RATING_ALL','IT_TICKET_ALL','IT_SALE_ALL');
DELETE FROM audit_log WHERE admin_id=@it_admin;
DELETE FROM admin_operation WHERE admin_id=@it_admin;
DELETE l FROM comment_like l JOIN comment c ON c.comment_id=l.comment_id WHERE c.novel_id=@it_novel;
DELETE FROM novel_rating WHERE novel_id=@it_novel;
DELETE FROM novel_rating_stat WHERE novel_id=@it_novel;
DELETE FROM comment WHERE novel_id=@it_novel AND parent_id IS NOT NULL;
DELETE FROM comment WHERE novel_id=@it_novel;
DELETE FROM reward_tip WHERE novel_id=@it_novel;
DELETE FROM chapter_subscription WHERE novel_id=@it_novel;
DELETE FROM account_transaction WHERE user_id BETWEEN @it_author AND @it_poor;
DELETE FROM user_bookshelf WHERE novel_id=@it_novel;
DELETE FROM novel_tag WHERE novel_id=@it_novel;
DELETE FROM chapter WHERE novel_id=@it_novel;
DELETE FROM novel WHERE novel_id=@it_novel;
DELETE FROM tag WHERE tag_id IN (@it_tag1,@it_tag2);
DELETE FROM user_account WHERE user_id BETWEEN @it_author AND @it_poor;
DELETE FROM app_user WHERE user_id BETWEEN @it_author AND @it_poor;

CALL sp_it_assert('CL01', '本次测试数据清理完成',
  (SELECT COUNT(*) FROM app_user WHERE user_id BETWEEN @it_author AND @it_poor)=0
  AND (SELECT COUNT(*) FROM user_account WHERE user_id BETWEEN @it_author AND @it_poor)=0
  AND (SELECT COUNT(*) FROM account_transaction WHERE user_id BETWEEN @it_author AND @it_poor)=0
  AND (SELECT COUNT(*) FROM audit_log WHERE admin_id=@it_admin)=0
  AND (SELECT COUNT(*) FROM novel WHERE novel_id=@it_novel)=0
  AND (SELECT COUNT(*) FROM ranking_config WHERE ranking_code LIKE 'IT\_%')=0
  AND (SELECT COUNT(*) FROM tag WHERE tag_id IN (@it_tag1,@it_tag2))=0,
  '仅本次测试数据被删除，测试数据库及业务对象保留');

SELECT CONCAT('SUMMARY|PASS=',SUM(status='PASS'),'|FAIL=',SUM(status='FAIL'),'|TOTAL=',COUNT(*)) AS summary FROM it_results;
SELECT case_id,case_name,status,detail FROM it_results ORDER BY result_id;
DROP PROCEDURE IF EXISTS sp_it_assert;
SELECT CONCAT('OBJECTS_AFTER_CLEANUP|PROCEDURES=',
  (SELECT COUNT(*) FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA=DATABASE() AND ROUTINE_TYPE='PROCEDURE'),
  '|TRIGGERS=',(SELECT COUNT(*) FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA=DATABASE())) AS object_result;
