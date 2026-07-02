package com.rbac.base.modules.rbac.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rbac.base.core.common.TreeUtils;
import com.rbac.base.core.common.Result;
import com.rbac.base.modules.rbac.entity.SysMenu;
import com.rbac.base.modules.rbac.mapper.SysMenuMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/menu")
public class MenuController {

    @Autowired
    private SysMenuMapper menuMapper;

    @GetMapping("/list")
    @SaCheckPermission("sys:menu:list")
    public Result<?> list() {
        return Result.success(menuMapper.selectList(new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSort)));
    }

    @GetMapping("/tree")
    @SaCheckPermission("sys:menu:list")
    public Result<?> tree() {
        List<SysMenu> menuList = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSort));
        return Result.success(TreeUtils.buildTree(
                menuList,
                SysMenu::getId,
                SysMenu::getParentId,
                0L,
                item -> {
                    SysMenu node = new SysMenu();
                    node.setId(item.getId());
                    node.setParentId(item.getParentId());
                    node.setTitle(item.getTitle());
                    node.setPath(item.getPath());
                    node.setComponent(item.getComponent());
                    node.setIcon(item.getIcon());
                    node.setSort(item.getSort());
                    node.setChildren(new java.util.ArrayList<>());
                    return node;
                },
                SysMenu::getChildren,
                SysMenu::setChildren
        ));
    }

    @GetMapping("/detail/{id}")
    @SaCheckPermission("sys:menu:list")
    public Result<?> detail(@PathVariable Long id) {
        return Result.success(menuMapper.selectById(id));
    }

    @PostMapping("/add")
    @SaCheckPermission("sys:menu:add")
    public Result<?> add(@RequestBody SysMenu menu) {
        menuMapper.insert(menu);
        return Result.success();
    }

    @PostMapping("/edit")
    @SaCheckPermission("sys:menu:edit")
    public Result<?> edit(@RequestBody SysMenu menu) {
        menuMapper.updateById(menu);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    @SaCheckPermission("sys:menu:delete")
    public Result<?> delete(@PathVariable Long id) {
        menuMapper.deleteById(id);
        return Result.success();
    }
}
