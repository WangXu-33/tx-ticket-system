package com.rbac.base.modules.system.vo;

import com.rbac.base.core.common.vo.NameValueVO;
import lombok.Data;

import java.util.List;

@Data
public class DashboardDistributionVO {
    private List<NameValueVO> loginStatusPie;
    private List<NameValueVO> fileStoragePie;
    private List<NameValueVO> deptUserTop;
    private List<NameValueVO> roleUserTop;
}
