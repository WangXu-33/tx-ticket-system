package com.rbac.base.modules.notice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rbac.base.modules.notice.entity.SysNotice;
import com.rbac.base.modules.notice.vo.NoticeListVO;
import com.rbac.base.modules.notice.vo.NoticeMyVO;
import com.rbac.base.modules.notice.vo.NoticeStatsVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysNoticeMapper extends BaseMapper<SysNotice> {
    List<NoticeListVO> selectManageList(@Param("keyword") String keyword,
                                        @Param("status") Integer status);

    List<NoticeMyVO> selectMyList(@Param("userId") Long userId,
                                  @Param("readFlag") Integer readFlag,
                                  @Param("keyword") String keyword);

    NoticeStatsVO selectStats(@Param("noticeId") Long noticeId);
}
