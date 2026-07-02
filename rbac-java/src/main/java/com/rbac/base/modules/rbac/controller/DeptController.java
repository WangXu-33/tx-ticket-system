package com.rbac.base.modules.rbac.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rbac.base.core.common.TreeUtils;
import com.rbac.base.core.common.Result;
import com.rbac.base.modules.rbac.entity.SysDept;
import com.rbac.base.modules.rbac.mapper.SysDeptMapper;
import com.rbac.base.modules.rbac.service.DeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/dept")
public class DeptController {

    @Autowired
    private DeptService deptService;
    
    @Autowired
    private SysDeptMapper deptMapper;

    /**
     * 获取部门树
     */
    @GetMapping("/list")
    @SaCheckPermission("sys:dept:list")
    public Result<?> list() {
        return Result.success(deptMapper.selectList(new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getOrderNum)));
    }

    @GetMapping("/tree")
    @SaCheckPermission("sys:dept:list")
    public Result<?> tree() {
        List<SysDept> deptList = deptMapper.selectList(new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getOrderNum));
        return Result.success(TreeUtils.buildTree(
                deptList,
                SysDept::getId,
                SysDept::getParentId,
                0L,
                item -> {
                    SysDept node = new SysDept();
                    node.setId(item.getId());
                    node.setParentId(item.getParentId());
                    node.setDeptCode(item.getDeptCode());
                    node.setDeptName(item.getDeptName());
                    node.setOrderNum(item.getOrderNum());
                    node.setStatus(item.getStatus());
                    node.setChildren(new java.util.ArrayList<>());
                    return node;
                },
                SysDept::getChildren,
                SysDept::setChildren
        ));
    }

    @GetMapping("/detail/{id}")
    @SaCheckPermission("sys:dept:list")
    public Result<?> detail(@PathVariable Long id) {
        return Result.success(deptMapper.selectById(id));
    }

    /**
     * 新增部门 (自动生成 dept_code)
     */
    @PostMapping("/add")
    @SaCheckPermission("sys:dept:add")
    public Result<?> add(@RequestBody SysDept dept) {
        deptService.addDept(dept);
        return Result.success();
    }

    @PostMapping("/edit")
    @SaCheckPermission("sys:dept:edit")
    public Result<?> edit(@RequestBody SysDept dept) {
        deptMapper.updateById(dept);
        return Result.success();
    }

    /**
     * 利用右 LIKE 查询该部门下所有子部门 (大数据量性能优化点)
     */
    @GetMapping("/children/{deptCode}")
    @SaCheckPermission("sys:dept:list")
    public Result<?> children(@PathVariable String deptCode) {
        return Result.success(deptService.getAllChildren(deptCode));
    }

    @DeleteMapping("/delete/{id}")
    @SaCheckPermission("sys:dept:delete")
    public Result<?> delete(@PathVariable Long id) {
        deptMapper.deleteById(id);
        return Result.success();
    }
}
