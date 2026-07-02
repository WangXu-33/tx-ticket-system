package com.rbac.base.modules.rbac.service;

import com.rbac.base.core.common.vo.NameValueVO;
import com.rbac.base.modules.rbac.mapper.RbacAnalyticsMapper;
import com.rbac.base.modules.rbac.vo.PermissionUsageVO;
import com.rbac.base.modules.rbac.vo.RbacOverviewVO;
import com.rbac.base.modules.rbac.vo.UserAuthTopVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RbacAnalyticsService {

    private final RbacAnalyticsMapper analyticsMapper;

    public RbacAnalyticsService(RbacAnalyticsMapper analyticsMapper) {
        this.analyticsMapper = analyticsMapper;
    }

    public RbacOverviewVO getOverview() {
        return analyticsMapper.selectOverview();
    }

    public List<NameValueVO> getRoleDistribution() {
        return analyticsMapper.selectRoleDistribution();
    }

    public List<PermissionUsageVO> getPermissionUsage() {
        return analyticsMapper.selectPermissionUsage();
    }

    public List<UserAuthTopVO> getUserAuthTop() {
        return analyticsMapper.selectUserAuthTop();
    }
}
