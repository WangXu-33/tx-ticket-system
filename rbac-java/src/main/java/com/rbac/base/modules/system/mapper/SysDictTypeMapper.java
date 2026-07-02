package com.rbac.base.modules.system.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rbac.base.modules.system.entity.SysDictType;
import org.apache.ibatis.annotations.Select;

public interface SysDictTypeMapper extends BaseMapper<SysDictType> {
    @Select("SELECT COUNT(*) FROM sys_dict_type WHERE del_flag = 0")
    Long countActiveTypes();

    @Select("SELECT COUNT(*) FROM sys_dict_type WHERE del_flag = 0 AND status = 1")
    Long countEnabledTypes();

    @Select("SELECT COUNT(*) FROM sys_dict_type WHERE del_flag = 0 AND status = 0")
    Long countDisabledTypes();
}
