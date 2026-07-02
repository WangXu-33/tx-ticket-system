package com.rbac.base.modules.rbac.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rbac.base.modules.rbac.entity.SysDept;
import com.rbac.base.modules.rbac.mapper.SysDeptMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DeptService {

    @Autowired
    private SysDeptMapper deptMapper;

    /**
     * 核心：物化路径编码生成逻辑 (更好的轮子)
     * 生成规则：父级Code + 3位递增数字 (例如 100 -> 100001, 100002)
     */
    @Transactional
    public void addDept(SysDept dept) {
        String parentCode = "";
        if (dept.getParentId() != 0) {
            SysDept parent = Optional.ofNullable(deptMapper.selectById(dept.getParentId()))
                    .orElseThrow(() -> new RuntimeException("父部门不存在"));
            parentCode = parent.getDeptCode();
        }

        // 查询当前父级下的最大编码 (绕过逻辑删除，防止由于删除了 100001 又生成 100001 导致唯一键冲突)
        String maxCode = deptMapper.selectMaxCodeByParentId(dept.getParentId());
        
        if (maxCode == null || maxCode.isEmpty()) {
            // 如果没有任何子项（不管是否被删除过），初始为 000
            maxCode = parentCode + "000";
        }

        // 截取最后3位并递增
        int sequence = Integer.parseInt(maxCode.substring(maxCode.length() - 3)) + 1;
        String newCode = parentCode + String.format("%03d", sequence);
        
        dept.setDeptCode(newCode);
        deptMapper.insert(dept);
    }

    /**
     * 极致性能：利用 B-Tree 索引的前缀匹配特性
     */
    public List<SysDept> getAllChildren(String deptCode) {
        return deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .likeRight(SysDept::getDeptCode, deptCode)
                .orderByAsc(SysDept::getDeptCode));
    }
}
