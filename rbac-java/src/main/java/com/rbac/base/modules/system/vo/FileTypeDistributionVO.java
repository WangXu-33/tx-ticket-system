package com.rbac.base.modules.system.vo;

import com.rbac.base.core.common.vo.NameValueVO;
import lombok.Data;

import java.util.List;

@Data
public class FileTypeDistributionVO {
    private List<NameValueVO> storageTypePie;
    private List<NameValueVO> suffixTop;
}
