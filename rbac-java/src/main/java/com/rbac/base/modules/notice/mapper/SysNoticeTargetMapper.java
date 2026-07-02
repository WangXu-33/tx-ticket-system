package com.rbac.base.modules.notice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rbac.base.modules.notice.entity.SysNoticeTarget;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface SysNoticeTargetMapper extends BaseMapper<SysNoticeTarget> {
    @Delete("DELETE FROM sys_notice_target WHERE notice_id = #{noticeId}")
    void deleteByNoticeId(@Param("noticeId") Long noticeId);
}
