package com.rbac.base.modules.log.vo;

import com.rbac.base.core.common.vo.NameValueVO;
import lombok.Data;

import java.util.List;

@Data
public class LoginLogSummaryVO {
    private Long totalCount;
    private Long successCount;
    private Long failCount;
    private Long todayCount;
    private List<NameValueVO> statusPie;
}
