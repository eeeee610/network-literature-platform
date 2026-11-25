-- =====================================================================
-- A 用户与交易模块 联调测试数据
-- 文件：A_用户交易_联调测试数据_v1.0_20261125.sql
-- 说明：仅用于本地/联调环境，禁止在生产库执行。
--       固定账号密码：admin/admin123（管理员）、reader/reader123、
--       author/author123（作者）。密码为 bcrypt 哈希。
--       脚本包含 1 部作品与 2 个章节的临时数据，仅用于 A 模块
--       订阅、打赏联调；B 模块交付正式测试数据后应删除该部分。
-- =====================================================================

USE novel_platform;

-- 用户（固定 ID，便于联调）
INSERT INTO app_user (user_id, username, password_hash, email, phone, nickname, role, status)
VALUES (1, 'admin', '$2b$10$a1B6RzPdjw6Xa/r9n3fGl.NLd1HuSKmdAWsrGmPXZOKEFA7Q9sri.',
        'admin@novel.local', NULL, '平台管理员', 'ADMIN', 'ACTIVE'),
       (2, 'reader', '$2b$10$UZjGrKyRkjHhUZggneF2/u2Bdh1zFaqvLMb3yR2VrN3uugwRMfL4u',
        'reader@novel.local', NULL, '测试读者', 'READER', 'ACTIVE'),
       (3, 'author', '$2b$10$PXA4TpxMhtStOJPGZi5svuFgbnDMWKBNgnHMP7RvrcJEfyYRGqlKO',
        'author@novel.local', NULL, '夜航', 'AUTHOR', 'ACTIVE')
ON DUPLICATE KEY UPDATE username = VALUES(username);

-- 账户：读者预置 100 元余额，便于订阅与打赏联调
INSERT INTO user_account (user_id, balance)
VALUES (1, 0.00),
       (2, 100.00),
       (3, 0.00)
ON DUPLICATE KEY UPDATE balance = VALUES(balance);

-- 以下为临时作品/章节数据（B 表），仅本地联调使用
INSERT INTO novel (novel_id, author_id, title, cover_url, description, status, audit_status, total_words)
VALUES (1001, 3, '星海归途', NULL, '少年在星际废墟中捡到一枚旧式引擎，从此踏上归途。', 0, 1, 0)
ON DUPLICATE KEY UPDATE title = VALUES(title);

INSERT INTO chapter (chapter_id, novel_id, chapter_no, title, content, is_vip, vip_price, word_count,
                     status, audit_status, publish_time)
VALUES (2001, 1001, 1, '第一章 废墟信号', '引擎的低鸣在舱壁间回荡……', 0, 0.00, 3000, 1, 1, NOW()),
       (2002, 1001, 2, '第二章 折跃点', '他把坐标推入主控台……', 1, 12.00, 4200, 1, 1, NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title);
