USE tx_ticket;

SET NAMES utf8mb4;

-- =========================================================
-- 2026-04-27 基础结构补齐
-- =========================================================

SET @ddl_sql = IF(
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'sex'),
  'SELECT 1',
  'ALTER TABLE `sys_user` ADD COLUMN `sex` tinyint DEFAULT 0 COMMENT ''性别(0未知 1男 2女)'' AFTER `phone`'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql = IF(
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'birth_date'),
  'SELECT 1',
  'ALTER TABLE `sys_user` ADD COLUMN `birth_date` date DEFAULT NULL COMMENT ''出生日期'' AFTER `sex`'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql = IF(
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'address'),
  'SELECT 1',
  'ALTER TABLE `sys_user` ADD COLUMN `address` varchar(255) DEFAULT NULL COMMENT ''联系地址'' AFTER `birth_date`'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql = IF(
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_permission' AND column_name = 'remark'),
  'SELECT 1',
  'ALTER TABLE `sys_permission` ADD COLUMN `remark` varchar(500) DEFAULT NULL COMMENT ''备注'' AFTER `update_time`'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql = IF(
  EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'sys_permission' AND index_name = 'idx_permission_menu_id')
  OR NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_permission' AND column_name = 'menu_id'),
  'SELECT 1',
  'ALTER TABLE `sys_permission` ADD INDEX `idx_permission_menu_id` (`menu_id`)'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql = IF(
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_dict_type' AND column_name = 'update_timeMap')
  AND NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_dict_type' AND column_name = 'update_time'),
  'ALTER TABLE `sys_dict_type` CHANGE COLUMN `update_timeMap` `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP',
  'SELECT 1'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql = IF(
  EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'sys_dict_data' AND index_name = 'idx_dict_type_parent_sort'),
  'SELECT 1',
  'ALTER TABLE `sys_dict_data` ADD INDEX `idx_dict_type_parent_sort` (`type_code`, `parent_id`, `sort`)'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql = IF(
  EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'sys_menu' AND index_name = 'idx_menu_parent_sort'),
  'SELECT 1',
  'ALTER TABLE `sys_menu` ADD INDEX `idx_menu_parent_sort` (`parent_id`, `sort`)'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql = IF(
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_file' AND column_name = 'storage_type'),
  'ALTER TABLE `sys_file` MODIFY COLUMN `storage_type` varchar(50) NOT NULL COMMENT ''存储类型(LOCAL/ALIYUN/MINIO)''',
  'SELECT 1'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql = IF(
  EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'sys_file' AND index_name = 'idx_file_storage_create_time'),
  'SELECT 1',
  'ALTER TABLE `sys_file` ADD INDEX `idx_file_storage_create_time` (`storage_type`, `create_time`)'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO `sys_permission` (`name`, `perm_key`, `status`)
VALUES ('删除文件', 'sys:file:delete', 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `status` = VALUES(`status`);

-- =========================================================
-- 2026-04-27 权限模型拆分
-- =========================================================

CREATE TABLE IF NOT EXISTS `sys_menu_permission` (
  `menu_id` bigint NOT NULL,
  `perm_id` bigint NOT NULL,
  PRIMARY KEY (`menu_id`, `perm_id`),
  KEY `idx_menu_permission_perm` (`perm_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO `sys_menu_permission` (`menu_id`, `perm_id`)
SELECT 9, `id` FROM `sys_permission` WHERE `perm_key` = 'sys:file:delete';

CREATE TABLE IF NOT EXISTS `sys_user_perm` (
  `user_id` bigint NOT NULL,
  `perm_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`, `perm_id`),
  KEY `idx_user_permission_perm` (`perm_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @ddl_sql = IF(
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_permission' AND column_name = 'menu_id'),
  'INSERT IGNORE INTO `sys_menu_permission` (`menu_id`, `perm_id`) SELECT `menu_id`, `id` FROM `sys_permission` WHERE `menu_id` IS NOT NULL',
  'SELECT 1'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql = IF(
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_permission' AND column_name = 'status'),
  'SELECT 1',
  'ALTER TABLE `sys_permission` ADD COLUMN `status` tinyint DEFAULT 1 AFTER `perm_key`'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `sys_permission`
SET `status` = 1
WHERE `status` IS NULL;

ALTER TABLE `sys_permission`
  MODIFY COLUMN `status` tinyint DEFAULT 1;

SET @ddl_sql = IF(
  EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'sys_permission' AND index_name = 'idx_permission_menu_id'),
  'ALTER TABLE `sys_permission` DROP INDEX `idx_permission_menu_id`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql = IF(
  EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'sys_permission' AND index_name = 'idx_permission_status'),
  'SELECT 1',
  'ALTER TABLE `sys_permission` ADD INDEX `idx_permission_status` (`status`)'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl_sql = IF(
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_permission' AND column_name = 'menu_id'),
  'ALTER TABLE `sys_permission` MODIFY COLUMN `menu_id` bigint NULL',
  'SELECT 1'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

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

SET @ddl_sql = IF(
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_permission' AND column_name = 'menu_id'),
  'ALTER TABLE `sys_permission` DROP COLUMN `menu_id`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =========================================================
-- 2026-04-27 头像文件引用
-- =========================================================

SET @ddl_sql = IF(
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'avatar_file_id'),
  'SELECT 1',
  'ALTER TABLE `sys_user` ADD COLUMN `avatar_file_id` bigint DEFAULT NULL COMMENT ''头像文件ID'' AFTER `nickname`'
);
PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

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

-- =========================================================
-- 2026-07-02 工单中心首页入口
-- =========================================================

INSERT INTO `sys_role` (`id`, `role_name`, `role_key`, `data_scope`, `status`)
VALUES (5, '客户', 'customer', 5, 1)
ON DUPLICATE KEY UPDATE
  `role_name` = VALUES(`role_name`),
  `role_key` = VALUES(`role_key`),
  `data_scope` = VALUES(`data_scope`),
  `status` = VALUES(`status`);

INSERT INTO `sys_role` (`role_name`, `role_key`, `data_scope`, `status`, `remark`)
VALUES ('工单主管', 'supervisor', 4, 1, '工单审批、分派、SLA 升级和知识审核角色')
ON DUPLICATE KEY UPDATE
  `role_name` = VALUES(`role_name`),
  `data_scope` = VALUES(`data_scope`),
  `status` = VALUES(`status`),
  `remark` = VALUES(`remark`);

INSERT INTO `sys_menu` (`id`, `parent_id`, `title`, `path`, `component`, `icon`, `sort`, `remark`)
VALUES
(100, 0, '工单中心', '/ticket', NULL, 'CustomerServiceOutlined', 4, '工单系统入口'),
(101, 100, '工单工作台', '/ticket/workbench', '/ticket/workbench/index', 'ToolOutlined', 3, '运维处理工单工作台'),
(102, 100, '我的工单', '/ticket/my', '/ticket/my/index', 'ProfileOutlined', 4, '客户查看和跟踪自己提交的工单'),
(103, 100, '知识库', '/ticket/knowledge', '/ticket/knowledge/index', 'BookOutlined', 6, '解决方案知识沉淀')
ON DUPLICATE KEY UPDATE
  `parent_id` = VALUES(`parent_id`),
  `title` = VALUES(`title`),
  `path` = VALUES(`path`),
  `component` = VALUES(`component`),
  `icon` = VALUES(`icon`),
  `sort` = VALUES(`sort`),
  `remark` = VALUES(`remark`);

INSERT INTO `sys_menu` (`id`, `parent_id`, `title`, `path`, `component`, `icon`, `sort`, `remark`)
VALUES (
  104,
  100,
  CONVERT(UNHEX('E5B7A5E58D95E9A696E9A1B5') USING utf8mb4),
  '/ticket/index',
  '/ticket/index',
  'DashboardOutlined',
  0,
  CONVERT(UNHEX('E5B7A5E58D95E4B8ADE5BF83E9A696E9A1B5') USING utf8mb4)
)
ON DUPLICATE KEY UPDATE
  `parent_id` = VALUES(`parent_id`),
  `title` = VALUES(`title`),
  `path` = VALUES(`path`),
  `component` = VALUES(`component`),
  `icon` = VALUES(`icon`),
  `sort` = VALUES(`sort`),
  `remark` = VALUES(`remark`);

INSERT INTO `sys_menu` (`id`, `parent_id`, `title`, `path`, `component`, `icon`, `sort`, `remark`)
VALUES
(
  105,
  100,
  CONVERT(UNHEX('E68F90E4BAA4E5B7A5E58D95') USING utf8mb4),
  '/ticket/create',
  '/ticket/create',
  'PlusOutlined',
  1,
  CONVERT(UNHEX('E5AEA2E688B7E78BACE7AB8BE68F90E4BAA4E5B7A5E58D95E585A5E58FA3') USING utf8mb4)
),
(
  107,
  100,
  CONVERT(UNHEX('E5BE85E68EA5E5B7A5E58D95') USING utf8mb4),
  '/ticket/pending',
  '/ticket/pending/index',
  'InboxOutlined',
  2,
  CONVERT(UNHEX('E8BF90E7BBB4E5BE85E68EA5E5B7A5E58D95E58897E8A1A8') USING utf8mb4)
),
(
  106,
  100,
  CONVERT(UNHEX('E7B3BBE7BB9FE9858DE7BDAE') USING utf8mb4),
  '/ticket/systems',
  '/ticket/systems',
  'ControlOutlined',
  5,
  CONVERT(UNHEX('E5B7A5E58D95E68980E5B19EE7B3BBE7BB9FE9858DE7BDAE') USING utf8mb4)
)
ON DUPLICATE KEY UPDATE
  `parent_id` = VALUES(`parent_id`),
  `title` = VALUES(`title`),
  `path` = VALUES(`path`),
  `component` = VALUES(`component`),
  `icon` = VALUES(`icon`),
  `sort` = VALUES(`sort`),
  `remark` = VALUES(`remark`);

INSERT INTO `sys_permission` (`id`, `name`, `perm_key`, `status`, `remark`)
VALUES
(100, '查看工单工作台', 'ticket:list', 1, '查看工单首页、待接工单和工作台'),
(101, '新增工单', 'ticket:add', 1, '运维或管理员代提交工单'),
(102, '接单', 'ticket:receive', 1, '运维接收待受理工单'),
(103, '回复工单', 'ticket:reply', 1, '运维处理过程回复与内部留痕'),
(104, '分派工单', 'ticket:assign', 1, '主管或管理员分派工单'),
(105, '转派工单', 'ticket:transfer', 1, '当前处理人转派工单'),
(106, '解决工单', 'ticket:resolve', 1, '填写最终解决方案'),
(107, '关闭工单', 'ticket:close', 1, '关闭已解决工单'),
(108, '驳回工单', 'ticket:reject', 1, '审批或处理阶段驳回工单'),
(109, '查看我的工单', 'ticket:my:list', 1, '客户查看自己提交的工单'),
(110, '客户提交工单', 'ticket:my:add', 1, '客户提交工单'),
(111, '客户回复工单', 'ticket:my:reply', 1, '客户补充材料或反馈'),
(112, '客户关闭工单', 'ticket:my:close', 1, '客户确认关闭、撤销或评价工单'),
(120, '查看知识库', 'knowledge:list', 1, '查看知识库条目'),
(121, '维护知识库', 'knowledge:edit', 1, '新增、编辑、提交审核知识库'),
(122, '发布知识库', 'knowledge:publish', 1, '审核、发布和下架知识库')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `perm_key` = VALUES(`perm_key`),
  `status` = VALUES(`status`),
  `remark` = VALUES(`remark`);

INSERT IGNORE INTO `sys_menu_permission` (`menu_id`, `perm_id`)
VALUES
(104, 100), (104, 109),
(105, 101), (105, 109), (105, 110),
(106, 100),
(107, 100), (107, 102), (107, 104), (107, 108),
(101, 100), (101, 101), (101, 102), (101, 103), (101, 104), (101, 105), (101, 106), (101, 107), (101, 108),
(102, 101), (102, 109), (102, 110), (102, 111), (102, 112),
(103, 120), (103, 121), (103, 122);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES
(1, 100),
(2, 100),
(3, 100),
(5, 100),
(1, 101),
(2, 101),
(3, 101),
(1, 102),
(2, 102),
(3, 102),
(5, 102),
(1, 103),
(2, 103),
(3, 103),
(1, 104),
(2, 104),
(3, 104),
(5, 104),
(1, 105),
(2, 105),
(3, 105),
(5, 105),
(1, 107),
(2, 107),
(3, 107),
(1, 106),
(2, 106),
(3, 106);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, m.menu_id
FROM `sys_role` r
JOIN (
  SELECT 100 AS menu_id UNION ALL SELECT 101 UNION ALL SELECT 102 UNION ALL SELECT 103
  UNION ALL SELECT 104 UNION ALL SELECT 105 UNION ALL SELECT 106 UNION ALL SELECT 107
) m
WHERE r.role_key = 'supervisor'
  AND r.del_flag = 0;

INSERT IGNORE INTO `sys_role_perm` (`role_id`, `perm_id`)
SELECT 1, id FROM `sys_permission`
WHERE del_flag = 0
  AND (perm_key LIKE 'ticket:%' OR perm_key LIKE 'knowledge:%');

INSERT IGNORE INTO `sys_role_perm` (`role_id`, `perm_id`)
SELECT 2, id FROM `sys_permission`
WHERE del_flag = 0
  AND (perm_key LIKE 'ticket:%' OR perm_key LIKE 'knowledge:%');

INSERT IGNORE INTO `sys_role_perm` (`role_id`, `perm_id`)
SELECT 3, id FROM `sys_permission`
WHERE del_flag = 0
  AND perm_key IN (
    'ticket:list','ticket:add','ticket:receive','ticket:reply','ticket:transfer',
    'ticket:resolve','ticket:close','knowledge:list','knowledge:edit',
    'sys:file:upload'
  );

INSERT IGNORE INTO `sys_role_perm` (`role_id`, `perm_id`)
SELECT r.id, p.id
FROM `sys_role` r
JOIN `sys_permission` p
WHERE r.role_key = 'supervisor'
  AND r.del_flag = 0
  AND p.del_flag = 0
  AND (p.perm_key LIKE 'ticket:%' OR p.perm_key LIKE 'knowledge:%' OR p.perm_key = 'sys:file:upload');

INSERT IGNORE INTO `sys_role_perm` (`role_id`, `perm_id`)
SELECT 5, id FROM `sys_permission`
WHERE del_flag = 0
  AND perm_key IN (
    'sys:dashboard:list','ticket:my:list','ticket:my:add','ticket:my:reply','ticket:my:close','sys:file:upload'
  );

INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`)
VALUES
(
  CONVERT(UNHEX('E99984E4BBB6E69C80E5A4A7E4B88AE4BCA0E5A4A7E5B08F4D42') USING utf8mb4),
  'file.security.max-size-mb',
  '200',
  'Y'
),
(
  CONVERT(UNHEX('E99984E4BBB6E58581E8AEB8E5908EE7BC80') USING utf8mb4),
  'file.security.allowed-suffixes',
  '.jpg,.jpeg,.png,.gif,.webp,.svg,.bmp,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.md,.log,.json,.xml,.csv,.zip,.rar,.mp4,.webm,.mov,.avi',
  'Y'
)
ON DUPLICATE KEY UPDATE
  `config_value` = VALUES(`config_value`),
  `config_type` = VALUES(`config_type`);

-- 工单业务表兜底创建：只在表不存在时创建，不触碰已有业务数据。
CREATE TABLE IF NOT EXISTS `tx_ticket` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ticket_no` varchar(40) NOT NULL COMMENT '工单编号',
  `title` varchar(200) NOT NULL COMMENT '工单标题',
  `description` text NOT NULL COMMENT '问题描述',
  `category` varchar(50) DEFAULT 'general' COMMENT '工单分类',
  `system_code` varchar(80) DEFAULT NULL COMMENT '所属系统编码',
  `priority` varchar(30) DEFAULT 'normal' COMMENT '优先级',
  `status` varchar(40) DEFAULT 'pending_approval' COMMENT '当前状态',
  `creator_id` bigint NOT NULL COMMENT '创建人ID',
  `creator_name` varchar(100) DEFAULT NULL COMMENT '创建人名称',
  `creator_phone` varchar(30) DEFAULT NULL COMMENT '联系电话',
  `creator_email` varchar(120) DEFAULT NULL COMMENT '联系邮箱',
  `handler_id` bigint DEFAULT NULL COMMENT '当前处理人ID',
  `handler_name` varchar(100) DEFAULT NULL COMMENT '当前处理人名称',
  `solution` text DEFAULT NULL COMMENT '最终解决方案',
  `merge_parent_id` bigint DEFAULT NULL COMMENT '合并后的主工单ID',
  `merge_reason` varchar(500) DEFAULT NULL COMMENT '合并原因',
  `reopen_count` int DEFAULT 0 COMMENT '重开次数',
  `evaluation_score` int DEFAULT NULL COMMENT '客户评价分值 1-5',
  `evaluation_content` varchar(500) DEFAULT NULL COMMENT '客户评价内容',
  `resolved_time` datetime DEFAULT NULL COMMENT '解决时间',
  `closed_time` datetime DEFAULT NULL COMMENT '关闭时间',
  `evaluation_time` datetime DEFAULT NULL COMMENT '评价时间',
  `sla_warn_time` datetime DEFAULT NULL COMMENT '最近 SLA 提醒时间',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_tx_ticket_no` (`ticket_no`),
  KEY `idx_tx_ticket_status_priority` (`status`,`priority`),
  KEY `idx_tx_ticket_category_status` (`category`,`status`,`priority`),
  KEY `idx_tx_ticket_system_status` (`system_code`,`status`),
  KEY `idx_tx_ticket_creator_time` (`creator_id`,`create_time`),
  KEY `idx_tx_ticket_handler_status` (`handler_id`,`status`),
  KEY `idx_tx_ticket_merge_parent` (`merge_parent_id`),
  KEY `idx_tx_ticket_creator_name` (`creator_name`),
  KEY `idx_tx_ticket_handler_name` (`handler_name`),
  KEY `idx_tx_ticket_title` (`title`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='头绪工单主表';

CREATE TABLE IF NOT EXISTS `tx_ticket_flow` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ticket_id` bigint NOT NULL COMMENT '工单ID',
  `action` varchar(40) NOT NULL COMMENT '流程动作',
  `from_status` varchar(40) DEFAULT NULL COMMENT '原状态',
  `to_status` varchar(40) DEFAULT NULL COMMENT '目标状态',
  `operator_id` bigint NOT NULL COMMENT '操作人ID',
  `operator_name` varchar(100) DEFAULT NULL COMMENT '操作人名称',
  `from_handler_id` bigint DEFAULT NULL COMMENT '原处理人ID',
  `to_handler_id` bigint DEFAULT NULL COMMENT '目标处理人ID',
  `content` text DEFAULT NULL COMMENT '处理内容',
  `visible_scope` varchar(30) DEFAULT 'public' COMMENT '可见范围 public/internal',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tx_ticket_flow_ticket_time` (`ticket_id`,`create_time`),
  KEY `idx_tx_ticket_flow_action` (`action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='头绪工单流程记录';

CREATE TABLE IF NOT EXISTS `tx_ticket_attachment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `business_type` varchar(40) NOT NULL COMMENT '业务类型 ticket/flow/knowledge',
  `ticket_id` bigint DEFAULT NULL COMMENT '工单ID',
  `flow_id` bigint DEFAULT NULL COMMENT '流程记录ID',
  `article_id` bigint DEFAULT NULL COMMENT '知识库文章ID',
  `file_id` bigint NOT NULL COMMENT 'sys_file文件ID',
  `visible_scope` varchar(30) DEFAULT 'public' COMMENT '可见范围',
  `upload_user_id` bigint DEFAULT NULL COMMENT '上传人ID',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tx_ticket_attach_ticket` (`ticket_id`,`business_type`),
  KEY `idx_tx_ticket_attach_flow` (`flow_id`),
  KEY `idx_tx_ticket_attach_article` (`article_id`),
  KEY `idx_tx_ticket_attach_file` (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='头绪工单附件关联';

CREATE TABLE IF NOT EXISTS `tx_knowledge_article` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL COMMENT '知识标题',
  `category` varchar(50) DEFAULT 'general' COMMENT '知识分类',
  `tags` varchar(500) DEFAULT NULL COMMENT '标签关键词',
  `phenomenon` text DEFAULT NULL COMMENT '问题现象',
  `cause_analysis` text DEFAULT NULL COMMENT '原因分析',
  `solution_steps` text NOT NULL COMMENT '解决步骤',
  `applicable_scope` text DEFAULT NULL COMMENT '适用范围',
  `status` varchar(30) DEFAULT 'draft' COMMENT '状态 draft/reviewing/rejected/published/withdrawn',
  `source_ticket_id` bigint DEFAULT NULL COMMENT '来源工单ID',
  `useful_count` int DEFAULT 0 COMMENT '有帮助次数',
  `useless_count` int DEFAULT 0 COMMENT '无帮助次数',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tx_knowledge_status_category` (`status`,`category`),
  KEY `idx_tx_knowledge_source_ticket` (`source_ticket_id`),
  KEY `idx_tx_knowledge_title` (`title`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='头绪知识库文章';

CREATE TABLE IF NOT EXISTS `tx_knowledge_ticket_link` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `article_id` bigint NOT NULL COMMENT '知识库文章ID',
  `ticket_id` bigint NOT NULL COMMENT '工单ID',
  `link_type` varchar(30) DEFAULT 'manual' COMMENT '关联类型 source/manual/recommend',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tx_knowledge_link_article` (`article_id`),
  KEY `idx_tx_knowledge_link_ticket` (`ticket_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库工单关联';

SET @idx_sql = IF(
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tx_ticket' AND column_name = 'system_code'),
  'SELECT 1',
  'ALTER TABLE tx_ticket ADD COLUMN system_code VARCHAR(80) NULL COMMENT ''所属系统编码'' AFTER category'
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_sql = IF(
  EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'tx_ticket' AND index_name = 'idx_tx_ticket_category_status'),
  'SELECT 1',
  'ALTER TABLE tx_ticket ADD INDEX idx_tx_ticket_category_status (category, status, priority)'
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_sql = IF(
  EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'tx_ticket' AND index_name = 'idx_tx_ticket_system_status'),
  'SELECT 1',
  'ALTER TABLE tx_ticket ADD INDEX idx_tx_ticket_system_status (system_code, status)'
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_sql = IF(
  EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'tx_ticket' AND index_name = 'idx_tx_ticket_creator_name'),
  'SELECT 1',
  'ALTER TABLE tx_ticket ADD INDEX idx_tx_ticket_creator_name (creator_name)'
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_sql = IF(
  EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'tx_ticket' AND index_name = 'idx_tx_ticket_handler_name'),
  'SELECT 1',
  'ALTER TABLE tx_ticket ADD INDEX idx_tx_ticket_handler_name (handler_name)'
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_sql = IF(
  EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'tx_ticket' AND index_name = 'idx_tx_ticket_title'),
  'SELECT 1',
  'ALTER TABLE tx_ticket ADD INDEX idx_tx_ticket_title (title)'
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_sql = IF(
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tx_ticket' AND column_name = 'merge_parent_id'),
  'SELECT 1',
  'ALTER TABLE tx_ticket ADD COLUMN merge_parent_id BIGINT NULL COMMENT ''合并后的主工单ID'' AFTER solution'
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_sql = IF(
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tx_ticket' AND column_name = 'merge_reason'),
  'SELECT 1',
  'ALTER TABLE tx_ticket ADD COLUMN merge_reason VARCHAR(500) NULL COMMENT ''合并原因'' AFTER merge_parent_id'
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_sql = IF(
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tx_ticket' AND column_name = 'reopen_count'),
  'SELECT 1',
  'ALTER TABLE tx_ticket ADD COLUMN reopen_count INT DEFAULT 0 COMMENT ''重开次数'' AFTER merge_reason'
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_sql = IF(
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tx_ticket' AND column_name = 'evaluation_score'),
  'SELECT 1',
  'ALTER TABLE tx_ticket ADD COLUMN evaluation_score INT NULL COMMENT ''客户评价分值 1-5'' AFTER reopen_count'
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_sql = IF(
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tx_ticket' AND column_name = 'evaluation_content'),
  'SELECT 1',
  'ALTER TABLE tx_ticket ADD COLUMN evaluation_content VARCHAR(500) NULL COMMENT ''客户评价内容'' AFTER evaluation_score'
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_sql = IF(
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tx_ticket' AND column_name = 'evaluation_time'),
  'SELECT 1',
  'ALTER TABLE tx_ticket ADD COLUMN evaluation_time DATETIME NULL COMMENT ''评价时间'' AFTER closed_time'
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_sql = IF(
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tx_ticket' AND column_name = 'sla_warn_time'),
  'SELECT 1',
  'ALTER TABLE tx_ticket ADD COLUMN sla_warn_time DATETIME NULL COMMENT ''最近 SLA 提醒时间'' AFTER evaluation_time'
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_sql = IF(
  EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'tx_ticket' AND index_name = 'idx_tx_ticket_merge_parent'),
  'SELECT 1',
  'ALTER TABLE tx_ticket ADD INDEX idx_tx_ticket_merge_parent (merge_parent_id)'
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_sql = IF(
  EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'tx_knowledge_article' AND index_name = 'idx_tx_knowledge_title'),
  'SELECT 1',
  'ALTER TABLE tx_knowledge_article ADD INDEX idx_tx_knowledge_title (title)'
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_sql = IF(
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tx_knowledge_article' AND column_name = 'status'),
  'ALTER TABLE tx_knowledge_article MODIFY COLUMN status VARCHAR(30) DEFAULT ''draft'' COMMENT ''状态 draft/reviewing/rejected/published/withdrawn''',
  'SELECT 1'
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO sys_dict_type (id, name, code, status) VALUES
(105, '工单所属系统', 'tx_ticket_system', 1)
ON DUPLICATE KEY UPDATE name=VALUES(name), code=VALUES(code), status=VALUES(status);

INSERT INTO sys_dict_data (id, type_code, parent_id, label, value, sort, status, remark) VALUES
(1007, 'tx_ticket_status', 0, '草稿', 'draft', 0, 1, NULL),
(1008, 'tx_ticket_status', 0, '待审批', 'pending_approval', 1, 1, NULL),
(1000, 'tx_ticket_status', 0, '待受理', 'pending', 2, 1, NULL),
(1001, 'tx_ticket_status', 0, '处理中', 'processing', 3, 1, NULL),
(1002, 'tx_ticket_status', 0, '待客户补充', 'waiting_customer', 4, 1, NULL),
(1003, 'tx_ticket_status', 0, '已转派', 'transferred', 5, 1, NULL),
(1004, 'tx_ticket_status', 0, '已解决', 'resolved', 6, 1, NULL),
(1005, 'tx_ticket_status', 0, '已关闭', 'closed', 7, 1, NULL),
(1006, 'tx_ticket_status', 0, '已驳回', 'rejected', 8, 1, NULL),
(1009, 'tx_ticket_status', 0, '已撤销', 'cancelled', 9, 1, NULL),
(1010, 'tx_ticket_priority', 0, '低', 'low', 1, 1, NULL),
(1011, 'tx_ticket_priority', 0, '普通', 'normal', 2, 1, NULL),
(1012, 'tx_ticket_priority', 0, '高', 'high', 3, 1, NULL),
(1013, 'tx_ticket_priority', 0, '紧急', 'urgent', 4, 1, NULL),
(1020, 'tx_ticket_category', 0, '通用问题', 'general', 1, 1, NULL),
(1021, 'tx_ticket_category', 0, '网络问题', 'network', 2, 1, NULL),
(1022, 'tx_ticket_category', 0, '系统问题', 'system', 3, 1, NULL),
(1023, 'tx_ticket_category', 0, '账号权限', 'account', 4, 1, NULL),
(1024, 'tx_ticket_category', 0, '服务器问题', 'server', 5, 1, NULL),
(1025, 'tx_ticket_category', 0, '数据问题', 'data', 6, 1, NULL),
(1026, 'tx_ticket_category', 0, '其他问题', 'other', 7, 1, NULL),
(1030, 'tx_ticket_action', 0, '创建工单', 'created', 1, 1, NULL),
(1060, 'tx_ticket_action', 0, '审批通过', 'approved', 2, 1, NULL),
(1061, 'tx_ticket_action', 0, '退回补充', 'returned', 3, 1, NULL),
(1031, 'tx_ticket_action', 0, '接单', 'received', 4, 1, NULL),
(1032, 'tx_ticket_action', 0, '回复', 'replied', 5, 1, NULL),
(1033, 'tx_ticket_action', 0, '分派', 'assigned', 6, 1, NULL),
(1034, 'tx_ticket_action', 0, '转派', 'transferred', 7, 1, NULL),
(1035, 'tx_ticket_action', 0, '解决', 'resolved', 8, 1, NULL),
(1036, 'tx_ticket_action', 0, '关闭', 'closed', 9, 1, NULL),
(1037, 'tx_ticket_action', 0, '驳回', 'rejected', 10, 1, NULL),
(1038, 'tx_ticket_action', 0, '合并', 'merged', 11, 1, NULL),
(1062, 'tx_ticket_action', 0, '重开', 'reopened', 12, 1, NULL),
(1063, 'tx_ticket_action', 0, '评价', 'evaluated', 13, 1, NULL),
(1064, 'tx_ticket_action', 0, 'SLA 提醒', 'sla_warned', 14, 1, NULL),
(1065, 'tx_ticket_action', 0, '撤销', 'cancelled', 15, 1, NULL),
(1040, 'tx_knowledge_status', 0, '草稿', 'draft', 1, 1, NULL),
(1043, 'tx_knowledge_status', 0, '待审核', 'reviewing', 2, 1, NULL),
(1044, 'tx_knowledge_status', 0, '审核驳回', 'rejected', 3, 1, NULL),
(1041, 'tx_knowledge_status', 0, '已发布', 'published', 4, 1, NULL),
(1042, 'tx_knowledge_status', 0, '已下架', 'withdrawn', 5, 1, NULL),
(1050, 'tx_ticket_system', 0, 'ERP 系统', 'erp', 1, 1, '{"ownerGroup":"应用运维一组","defaultPriority":"normal","remark":"财务、销售、库存相关问题"}'),
(1051, 'tx_ticket_system', 0, 'CRM 系统', 'crm', 2, 1, '{"ownerGroup":"应用运维二组","defaultPriority":"normal","remark":"客户、商机、跟进记录相关问题"}'),
(1052, 'tx_ticket_system', 0, 'OA 办公系统', 'oa', 3, 1, '{"ownerGroup":"综合运维组","defaultPriority":"normal","remark":"审批、流程、通知相关问题"}'),
(1053, 'tx_ticket_system', 0, '网络与基础设施', 'network', 4, 1, '{"ownerGroup":"基础设施组","defaultPriority":"high","remark":"网络、VPN、服务器与机房类问题"}')
ON DUPLICATE KEY UPDATE
label=VALUES(label),
value=VALUES(value),
sort=VALUES(sort),
status=VALUES(status),
remark=VALUES(remark);

SET FOREIGN_KEY_CHECKS = 1;
