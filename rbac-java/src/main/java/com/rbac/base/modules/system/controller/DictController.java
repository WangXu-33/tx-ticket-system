package com.rbac.base.modules.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rbac.base.core.common.Result;
import com.rbac.base.modules.system.entity.SysDictData;
import com.rbac.base.modules.system.entity.SysDictType;
import com.rbac.base.modules.system.mapper.SysDictDataMapper;
import com.rbac.base.modules.system.mapper.SysDictTypeMapper;
import com.rbac.base.modules.system.vo.DictSummaryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/system/dict")
public class DictController {

    @Autowired
    private SysDictTypeMapper dictTypeMapper;

    @Autowired
    private SysDictDataMapper dictDataMapper;

    @GetMapping("/type/list")
    @SaCheckPermission("sys:dict:list")
    public Result<?> typeList(@RequestParam(required = false) String keyword) {
        return Result.success(dictTypeMapper.selectList(new LambdaQueryWrapper<SysDictType>()
                .and(keyword != null && !keyword.isEmpty(),
                        w -> w.like(SysDictType::getName, keyword).or().like(SysDictType::getCode, keyword))
                .orderByAsc(SysDictType::getId)));
    }

    @GetMapping("/type/detail/{id}")
    @SaCheckPermission("sys:dict:list")
    public Result<?> typeDetail(@PathVariable Long id) {
        return Result.success(dictTypeMapper.selectById(id));
    }

    @PostMapping("/type/add")
    @SaCheckPermission("sys:dict:add")
    public Result<?> typeAdd(@RequestBody SysDictType type) {
        dictTypeMapper.insert(type);
        return Result.success();
    }

    @PostMapping("/type/edit")
    @SaCheckPermission("sys:dict:edit")
    public Result<?> typeEdit(@RequestBody SysDictType type) {
        dictTypeMapper.updateById(type);
        return Result.success();
    }

    @DeleteMapping("/type/delete/{id}")
    @SaCheckPermission("sys:dict:delete")
    public Result<?> typeDelete(@PathVariable Long id) {
        dictTypeMapper.deleteById(id);
        return Result.success();
    }

    @GetMapping("/data/list")
    @SaCheckPermission("sys:dict:list")
    public Result<?> dataList(@RequestParam String typeCode) {
        List<SysDictData> list = dictDataMapper.selectList(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getTypeCode, typeCode)
                .orderByAsc(SysDictData::getSort));

        Map<Long, SysDictData> map = list.stream().collect(Collectors.toMap(SysDictData::getId, d -> d));
        List<SysDictData> tree = new ArrayList<>();
        for (SysDictData item : list) {
            if (item.getParentId() == null || item.getParentId() == 0) {
                tree.add(item);
            } else {
                SysDictData parent = map.get(item.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(item);
                }
            }
        }
        return Result.success(tree);
    }

    @GetMapping("/data/detail/{id}")
    @SaCheckPermission("sys:dict:list")
    public Result<?> dataDetail(@PathVariable Long id) {
        return Result.success(dictDataMapper.selectById(id));
    }

    @PostMapping("/data/add")
    @SaCheckPermission("sys:dict:add")
    public Result<?> dataAdd(@RequestBody SysDictData data) {
        dictDataMapper.insert(data);
        return Result.success();
    }

    @PostMapping("/data/edit")
    @SaCheckPermission("sys:dict:edit")
    public Result<?> dataEdit(@RequestBody SysDictData data) {
        dictDataMapper.updateById(data);
        return Result.success();
    }

    @DeleteMapping("/data/delete/{id}")
    @SaCheckPermission("sys:dict:delete")
    public Result<?> dataDelete(@PathVariable Long id) {
        dictDataMapper.deleteById(id);
        return Result.success();
    }

    @GetMapping("/stats/summary")
    @SaCheckPermission("sys:dict:list")
    public Result<?> summary() {
        DictSummaryVO summary = new DictSummaryVO();
        summary.setTypeTotal(zeroIfNull(dictTypeMapper.countActiveTypes()));
        summary.setEnabledTypeTotal(zeroIfNull(dictTypeMapper.countEnabledTypes()));
        summary.setDisabledTypeTotal(zeroIfNull(dictTypeMapper.countDisabledTypes()));
        summary.setDataItemTotal(zeroIfNull(dictDataMapper.countActiveDataItems()));
        summary.setDataItemTop(dictDataMapper.selectTypeCodeTop());
        return Result.success(summary);
    }

    private long zeroIfNull(Long value) {
        return value == null ? 0L : value;
    }
}
