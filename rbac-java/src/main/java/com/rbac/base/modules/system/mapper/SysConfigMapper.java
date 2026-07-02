package com.rbac.base.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rbac.base.modules.system.entity.SysConfig;
import org.apache.ibatis.annotations.Select;

public interface SysConfigMapper extends BaseMapper<SysConfig> {
    
    @Select("SELECT config_value FROM sys_config WHERE config_key = #{configKey}")
    String getConfigValue(String configKey);
}
