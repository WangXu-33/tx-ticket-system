package com.rbac.base.modules.rbac.mapper;

import com.rbac.base.core.common.vo.NameValueVO;
import com.rbac.base.modules.rbac.vo.PermissionUsageVO;
import com.rbac.base.modules.rbac.vo.RbacOverviewVO;
import com.rbac.base.modules.rbac.vo.UserAuthTopVO;

import java.util.List;

public interface RbacAnalyticsMapper {
    RbacOverviewVO selectOverview();

    List<NameValueVO> selectRoleDistribution();

    List<PermissionUsageVO> selectPermissionUsage();

    List<UserAuthTopVO> selectUserAuthTop();
}
