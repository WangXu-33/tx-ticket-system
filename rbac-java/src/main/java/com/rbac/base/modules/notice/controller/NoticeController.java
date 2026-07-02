package com.rbac.base.modules.notice.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.rbac.base.core.annotation.Log;
import com.rbac.base.core.common.Result;
import com.rbac.base.modules.notice.dto.NoticeSaveDTO;
import com.rbac.base.modules.notice.service.NoticeService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/system/notice")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping("/list")
    @SaCheckPermission("sys:notice:list")
    public Result<?> list(@RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Integer status) {
        return Result.success(noticeService.listManage(keyword, status));
    }

    @GetMapping("/detail/{id}")
    @SaCheckPermission("sys:notice:list")
    public Result<?> detail(@PathVariable Long id) {
        return Result.success(noticeService.detail(id));
    }

    @GetMapping("/targets/{id}")
    @SaCheckPermission("sys:notice:list")
    public Result<?> targets(@PathVariable Long id) {
        return Result.success(noticeService.targets(id));
    }

    @PostMapping("/add")
    @SaCheckPermission("sys:notice:add")
    @Log(title = "公告消息", businessType = 1)
    public Result<?> add(@RequestBody NoticeSaveDTO dto) {
        dto.setId(null);
        return Result.success(noticeService.saveDraft(dto));
    }

    @PostMapping("/edit")
    @SaCheckPermission("sys:notice:edit")
    @Log(title = "公告消息", businessType = 2)
    public Result<?> edit(@RequestBody NoticeSaveDTO dto) {
        return Result.success(noticeService.saveDraft(dto));
    }

    @PostMapping("/publish/{id}")
    @SaCheckPermission("sys:notice:publish")
    @Log(title = "发布公告", businessType = 2)
    public Result<?> publish(@PathVariable Long id) {
        noticeService.publish(id);
        return Result.success();
    }

    @PostMapping("/withdraw/{id}")
    @SaCheckPermission("sys:notice:withdraw")
    @Log(title = "撤回公告", businessType = 2)
    public Result<?> withdraw(@PathVariable Long id) {
        noticeService.withdraw(id);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    @SaCheckPermission("sys:notice:delete")
    @Log(title = "删除公告", businessType = 3)
    public Result<?> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return Result.success();
    }

    @GetMapping("/stats/{id}")
    @SaCheckPermission("sys:notice:list")
    public Result<?> stats(@PathVariable Long id) {
        return Result.success(noticeService.getStats(id));
    }

    @GetMapping("/my")
    @SaCheckPermission("sys:notice:read")
    public Result<?> my(@RequestParam(required = false) Integer readFlag,
                        @RequestParam(required = false) String keyword) {
        return Result.success(noticeService.listMy(StpUtil.getLoginIdAsLong(), readFlag, keyword));
    }

    @GetMapping("/my/unread-count")
    @SaCheckPermission("sys:notice:read")
    public Result<?> unreadCount() {
        return Result.success(noticeService.countUnread(StpUtil.getLoginIdAsLong()));
    }

    @PostMapping("/my/read/{id}")
    @SaCheckPermission("sys:notice:read")
    public Result<?> markRead(@PathVariable Long id) {
        noticeService.markRead(id, StpUtil.getLoginIdAsLong());
        return Result.success();
    }
}
