-- 修改时间：2026-07-05
-- 功能说明：回收老库 operator 角色误保留的 ticket:assign / ticket:reject 权限。
-- 执行原则：先归档再删除，避免直接丢失历史授权关系。

CREATE TABLE IF NOT EXISTS `sys_role_perm_his` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_id` bigint NOT NULL,
  `perm_id` bigint NOT NULL,
  `archive_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `archive_by` varchar(64) NOT NULL DEFAULT 'script',
  `operation_type` varchar(32) NOT NULL DEFAULT 'revoke',
  `remark` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_role_perm_his_role_perm` (`role_id`, `perm_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `sys_role_perm_his` (`role_id`, `perm_id`, `archive_time`, `archive_by`, `operation_type`, `remark`)
SELECT rp.`role_id`,
       rp.`perm_id`,
       NOW(),
       'cleanup_operator_ticket_permissions.sql',
       'revoke',
       '回收 operator 的 ticket:assign / ticket:reject 历史授权'
FROM `sys_role_perm` rp
JOIN `sys_role` r ON r.`id` = rp.`role_id`
JOIN `sys_permission` p ON p.`id` = rp.`perm_id`
LEFT JOIN `sys_role_perm_his` h
       ON h.`role_id` = rp.`role_id`
      AND h.`perm_id` = rp.`perm_id`
      AND h.`operation_type` = 'revoke'
      AND h.`remark` = '回收 operator 的 ticket:assign / ticket:reject 历史授权'
WHERE r.`role_key` = 'operator'
  AND p.`perm_key` IN ('ticket:assign', 'ticket:reject')
  AND h.`id` IS NULL;

DELETE rp
FROM `sys_role_perm` rp
JOIN `sys_role` r ON r.`id` = rp.`role_id`
JOIN `sys_permission` p ON p.`id` = rp.`perm_id`
WHERE r.`role_key` = 'operator'
  AND p.`perm_key` IN ('ticket:assign', 'ticket:reject');

SELECT r.`role_key`, p.`perm_key`
FROM `sys_role_perm` rp
JOIN `sys_role` r ON r.`id` = rp.`role_id`
JOIN `sys_permission` p ON p.`id` = rp.`perm_id`
WHERE r.`role_key` = 'operator'
  AND p.`perm_key` IN ('ticket:assign', 'ticket:reject');
