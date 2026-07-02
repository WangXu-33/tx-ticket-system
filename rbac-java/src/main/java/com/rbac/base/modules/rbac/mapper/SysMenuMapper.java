package com.rbac.base.modules.rbac.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rbac.base.modules.rbac.entity.SysMenu;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface SysMenuMapper extends BaseMapper<SysMenu> {
    @Select("SELECT m.* FROM sys_menu m " +
            "JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "JOIN sys_user_role ur ON rm.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND m.del_flag = 0 " +
            "GROUP BY m.id, m.parent_id, m.title, m.path, m.component, m.icon, m.sort, m.create_by, m.create_time, m.update_by, m.update_time, m.remark, m.del_flag " +
            "ORDER BY m.sort")
    List<SysMenu> getMenusByUserId(Long userId);
}
