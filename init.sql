/*
 头绪工单系统 1.0 生产级数据库初始化脚本
 1. 补全所有表的等保审计字段与备注字段 (remark)
 2. 优化部门、菜单、字典、权限表结构
*/

CREATE DATABASE IF NOT EXISTS tx_ticket DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE tx_ticket;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. 部门表
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `parent_id` bigint DEFAULT 0,
  `dept_code` varchar(64) NOT NULL COMMENT '物化编码: 100, 100001',
  `dept_name` varchar(50) NOT NULL,
  `order_num` int DEFAULT 0,
  `status` tinyint DEFAULT 1,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_dept_code` (`dept_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- 2. 用户表
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dept_id` bigint DEFAULT NULL,
  `username` varchar(50) NOT NULL,
  `password` varchar(100) NOT NULL,
  `nickname` varchar(50) DEFAULT NULL,
  `avatar_file_id` bigint DEFAULT NULL COMMENT '头像文件ID',
  `email` varchar(100) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `sex` tinyint DEFAULT 0 COMMENT '性别(0未知 1男 2女)',
  `birth_date` date DEFAULT NULL COMMENT '出生日期',
  `address` varchar(255) DEFAULT NULL COMMENT '联系地址',
  `status` tinyint DEFAULT 1,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_username` (`username`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_user_status_dept` (`status`,`dept_id`),
  KEY `idx_user_avatar_file_id` (`avatar_file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 3. 角色表
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_name` varchar(50) NOT NULL,
  `role_key` varchar(50) NOT NULL,
  `data_scope` tinyint DEFAULT 1,
  `status` tinyint DEFAULT 1,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_role_key` (`role_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 4. 菜单表
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `parent_id` bigint DEFAULT 0,
  `title` varchar(50) NOT NULL,
  `path` varchar(100) DEFAULT NULL,
  `component` varchar(100) DEFAULT NULL,
  `icon` varchar(50) DEFAULT NULL,
  `sort` int DEFAULT 0,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_menu_parent_sort` (`parent_id`,`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

-- 5. 按钮权限表
DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `perm_key` varchar(100) NOT NULL,
  `status` tinyint DEFAULT 1,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_perm_key` (`perm_key`),
  KEY `idx_permission_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限按钮';

-- 6. 字典类型表
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `code` varchar(100) NOT NULL,
  `status` tinyint DEFAULT 1,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_dict_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典分类';

-- 7. 字典数据表
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type_code` varchar(100) NOT NULL,
  `parent_id` bigint DEFAULT 0,
  `label` varchar(100) NOT NULL,
  `value` varchar(100) NOT NULL,
  `sort` int DEFAULT 0,
  `status` tinyint DEFAULT 1,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dict_type_parent_sort` (`type_code`,`parent_id`,`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典数据';

-- 8. 关联表
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (`user_id` bigint NOT NULL, `role_id` bigint NOT NULL, PRIMARY KEY (`user_id`,`role_id`), KEY `idx_user_role_role` (`role_id`,`user_id`)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (`role_id` bigint NOT NULL, `menu_id` bigint NOT NULL, PRIMARY KEY (`role_id`,`menu_id`)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
DROP TABLE IF EXISTS `sys_role_perm`;
CREATE TABLE `sys_role_perm` (`role_id` bigint NOT NULL, `perm_id` bigint NOT NULL, PRIMARY KEY (`role_id`,`perm_id`), KEY `idx_role_perm_perm` (`perm_id`,`role_id`)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
DROP TABLE IF EXISTS `sys_role_dept`;
CREATE TABLE `sys_role_dept` (`role_id` bigint NOT NULL, `dept_id` bigint NOT NULL, PRIMARY KEY (`role_id`,`dept_id`), KEY `idx_role_dept_dept` (`dept_id`,`role_id`)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
DROP TABLE IF EXISTS `sys_menu_permission`;
CREATE TABLE `sys_menu_permission` (`menu_id` bigint NOT NULL, `perm_id` bigint NOT NULL, PRIMARY KEY (`menu_id`,`perm_id`), KEY `idx_menu_permission_perm` (`perm_id`,`menu_id`)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
DROP TABLE IF EXISTS `sys_user_perm`;
CREATE TABLE `sys_user_perm` (`user_id` bigint NOT NULL, `perm_id` bigint NOT NULL, PRIMARY KEY (`user_id`,`perm_id`), KEY `idx_user_permission_perm` (`perm_id`,`user_id`)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9. 操作日志表
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(50) DEFAULT '' COMMENT '模块标题',
  `business_type` int DEFAULT 0 COMMENT '业务类型（0其它 1新增 2修改 3删除）',
  `method` varchar(100) DEFAULT '' COMMENT '方法名称',
  `request_method` varchar(10) DEFAULT '' COMMENT '请求方式',
  `oper_name` varchar(50) DEFAULT '' COMMENT '操作人员',
  `oper_url` varchar(255) DEFAULT '' COMMENT '请求URL',
  `oper_ip` varchar(128) DEFAULT '' COMMENT '主机地址',
  `oper_param` varchar(2000) DEFAULT '' COMMENT '请求参数',
  `json_result` varchar(2000) DEFAULT '' COMMENT '返回参数',
  `status` int DEFAULT 0 COMMENT '操作状态（0正常 1异常）',
  `error_msg` varchar(2000) DEFAULT '' COMMENT '错误消息',
  `oper_time` datetime DEFAULT NULL COMMENT '操作时间',
  `cost_time` bigint DEFAULT 0 COMMENT '消耗时间',
  PRIMARY KEY (`id`),
  KEY `idx_oper_log_time_type_status` (`oper_time`,`business_type`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志记录';

-- 10. 登录日志表
DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) DEFAULT '' COMMENT '用户账号',
  `ipaddr` varchar(128) DEFAULT '' COMMENT '登录IP地址',
  `login_location` varchar(255) DEFAULT '' COMMENT '登录地点',
  `browser` varchar(50) DEFAULT '' COMMENT '浏览器类型',
  `os` varchar(50) DEFAULT '' COMMENT '操作系统',
  `status` tinyint DEFAULT 0 COMMENT '登录状态（0成功 1失败）',
  `msg` varchar(255) DEFAULT '' COMMENT '提示消息',
  `login_time` datetime DEFAULT NULL COMMENT '访问时间',
  PRIMARY KEY (`id`),
  KEY `idx_login_log_time_status` (`login_time`,`status`),
  KEY `idx_login_log_username_status_time` (`username`,`status`,`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统访问记录';

-- 11. 文件资源表
DROP TABLE IF EXISTS `sys_file`;
CREATE TABLE `sys_file` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `file_name` varchar(255) NOT NULL COMMENT '文件名称',
  `file_suffix` varchar(50) DEFAULT '' COMMENT '文件后缀',
  `file_size` bigint DEFAULT 0 COMMENT '文件大小',
  `url` varchar(500) NOT NULL COMMENT '访问地址',
  `storage_type` varchar(50) NOT NULL COMMENT '存储类型(LOCAL/ALIYUN/MINIO)',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_file_create_time_type_suffix` (`create_time`,`storage_type`,`file_suffix`),
  KEY `idx_file_storage_create_time` (`storage_type`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件资源表';

-- 12. 系统配置表
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `config_name` varchar(100) DEFAULT '' COMMENT '参数名称',
  `config_key` varchar(100) DEFAULT '' COMMENT '参数键名',
  `config_value` varchar(500) DEFAULT '' COMMENT '参数键值',
  `config_type` char(1) DEFAULT 'Y' COMMENT '系统内置（Y是 N否）',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='参数配置表';

-- 初始数据填充
INSERT INTO `sys_config` (config_name, config_key, config_value, config_type) VALUES 
('存储策略', 'system.storage.active', 'localStorageService', 'Y'),
('Local 存储路径', 'local.storage.path', 'E:/tx_ticket_upload/', 'Y'),
('Local 访问前缀', 'local.storage.domain', '/tx_files/', 'Y')
ON DUPLICATE KEY UPDATE config_value=VALUES(config_value);

INSERT INTO `sys_dept` (id, parent_id, dept_code, dept_name) VALUES
(1, 0, '100', '总公司'),
(2, 1, '100001', '管理部'),
(3, 1, '100002', '运营部'),
(4, 1, '100003', '审计部');

INSERT INTO `sys_user` (id, dept_id, username, password, nickname, status) VALUES
(1, 1, 'admin', 'admin123456', '超级管理员', 1),
(2, 2, 'manager', 'manager123456', '系统管理员', 1),
(3, 3, 'operator', 'operator123456', '业务操作员', 1),
(4, 4, 'auditor', 'audit123456', '审计员', 1);

INSERT INTO `sys_role` (id, role_name, role_key, data_scope, status) VALUES
(1, '超级管理员', 'admin', 1, 1),
(2, '系统管理员', 'system_admin', 1, 1),
(3, '业务操作员', 'operator', 4, 1),
(4, '审计员', 'auditor', 1, 1);

INSERT INTO `sys_user_role` VALUES
(1, 1),
(2, 2),
(3, 3),
(4, 4);

-- 初始菜单
INSERT INTO `sys_menu` (id, parent_id, title, path, component, icon, sort) VALUES
(1, 0, '系统管理', '/system', NULL, 'SettingOutlined', 1),
(2, 1, '用户管理', '/system/user', 'rbac/user/index', 'UserOutlined', 1),
(3, 1, '部门管理', '/system/dept', 'rbac/dept/index', 'ApartmentOutlined', 2),
(4, 1, '角色管理', '/system/role', 'rbac/role/index', 'TeamOutlined', 3),
(5, 1, '菜单管理', '/system/menu', 'rbac/menu/index', 'MenuOutlined', 4),
(6, 1, '字典管理', '/system/dict', '/system/dict/index', 'BookOutlined', 5),
(9, 1, '文件管理', '/system/file', '/system/file/index', 'CloudOutlined', 6),
(10, 1, '存储配置', '/system/config', '/system/config/index', 'ControlOutlined', 7),
(14, 1, '权限分析', '/rbac/analytics', '/rbac/analytics/index', 'PieChartOutlined', 8),
(11, 0, '日志管理', '/log', NULL, 'ProfileOutlined', 2),
(7, 11, '操作日志', '/log/operlog', 'log/operlog/index', 'FileTextOutlined', 1),
(8, 11, '登录日志', '/log/loginlog', 'log/loginlog/index', 'LoginOutlined', 2),
(12, 0, '研发工具', '/tool', NULL, 'BuildOutlined', 3),
(13, 12, '代码生成', '/tool/gen', '/tool/gen/index', 'CodeOutlined', 1)
ON DUPLICATE KEY UPDATE title=VALUES(title), parent_id=VALUES(parent_id), path=VALUES(path);

-- 初始按钮权限
INSERT INTO `sys_permission` (id, name, perm_key, status) VALUES
(1, '查询用户', 'sys:user:list', 1),
(2, '新增用户', 'sys:user:add', 1),
(3, '修改用户', 'sys:user:edit', 1),
(4, '删除用户', 'sys:user:delete', 1),
(5, '查询角色', 'sys:role:list', 1),
(6, '新增角色', 'sys:role:add', 1),
(7, '修改角色', 'sys:role:edit', 1),
(8, '删除角色', 'sys:role:delete', 1),
(9, '查询菜单', 'sys:menu:list', 1),
(10, '新增菜单', 'sys:menu:add', 1),
(11, '修改菜单', 'sys:menu:edit', 1),
(12, '删除菜单', 'sys:menu:delete', 1),
(13, '查询部门', 'sys:dept:list', 1),
(14, '新增部门', 'sys:dept:add', 1),
(15, '删除部门', 'sys:dept:delete', 1),
(16, '查询字典', 'sys:dict:list', 1),
(17, '查询操作日志', 'sys:operlog:list', 1),
(18, '查询登录日志', 'sys:loginlog:list', 1),
(19, '查询文件', 'sys:file:list', 1),
(20, '上传文件', 'sys:file:upload', 1),
(21, '删除文件', 'sys:file:delete', 1),
(22, '查询存储配置', 'sys:config:list', 1),
(23, '修改存储配置', 'sys:config:edit', 1),
(24, '预览代码', 'tool:gen:preview', 1),
(25, '执行生成', 'tool:gen:execute', 1),
(26, '用户直授权限', 'sys:user:assignPerm', 1),
(27, '重置用户密码', 'sys:user:resetPwd', 1),
(28, '强制用户下线', 'sys:user:kickout', 1),
(29, '导出登录日志', 'sys:loginlog:export', 1),
(30, '导出操作日志', 'sys:operlog:export', 1),
(31, '导出文件报表', 'sys:file:export', 1),
(32, '查看权限分析', 'sys:analytics:list', 1),
(33, '导出权限分析', 'sys:analytics:export', 1),
(34, '修改部门', 'sys:dept:edit', 1),
(35, '新增字典', 'sys:dict:add', 1),
(36, '修改字典', 'sys:dict:edit', 1),
(37, '删除字典', 'sys:dict:delete', 1),
(38, '查看首页', 'sys:dashboard:list', 1),
(39, '查看代码生成', 'tool:gen:list', 1)
ON DUPLICATE KEY UPDATE name=VALUES(name), status=VALUES(status);

INSERT IGNORE INTO `sys_menu_permission` (menu_id, perm_id) VALUES
(2,1),(2,2),(2,3),(2,4),(2,26),(2,27),(2,28),
(3,13),(3,14),(3,15),(3,34),
(4,5),(4,6),(4,7),(4,8),
(5,9),(5,10),(5,11),(5,12),
(6,16),(6,35),(6,36),(6,37),
(7,17),
(8,18),
(9,19),(9,20),(9,21),
(10,22),(10,23),
(14,32),(14,33),
(13,39),(13,24),(13,25);

-- 初始权限授权
INSERT IGNORE INTO `sys_role_menu` (role_id, menu_id) VALUES (1,1),(1,2),(1,3),(1,4),(1,5),(1,6),(1,7),(1,8),(1,9),(1,10),(1,11),(1,12),(1,13),(1,14);
INSERT IGNORE INTO `sys_role_perm` (role_id, perm_id)
SELECT 1, id FROM `sys_permission` WHERE del_flag = 0;

INSERT IGNORE INTO `sys_role_menu` (role_id, menu_id) VALUES
(2,1),(2,2),(2,3),(2,4),(2,5),(2,6),(2,7),(2,8),(2,9),(2,10),(2,11),(2,14),
(3,1),(3,2),(3,3),(3,6),(3,9),
(4,1),(4,7),(4,8),(4,9),(4,11),(4,14);

INSERT IGNORE INTO `sys_role_perm` (role_id, perm_id)
SELECT 2, id FROM `sys_permission`
WHERE del_flag = 0
  AND perm_key NOT IN ('tool:gen:execute');

INSERT IGNORE INTO `sys_role_perm` (role_id, perm_id)
SELECT 3, id FROM `sys_permission`
WHERE del_flag = 0
  AND perm_key IN (
    'sys:dashboard:list',
    'sys:user:list','sys:user:add','sys:user:edit','sys:user:resetPwd',
    'sys:dept:list','sys:dept:add','sys:dept:edit',
    'sys:dict:list','sys:dict:add','sys:dict:edit',
    'sys:file:list','sys:file:upload','sys:file:delete'
  );

INSERT IGNORE INTO `sys_role_perm` (role_id, perm_id)
SELECT 4, id FROM `sys_permission`
WHERE del_flag = 0
  AND perm_key IN (
    'sys:dashboard:list',
    'sys:file:list',
    'sys:operlog:list','sys:operlog:export',
    'sys:loginlog:list','sys:loginlog:export',
    'sys:analytics:list','sys:analytics:export'
  );

-- 初始字典
INSERT INTO `sys_dict_type` (id, name, code, status) VALUES
(1, '系统状态', 'sys_normal_disable', 1),
(2, '是否选项', 'sys_yes_no', 1),
(3, '用户性别', 'sys_user_sex', 1),
(4, '数据范围', 'sys_data_scope', 1),
(5, '存储类型', 'sys_storage_type', 1),
(6, '文件业务类型', 'sys_file_biz_type', 1),
(7, '操作类型', 'sys_oper_type', 1),
(8, '登录状态', 'sys_login_status', 1)
ON DUPLICATE KEY UPDATE name=VALUES(name), status=VALUES(status);

INSERT INTO `sys_dict_data` (id, type_code, parent_id, label, value, sort, status) VALUES
(1, 'sys_normal_disable', 0, '正常', '1', 1, 1),
(2, 'sys_normal_disable', 0, '停用', '0', 2, 1),
(3, 'sys_yes_no', 0, '是', 'Y', 1, 1),
(4, 'sys_yes_no', 0, '否', 'N', 2, 1),
(5, 'sys_user_sex', 0, '未知', '0', 1, 1),
(6, 'sys_user_sex', 0, '男', '1', 2, 1),
(7, 'sys_user_sex', 0, '女', '2', 3, 1),
(8, 'sys_data_scope', 0, '全部数据', '1', 1, 1),
(9, 'sys_data_scope', 0, '自定义数据', '2', 2, 1),
(10, 'sys_data_scope', 0, '本部门', '3', 3, 1),
(11, 'sys_data_scope', 0, '本部门及以下', '4', 4, 1),
(12, 'sys_data_scope', 0, '仅本人', '5', 5, 1),
(13, 'sys_storage_type', 0, '本地', 'LOCAL', 1, 1),
(14, 'sys_storage_type', 0, '阿里云OSS', 'ALIYUN', 2, 1),
(15, 'sys_storage_type', 0, 'MinIO', 'MINIO', 3, 1),
(16, 'sys_file_biz_type', 0, '头像', 'avatar', 1, 1),
(17, 'sys_file_biz_type', 0, '附件', 'attachment', 2, 1),
(18, 'sys_file_biz_type', 0, '图片', 'image', 3, 1),
(19, 'sys_file_biz_type', 0, '文档', 'document', 4, 1),
(20, 'sys_file_biz_type', 0, '报告', 'report', 5, 1),
(21, 'sys_file_biz_type', 0, '其他', 'other', 6, 1),
(22, 'sys_oper_type', 0, '新增', '1', 1, 1),
(23, 'sys_oper_type', 0, '修改', '2', 2, 1),
(24, 'sys_oper_type', 0, '删除', '3', 3, 1),
(25, 'sys_oper_type', 0, '导入', '4', 4, 1),
(26, 'sys_oper_type', 0, '导出', '5', 5, 1),
(27, 'sys_oper_type', 0, '上传', '6', 6, 1),
(28, 'sys_oper_type', 0, '下载', '7', 7, 1),
(29, 'sys_oper_type', 0, '授权', '8', 8, 1),
(30, 'sys_oper_type', 0, '强退', '9', 9, 1),
(31, 'sys_login_status', 0, '成功', '0', 1, 1),
(32, 'sys_login_status', 0, '失败', '1', 2, 1)
ON DUPLICATE KEY UPDATE label=VALUES(label), value=VALUES(value), sort=VALUES(sort), status=VALUES(status);

-- TX Ticket System business tables and seed data.
-- 说明：业务表初始化使用 IF NOT EXISTS，不对工单业务数据执行物理删除。
CREATE TABLE IF NOT EXISTS `tx_ticket` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ticket_no` varchar(40) NOT NULL COMMENT '工单编号',
  `title` varchar(200) NOT NULL COMMENT '工单标题',
  `description` text NOT NULL COMMENT '问题描述',
  `category` varchar(50) DEFAULT 'general' COMMENT '工单分类',
  `priority` varchar(30) DEFAULT 'normal' COMMENT '优先级',
  `status` varchar(40) DEFAULT 'pending' COMMENT '当前状态',
  `creator_id` bigint NOT NULL COMMENT '创建人ID',
  `creator_name` varchar(100) DEFAULT NULL COMMENT '创建人名称',
  `creator_phone` varchar(30) DEFAULT NULL COMMENT '联系电话',
  `creator_email` varchar(120) DEFAULT NULL COMMENT '联系邮箱',
  `handler_id` bigint DEFAULT NULL COMMENT '当前处理人ID',
  `handler_name` varchar(100) DEFAULT NULL COMMENT '当前处理人名称',
  `solution` text DEFAULT NULL COMMENT '最终解决方案',
  `resolved_time` datetime DEFAULT NULL COMMENT '解决时间',
  `closed_time` datetime DEFAULT NULL COMMENT '关闭时间',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_tx_ticket_no` (`ticket_no`),
  KEY `idx_tx_ticket_status_priority` (`status`,`priority`),
  KEY `idx_tx_ticket_creator_time` (`creator_id`,`create_time`),
  KEY `idx_tx_ticket_handler_status` (`handler_id`,`status`)
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
  `status` varchar(30) DEFAULT 'draft' COMMENT '状态 draft/published/withdrawn',
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
  KEY `idx_tx_knowledge_source_ticket` (`source_ticket_id`)
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

INSERT INTO `sys_role` (id, role_name, role_key, data_scope, status, remark) VALUES
(5, '客户', 'customer', 5, 1, '客户注册后的默认角色')
ON DUPLICATE KEY UPDATE role_name=VALUES(role_name), role_key=VALUES(role_key), status=VALUES(status);

INSERT INTO `sys_menu` (id, parent_id, title, path, component, icon, sort) VALUES
(100, 0, '工单中心', '/ticket', NULL, 'CustomerServiceOutlined', 4),
(101, 100, '工单工作台', '/ticket/workbench', '/ticket/workbench/index', 'ToolOutlined', 1),
(102, 100, '我的工单', '/ticket/my', '/ticket/my/index', 'ProfileOutlined', 2),
(103, 100, '知识库', '/ticket/knowledge', '/ticket/knowledge/index', 'BookOutlined', 3)
ON DUPLICATE KEY UPDATE title=VALUES(title), parent_id=VALUES(parent_id), path=VALUES(path), component=VALUES(component), icon=VALUES(icon), sort=VALUES(sort);

INSERT INTO `sys_permission` (id, name, perm_key, status) VALUES
(100, '查看工单工作台', 'ticket:list', 1),
(101, '新增工单', 'ticket:add', 1),
(102, '接单', 'ticket:receive', 1),
(103, '回复工单', 'ticket:reply', 1),
(104, '分派工单', 'ticket:assign', 1),
(105, '转派工单', 'ticket:transfer', 1),
(106, '解决工单', 'ticket:resolve', 1),
(107, '关闭工单', 'ticket:close', 1),
(108, '驳回工单', 'ticket:reject', 1),
(109, '查看我的工单', 'ticket:my:list', 1),
(110, '客户提交工单', 'ticket:my:add', 1),
(111, '客户回复工单', 'ticket:my:reply', 1),
(112, '客户关闭工单', 'ticket:my:close', 1),
(120, '查看知识库', 'knowledge:list', 1),
(121, '维护知识库', 'knowledge:edit', 1),
(122, '发布知识库', 'knowledge:publish', 1)
ON DUPLICATE KEY UPDATE name=VALUES(name), perm_key=VALUES(perm_key), status=VALUES(status);

INSERT IGNORE INTO `sys_menu_permission` (menu_id, perm_id) VALUES
(101,100),(101,101),(101,102),(101,103),(101,104),(101,105),(101,106),(101,107),(101,108),
(102,109),(102,110),(102,111),(102,112),
(103,120),(103,121),(103,122);

INSERT IGNORE INTO `sys_role_menu` (role_id, menu_id) VALUES
(1,100),(1,101),(1,102),(1,103),
(2,100),(2,101),(2,102),(2,103),
(3,100),(3,101),(3,102),(3,103),
(5,100),(5,102);

INSERT IGNORE INTO `sys_role_perm` (role_id, perm_id)
SELECT 1, id FROM `sys_permission`
WHERE del_flag = 0
  AND (perm_key LIKE 'ticket:%' OR perm_key LIKE 'knowledge:%');

INSERT IGNORE INTO `sys_role_perm` (role_id, perm_id)
SELECT 2, id FROM `sys_permission`
WHERE del_flag = 0
  AND (perm_key LIKE 'ticket:%' OR perm_key LIKE 'knowledge:%');

INSERT IGNORE INTO `sys_role_perm` (role_id, perm_id)
SELECT 3, id FROM `sys_permission`
WHERE del_flag = 0
  AND perm_key IN (
    'ticket:list','ticket:receive','ticket:reply','ticket:assign','ticket:transfer',
    'ticket:resolve','ticket:close','ticket:reject','knowledge:list','knowledge:edit',
    'sys:file:upload'
  );

INSERT IGNORE INTO `sys_role_perm` (role_id, perm_id)
SELECT 5, id FROM `sys_permission`
WHERE del_flag = 0
  AND perm_key IN (
    'sys:dashboard:list','ticket:my:list','ticket:my:add','ticket:my:reply','ticket:my:close','sys:file:upload'
  );

INSERT INTO `sys_dict_type` (id, name, code, status) VALUES
(100, '工单状态', 'tx_ticket_status', 1),
(101, '工单优先级', 'tx_ticket_priority', 1),
(102, '工单分类', 'tx_ticket_category', 1),
(103, '工单流程动作', 'tx_ticket_action', 1),
(104, '知识库状态', 'tx_knowledge_status', 1)
ON DUPLICATE KEY UPDATE name=VALUES(name), code=VALUES(code), status=VALUES(status);

INSERT INTO `sys_dict_data` (id, type_code, parent_id, label, value, sort, status) VALUES
(1000, 'tx_ticket_status', 0, '待受理', 'pending', 1, 1),
(1001, 'tx_ticket_status', 0, '处理中', 'processing', 2, 1),
(1002, 'tx_ticket_status', 0, '待客户补充', 'waiting_customer', 3, 1),
(1003, 'tx_ticket_status', 0, '已转派', 'transferred', 4, 1),
(1004, 'tx_ticket_status', 0, '已解决', 'resolved', 5, 1),
(1005, 'tx_ticket_status', 0, '已关闭', 'closed', 6, 1),
(1006, 'tx_ticket_status', 0, '已驳回', 'rejected', 7, 1),
(1010, 'tx_ticket_priority', 0, '低', 'low', 1, 1),
(1011, 'tx_ticket_priority', 0, '普通', 'normal', 2, 1),
(1012, 'tx_ticket_priority', 0, '高', 'high', 3, 1),
(1013, 'tx_ticket_priority', 0, '紧急', 'urgent', 4, 1),
(1020, 'tx_ticket_category', 0, '通用问题', 'general', 1, 1),
(1021, 'tx_ticket_category', 0, '网络问题', 'network', 2, 1),
(1022, 'tx_ticket_category', 0, '系统故障', 'system', 3, 1),
(1023, 'tx_ticket_category', 0, '账号权限', 'account', 4, 1),
(1030, 'tx_ticket_action', 0, '创建工单', 'created', 1, 1),
(1031, 'tx_ticket_action', 0, '接单', 'received', 2, 1),
(1032, 'tx_ticket_action', 0, '回复', 'replied', 3, 1),
(1033, 'tx_ticket_action', 0, '分派', 'assigned', 4, 1),
(1034, 'tx_ticket_action', 0, '转派', 'transferred', 5, 1),
(1035, 'tx_ticket_action', 0, '解决', 'resolved', 6, 1),
(1036, 'tx_ticket_action', 0, '关闭', 'closed', 7, 1),
(1037, 'tx_ticket_action', 0, '驳回', 'rejected', 8, 1),
(1040, 'tx_knowledge_status', 0, '草稿', 'draft', 1, 1),
(1041, 'tx_knowledge_status', 0, '已发布', 'published', 2, 1),
(1042, 'tx_knowledge_status', 0, '已下架', 'withdrawn', 3, 1)
ON DUPLICATE KEY UPDATE label=VALUES(label), value=VALUES(value), sort=VALUES(sort), status=VALUES(status);

SET FOREIGN_KEY_CHECKS = 1;
