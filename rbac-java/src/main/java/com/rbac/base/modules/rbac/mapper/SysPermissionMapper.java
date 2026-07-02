package com.rbac.base.modules.rbac.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rbac.base.modules.rbac.entity.SysPermission;
import com.rbac.base.modules.rbac.vo.PermissionGrantVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysPermissionMapper extends BaseMapper<SysPermission> {
    List<String> selectEffectivePermKeysByUserId(@Param("userId") Long userId);

    List<SysPermission> selectByMenuId(@Param("menuId") Long menuId);

    List<Long> selectRolePermIds(@Param("roleId") Long roleId);

    Page<PermissionGrantVO> selectPermissionGrantPage(Page<PermissionGrantVO> page,
                                                      @Param("keyword") String keyword,
                                                      @Param("menuId") Long menuId);
}
