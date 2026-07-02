package com.rbac.base.modules.system.mapper;

import com.rbac.base.core.common.vo.NameValueVO;
import com.rbac.base.core.common.vo.TrendPointVO;
import com.rbac.base.modules.system.vo.DashboardOverviewVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DashboardMapper {
    DashboardOverviewVO selectOverview();

    List<TrendPointVO> selectLoginTrend(@Param("startDateTime") LocalDateTime startDateTime,
                                        @Param("endDateTime") LocalDateTime endDateTime);

    List<TrendPointVO> selectOperTrend(@Param("startDateTime") LocalDateTime startDateTime,
                                       @Param("endDateTime") LocalDateTime endDateTime);

    List<TrendPointVO> selectFileUploadTrend(@Param("startDateTime") LocalDateTime startDateTime,
                                             @Param("endDateTime") LocalDateTime endDateTime);

    List<NameValueVO> selectLoginStatusPie();

    List<NameValueVO> selectFileStoragePie();

    List<NameValueVO> selectDeptUserTop();

    List<NameValueVO> selectRoleUserTop();
}
