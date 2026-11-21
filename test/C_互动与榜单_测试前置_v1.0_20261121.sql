-- =====================================================================
-- 网络文学创作与阅读平台 · C 互动与榜单  测试前置脚本
-- 文件：C_互动与榜单_测试前置_v1.0_20261121.sql
-- 用途：仅供本地/联调验证使用，不属于交付基线；用于构造 A、B 模块最小前置表
-- 警告：脚本会重建测试库 c_module_test，请勿在开发库或生产库执行
-- =====================================================================

DROP DATABASE IF EXISTS c_module_test;
CREATE DATABASE c_module_test DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE c_module_test;

-- A 模块最小前置：用户
CREATE TABLE app_user (
  user_id  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  nickname VARCHAR(50)     NOT NULL,
  PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='A 用户表（测试最小结构）';

-- B 模块最小前置：小说 / 章节
CREATE TABLE novel (
  novel_id     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  author_id    BIGINT UNSIGNED NOT NULL,
  title        VARCHAR(120)    NOT NULL,
  status       TINYINT         NOT NULL DEFAULT 0 COMMENT '0连载中 1已完结 2已下架',
  audit_status TINYINT         NOT NULL DEFAULT 0,
  PRIMARY KEY (novel_id),
  CONSTRAINT fk_novel_author FOREIGN KEY (author_id) REFERENCES app_user (user_id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='B 小说表（测试最小结构）';

CREATE TABLE chapter (
  chapter_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  novel_id   BIGINT UNSIGNED NOT NULL,
  chapter_no INT UNSIGNED    NOT NULL,
  title      VARCHAR(120)    NOT NULL,
  status     TINYINT         NOT NULL DEFAULT 1,
  PRIMARY KEY (chapter_id),
  CONSTRAINT fk_chapter_novel FOREIGN KEY (novel_id) REFERENCES novel (novel_id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='B 章节表（测试最小结构）';

-- A 模块最小前置：书架（收藏榜数据源）
CREATE TABLE user_bookshelf (
  user_id    BIGINT UNSIGNED NOT NULL,
  novel_id   BIGINT UNSIGNED NOT NULL,
  created_at DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (user_id, novel_id),
  CONSTRAINT fk_bookshelf_user  FOREIGN KEY (user_id)  REFERENCES app_user (user_id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_bookshelf_novel FOREIGN KEY (novel_id) REFERENCES novel (novel_id)    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='A 书架表（测试最小结构）';

-- A 模块最小前置：章节订阅（销售榜数据源）
CREATE TABLE chapter_subscription (
  user_id     BIGINT UNSIGNED NOT NULL,
  chapter_id  BIGINT UNSIGNED NOT NULL,
  novel_id    BIGINT UNSIGNED NOT NULL,
  paid_amount DECIMAL(12,2)   NOT NULL,
  status      ENUM('ACTIVE','REFUNDED') NOT NULL DEFAULT 'ACTIVE',
  PRIMARY KEY (user_id, chapter_id),
  CONSTRAINT fk_sub_user    FOREIGN KEY (user_id)    REFERENCES app_user (user_id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_sub_chapter FOREIGN KEY (chapter_id) REFERENCES chapter  (chapter_id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_sub_novel   FOREIGN KEY (novel_id)   REFERENCES novel    (novel_id)   ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='A 章节订阅表（测试最小结构）';

-- A 模块最小前置：打赏（打赏榜数据源）
CREATE TABLE reward_tip (
  tip_id   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  novel_id BIGINT UNSIGNED NOT NULL,
  amount   DECIMAL(12,2)   NOT NULL,
  status   ENUM('SUCCESS','REFUNDED') NOT NULL DEFAULT 'SUCCESS',
  PRIMARY KEY (tip_id),
  CONSTRAINT fk_tip_novel FOREIGN KEY (novel_id) REFERENCES novel (novel_id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='A 打赏表（测试最小结构）';
