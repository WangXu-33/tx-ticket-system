package com.rbac.base.modules.rbac.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rbac.base.modules.rbac.entity.SysUser;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface SysUserMapper extends BaseMapper<SysUser> {
    
    /**
     * 更新为从 sys_permission 表加载权限码
     */
    @Select("SELECT p.perm_key FROM sys_permission p " +
            "JOIN sys_role_perm rp ON p.id = rp.perm_id " +
            "JOIN sys_user_role ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND p.del_flag = 0")
    List<String> getPermissionsByUserId(Long userId);

    @Select("SELECT r.role_key FROM sys_role r " +
            "JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.del_flag = 0")
    List<String> getRolesByUserId(Long userId);
}
