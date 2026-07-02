package com.rbac.base.modules.notice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rbac.base.modules.notice.entity.SysNoticeRecipient;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface SysNoticeRecipientMapper extends BaseMapper<SysNoticeRecipient> {
    @Delete("DELETE FROM sys_notice_recipient WHERE notice_id = #{noticeId}")
    void deleteByNoticeId(@Param("noticeId") Long noticeId);

    @Update("UPDATE sys_notice_recipient SET read_flag = 1, read_time = NOW() WHERE notice_id = #{noticeId} AND user_id = #{userId} AND read_flag = 0")
    void markRead(@Param("noticeId") Long noticeId, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM sys_notice_recipient WHERE user_id = #{userId} AND read_flag = 0")
    Long countUnreadByUserId(@Param("userId") Long userId);
}
