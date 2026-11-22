-- 网络文学创作与阅读平台 D 模块（运营后台）建表脚本
-- 适用版本：MySQL 8.0
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
