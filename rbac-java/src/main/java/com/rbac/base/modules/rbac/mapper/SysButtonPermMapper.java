package com.rbac.base.modules.rbac.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rbac.base.modules.rbac.entity.SysButtonPerm;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface SysButtonPermMapper extends BaseMapper<SysButtonPerm> {
    @Select("SELECT p.perm_key FROM sys_button_perm p " +
            "JOIN sys_role_perm rp ON p.id = rp.perm_id " +
            "JOIN sys_user_role ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<String> getPermKeysByUserId(Long userId);
}
