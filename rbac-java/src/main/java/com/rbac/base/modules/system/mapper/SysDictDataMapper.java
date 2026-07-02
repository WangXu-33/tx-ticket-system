package com.rbac.base.modules.system.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rbac.base.core.common.vo.NameValueVO;
import com.rbac.base.modules.system.entity.SysDictData;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SysDictDataMapper extends BaseMapper<SysDictData> {
    @Select("SELECT COUNT(*) FROM sys_dict_data WHERE del_flag = 0")
    Long countActiveDataItems();

    List<NameValueVO> selectTypeCodeTop();
}
