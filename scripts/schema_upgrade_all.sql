USE tx_ticket;

SET NAMES utf8mb4;

-- =========================================================
-- 2026-04-27 基础结构补齐
-- =========================================================

ALTER TABLE `sys_user`
  ADD COLUMN `sex` tinyint DEFAULT 0 COMMENT '性别(0未知 1男 2女)' AFTER `phone`,
  ADD COLUMN `birth_date` date DEFAULT NULL COMMENT '出生日期' AFTER `sex`,
  ADD COLUMN `address` varchar(255) DEFAULT NULL COMMENT '联系地址' AFTER `birth_date`;

ALTER TABLE `sys_permission`
  ADD COLUMN `remark` varchar(500) DEFAULT NULL COMMENT '备注' AFTER `update_time`,
  ADD INDEX `idx_permission_menu_id` (`menu_id`);

ALTER TABLE `sys_dict_type`
  CHANGE COLUMN `update_timeMap` `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE `sys_dict_data`
  ADD INDEX `idx_dict_type_parent_sort` (`type_code`, `parent_id`, `sort`);

ALTER TABLE `sys_menu`
  ADD INDEX `idx_menu_parent_sort` (`parent_id`, `sort`);

ALTER TABLE `sys_file`
  MODIFY COLUMN `storage_type` varchar(50) NOT NULL COMMENT '存储类型(LOCAL/ALIYUN/MINIO)',
  ADD INDEX `idx_file_storage_create_time` (`storage_type`, `create_time`);

INSERT INTO `sys_permission` (`menu_id`, `name`, `perm_key`)
VALUES (9, '删除文件', 'sys:file:delete')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `menu_id` = VALUES(`menu_id`);

-- =========================================================
-- 2026-04-27 权限模型拆分
-- =========================================================

CREATE TABLE IF NOT EXISTS `sys_menu_permission` (
  `menu_id` bigint NOT NULL,
  `perm_id` bigint NOT NULL,
  PRIMARY KEY (`menu_id`, `perm_id`),
  KEY `idx_menu_permission_perm` (`perm_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_user_perm` (
  `user_id` bigint NOT NULL,
  `perm_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`, `perm_id`),
  KEY `idx_user_permission_perm` (`perm_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO `sys_menu_permission` (`menu_id`, `perm_id`)
SELECT `menu_id`, `id`
FROM `sys_permission`
WHERE `menu_id` IS NOT NULL;

ALTER TABLE `sys_permission`
  ADD COLUMN `status` tinyint DEFAULT 1 AFTER `perm_key`;

UPDATE `sys_permission`
SET `status` = 1
WHERE `status` IS NULL;

ALTER TABLE `sys_permission`
  MODIFY COLUMN `status` tinyint DEFAULT 1;

ALTER TABLE `sys_permission`
  DROP INDEX `idx_permission_menu_id`;

ALTER TABLE `sys_permission`
  ADD INDEX `idx_permission_status` (`status`);

ALTER TABLE `sys_permission`
  MODIFY COLUMN `menu_id` bigint NULL;

INSERT INTO `sys_permission` (`name`, `perm_key`, `status`)
VALUES
('查询角色', 'sys:role:list', 1),
('新增角色', 'sys:role:add', 1),
('修改角色', 'sys:role:edit', 1),
('删除角色', 'sys:role:delete', 1),
('查询菜单', 'sys:menu:list', 1),
('新增菜单', 'sys:menu:add', 1),
('修改菜单', 'sys:menu:edit', 1),
('删除菜单', 'sys:menu:delete', 1),
('查询部门', 'sys:dept:list', 1),
('新增部门', 'sys:dept:add', 1),
('删除部门', 'sys:dept:delete', 1),
('查询字典', 'sys:dict:list', 1),
('修改存储配置', 'sys:config:edit', 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `status` = VALUES(`status`);

INSERT IGNORE INTO `sys_menu_permission` (`menu_id`, `perm_id`)
SELECT 4, `id` FROM `sys_permission` WHERE `perm_key` IN ('sys:role:list', 'sys:role:add', 'sys:role:edit', 'sys:role:delete');

INSERT IGNORE INTO `sys_menu_permission` (`menu_id`, `perm_id`)
SELECT 5, `id` FROM `sys_permission` WHERE `perm_key` IN ('sys:menu:list', 'sys:menu:add', 'sys:menu:edit', 'sys:menu:delete');

INSERT IGNORE INTO `sys_menu_permission` (`menu_id`, `perm_id`)
SELECT 3, `id` FROM `sys_permission` WHERE `perm_key` IN ('sys:dept:list', 'sys:dept:add', 'sys:dept:delete');

INSERT IGNORE INTO `sys_menu_permission` (`menu_id`, `perm_id`)
SELECT 6, `id` FROM `sys_permission` WHERE `perm_key` IN ('sys:dict:list');

INSERT IGNORE INTO `sys_menu_permission` (`menu_id`, `perm_id`)
SELECT 10, `id` FROM `sys_permission` WHERE `perm_key` IN ('sys:config:list', 'sys:config:edit');

ALTER TABLE `sys_permission`
  DROP COLUMN `menu_id`;

-- =========================================================
-- 2026-04-27 头像文件引用
-- =========================================================

ALTER TABLE `sys_user`
  ADD COLUMN IF NOT EXISTS `avatar_file_id` bigint DEFAULT NULL COMMENT '头像文件ID' AFTER `nickname`;

SET @avatar_column_exists = (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_user'
    AND column_name = 'avatar'
);
SET @avatar_numeric_backfill_sql = IF(
  @avatar_column_exists = 1,
  'UPDATE `sys_user` SET `avatar_file_id` = CAST(`avatar` AS UNSIGNED) WHERE `avatar_file_id` IS NULL AND `avatar` IS NOT NULL AND `avatar` REGEXP ''^[0-9]+$''',
  'SELECT 1'
);
PREPARE avatar_numeric_backfill_stmt FROM @avatar_numeric_backfill_sql;
EXECUTE avatar_numeric_backfill_stmt;
DEALLOCATE PREPARE avatar_numeric_backfill_stmt;

SET @avatar_url_backfill_sql = IF(
  @avatar_column_exists = 1,
  'UPDATE `sys_user` u JOIN `sys_file` f ON f.`url` = u.`avatar` SET u.`avatar_file_id` = f.`id` WHERE u.`avatar_file_id` IS NULL AND u.`avatar` IS NOT NULL AND LENGTH(TRIM(u.`avatar`)) > 0',
  'SELECT 1'
);
PREPARE avatar_url_backfill_stmt FROM @avatar_url_backfill_sql;
EXECUTE avatar_url_backfill_stmt;
DEALLOCATE PREPARE avatar_url_backfill_stmt;

SET @avatar_file_index_exists = (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_user'
    AND index_name = 'idx_user_avatar_file_id'
);
SET @avatar_file_index_sql = IF(
  @avatar_file_index_exists = 0,
  'CREATE INDEX `idx_user_avatar_file_id` ON `sys_user` (`avatar_file_id`)',
  'SELECT 1'
);
PREPARE avatar_file_index_stmt FROM @avatar_file_index_sql;
EXECUTE avatar_file_index_stmt;
DEALLOCATE PREPARE avatar_file_index_stmt;

-- =========================================================
-- 2026-04-27 安全与数据权限
-- =========================================================

CREATE TABLE IF NOT EXISTS `sys_role_dept` (
  `role_id` bigint NOT NULL,
  `dept_id` bigint NOT NULL,
  PRIMARY KEY (`role_id`,`dept_id`),
  KEY `idx_role_dept_dept` (`dept_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @login_log_index_exists = (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_login_log'
    AND index_name = 'idx_login_log_username_status_time'
);
SET @login_log_index_sql = IF(
  @login_log_index_exists = 0,
  'CREATE INDEX `idx_login_log_username_status_time` ON `sys_login_log` (`username`,`status`,`login_time`)',
  'SELECT 1'
);
PREPARE login_log_index_stmt FROM @login_log_index_sql;
EXECUTE login_log_index_stmt;
DEALLOCATE PREPARE login_log_index_stmt;

INSERT INTO `sys_permission` (`name`, `perm_key`, `status`)
VALUES
  ('用户直授权限', 'sys:user:assignPerm', 1),
  ('重置用户密码', 'sys:user:resetPwd', 1),
  ('强制用户下线', 'sys:user:kickout', 1)
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `status` = VALUES(`status`);

INSERT IGNORE INTO `sys_menu_permission` (`menu_id`, `perm_id`)
SELECT 2, id FROM `sys_permission`
WHERE `perm_key` IN ('sys:user:assignPerm', 'sys:user:resetPwd', 'sys:user:kickout');

-- =========================================================
-- 2026-04-28 权限分析与代码生成相关索引
-- =========================================================

SET @idx_user_status_dept_exists = (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_user'
    AND index_name = 'idx_user_status_dept'
);
SET @idx_user_status_dept_sql = IF(
  @idx_user_status_dept_exists = 0,
  'CREATE INDEX `idx_user_status_dept` ON `sys_user` (`status`,`dept_id`)',
  'SELECT 1'
);
PREPARE idx_user_status_dept_stmt FROM @idx_user_status_dept_sql;
EXECUTE idx_user_status_dept_stmt;
DEALLOCATE PREPARE idx_user_status_dept_stmt;

SET @idx_user_role_role_exists = (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_user_role'
    AND index_name = 'idx_user_role_role'
);
SET @idx_user_role_role_sql = IF(
  @idx_user_role_role_exists = 0,
  'CREATE INDEX `idx_user_role_role` ON `sys_user_role` (`role_id`,`user_id`)',
  'SELECT 1'
);
PREPARE idx_user_role_role_stmt FROM @idx_user_role_role_sql;
EXECUTE idx_user_role_role_stmt;
DEALLOCATE PREPARE idx_user_role_role_stmt;

SET @idx_role_perm_perm_exists = (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_role_perm'
    AND index_name = 'idx_role_perm_perm'
);
SET @idx_role_perm_perm_sql = IF(
  @idx_role_perm_perm_exists = 0,
  'CREATE INDEX `idx_role_perm_perm` ON `sys_role_perm` (`perm_id`,`role_id`)',
  'SELECT 1'
);
PREPARE idx_role_perm_perm_stmt FROM @idx_role_perm_perm_sql;
EXECUTE idx_role_perm_perm_stmt;
DEALLOCATE PREPARE idx_role_perm_perm_stmt;

SET @idx_oper_log_time_type_status_exists = (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_oper_log'
    AND index_name = 'idx_oper_log_time_type_status'
);
SET @idx_oper_log_time_type_status_sql = IF(
  @idx_oper_log_time_type_status_exists = 0,
  'CREATE INDEX `idx_oper_log_time_type_status` ON `sys_oper_log` (`oper_time`,`business_type`,`status`)',
  'SELECT 1'
);
PREPARE idx_oper_log_time_type_status_stmt FROM @idx_oper_log_time_type_status_sql;
EXECUTE idx_oper_log_time_type_status_stmt;
DEALLOCATE PREPARE idx_oper_log_time_type_status_stmt;

SET @idx_login_log_time_status_exists = (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_login_log'
    AND index_name = 'idx_login_log_time_status'
);
SET @idx_login_log_time_status_sql = IF(
  @idx_login_log_time_status_exists = 0,
  'CREATE INDEX `idx_login_log_time_status` ON `sys_login_log` (`login_time`,`status`)',
  'SELECT 1'
);
PREPARE idx_login_log_time_status_stmt FROM @idx_login_log_time_status_sql;
EXECUTE idx_login_log_time_status_stmt;
DEALLOCATE PREPARE idx_login_log_time_status_stmt;

SET @idx_file_create_time_type_suffix_exists = (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_file'
    AND index_name = 'idx_file_create_time_type_suffix'
);
SET @idx_file_create_time_type_suffix_sql = IF(
  @idx_file_create_time_type_suffix_exists = 0,
  'CREATE INDEX `idx_file_create_time_type_suffix` ON `sys_file` (`create_time`,`storage_type`,`file_suffix`)',
  'SELECT 1'
);
PREPARE idx_file_create_time_type_suffix_stmt FROM @idx_file_create_time_type_suffix_sql;
EXECUTE idx_file_create_time_type_suffix_stmt;
DEALLOCATE PREPARE idx_file_create_time_type_suffix_stmt;

INSERT INTO `sys_menu` (`id`, `parent_id`, `title`, `path`, `component`, `icon`, `sort`)
VALUES (14, 1, '权限分析', '/rbac/analytics', '/rbac/analytics/index', 'PieChartOutlined', 8)
ON DUPLICATE KEY UPDATE
  `parent_id` = VALUES(`parent_id`),
  `title` = VALUES(`title`),
  `path` = VALUES(`path`),
  `component` = VALUES(`component`),
  `icon` = VALUES(`icon`),
  `sort` = VALUES(`sort`);

INSERT INTO `sys_permission` (`name`, `perm_key`, `status`)
VALUES
  ('导出登录日志', 'sys:loginlog:export', 1),
  ('导出操作日志', 'sys:operlog:export', 1),
  ('导出文件报表', 'sys:file:export', 1),
  ('查看权限分析', 'sys:analytics:list', 1),
  ('导出权限分析', 'sys:analytics:export', 1)
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `status` = VALUES(`status`);

INSERT IGNORE INTO `sys_menu_permission` (`menu_id`, `perm_id`)
SELECT 14, id FROM `sys_permission`
WHERE `perm_key` IN ('sys:analytics:list', 'sys:analytics:export');

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 14);

-- =========================================================
-- 2026-06-11 公告消息
-- =========================================================

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `sys_notice` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(120) NOT NULL COMMENT '公告标题',
  `content` text NOT NULL COMMENT '公告正文',
  `notice_type` varchar(30) DEFAULT '公告' COMMENT '公告类型',
  `priority` varchar(30) DEFAULT '普通' COMMENT '优先级',
  `status` tinyint DEFAULT 0 COMMENT '状态: 0草稿 1已发布 2已撤回',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_notice_status_publish_time` (`status`, `publish_time`),
  KEY `idx_notice_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告消息表';

CREATE TABLE IF NOT EXISTS `sys_notice_target` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `notice_id` bigint NOT NULL COMMENT '公告ID',
  `target_type` varchar(20) NOT NULL COMMENT '目标类型: ALL DEPT ROLE USER',
  `target_id` bigint DEFAULT NULL COMMENT '目标ID',
  `effect` varchar(20) NOT NULL COMMENT 'INCLUDE 或 EXCLUDE',
  PRIMARY KEY (`id`),
  KEY `idx_notice_target_notice` (`notice_id`),
  KEY `idx_notice_target_lookup` (`target_type`, `target_id`, `effect`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告目标规则表';

CREATE TABLE IF NOT EXISTS `sys_notice_recipient` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `notice_id` bigint NOT NULL COMMENT '公告ID',
  `user_id` bigint NOT NULL COMMENT '接收用户ID',
  `read_flag` tinyint DEFAULT 0 COMMENT '是否已读: 0未读 1已读',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `receive_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '接收时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_notice_recipient_unique` (`notice_id`, `user_id`),
  KEY `idx_notice_recipient_user_read` (`user_id`, `read_flag`, `receive_time`),
  KEY `idx_notice_recipient_notice_read` (`notice_id`, `read_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告接收人快照表';

INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `title`, `path`, `component`, `icon`, `sort`, `remark`)
VALUES
(120, 0, '公告消息', '/system/notice', 'system/notice/index', 'NotificationOutlined', 75, '公告消息管理'),
(121, 0, '我的公告', '/notice/my', 'notice/my', 'BellOutlined', 76, '当前用户公告收件箱');

INSERT IGNORE INTO `sys_permission` (`id`, `name`, `perm_key`, `status`, `remark`)
VALUES
(1201, '公告列表', 'sys:notice:list', 1, '公告管理列表和统计'),
(1202, '新增公告', 'sys:notice:add', 1, '新增公告草稿'),
(1203, '编辑公告', 'sys:notice:edit', 1, '编辑公告草稿'),
(1204, '发布公告', 'sys:notice:publish', 1, '发布公告并生成接收人快照'),
(1205, '撤回公告', 'sys:notice:withdraw', 1, '撤回已发布公告'),
(1206, '删除公告', 'sys:notice:delete', 1, '删除公告'),
(1207, '查看我的公告', 'sys:notice:read', 1, '查看当前用户接收的公告');

INSERT IGNORE INTO `sys_menu_permission` (`menu_id`, `perm_id`)
VALUES
(120, 1201),
(120, 1202),
(120, 1203),
(120, 1204),
(120, 1205),
(120, 1206),
(121, 1207);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES
(1, 120),
(1, 121);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT id, 121
FROM sys_role
WHERE del_flag = 0;

INSERT IGNORE INTO `sys_role_perm` (`role_id`, `perm_id`)
VALUES
(1, 1201),
(1, 1202),
(1, 1203),
(1, 1204),
(1, 1205),
(1, 1206),
(1, 1207);

INSERT IGNORE INTO `sys_role_perm` (`role_id`, `perm_id`)
SELECT id, 1207
FROM sys_role
WHERE del_flag = 0;

SET FOREIGN_KEY_CHECKS = 1;
