package com.rbac.base.modules.system.vo;

import com.rbac.base.core.common.vo.NameValueVO;
import lombok.Data;

import java.util.List;

@Data
public class DictSummaryVO {
    private Long typeTotal;
    private Long enabledTypeTotal;
    private Long disabledTypeTotal;
    private Long dataItemTotal;
    private List<NameValueVO> dataItemTop;
}
