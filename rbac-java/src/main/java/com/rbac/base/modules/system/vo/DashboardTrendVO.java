package com.rbac.base.modules.system.vo;

import com.rbac.base.core.common.vo.TrendPointVO;
import lombok.Data;

import java.util.List;

@Data
public class DashboardTrendVO {
    private List<TrendPointVO> loginTrend;
    private List<TrendPointVO> operTrend;
    private List<TrendPointVO> fileUploadTrend;
}
