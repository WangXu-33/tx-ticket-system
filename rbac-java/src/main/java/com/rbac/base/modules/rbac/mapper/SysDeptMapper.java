package com.rbac.base.modules.rbac.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rbac.base.modules.rbac.entity.SysDept;
import org.apache.ibatis.annotations.Select;

public interface SysDeptMapper extends BaseMapper<SysDept> {
    
    /**
     * 绕过逻辑删除，查询父级下曾经生成过的最大部门编码
     * @param parentId 父级ID
     * @return 最大编码
     */
    @Select("SELECT MAX(dept_code) FROM sys_dept WHERE parent_id = #{parentId}")
    String selectMaxCodeByParentId(Long parentId);
}
