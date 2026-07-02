package com.rbac.base.modules.notice.service;

import com.rbac.base.modules.notice.entity.SysNotice;
import com.rbac.base.modules.notice.entity.SysNoticeRecipient;
import com.rbac.base.modules.notice.entity.SysNoticeTarget;
import com.rbac.base.modules.notice.mapper.SysNoticeMapper;
import com.rbac.base.modules.notice.mapper.SysNoticeRecipientMapper;
import com.rbac.base.modules.notice.mapper.SysNoticeTargetMapper;
import com.rbac.base.modules.realtime.service.RealtimeMessageService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NoticeServiceTest {

    @Test
    void publishCreatesRecipientSnapshotWithExcludes() {
        SysNoticeMapper noticeMapper = mock(SysNoticeMapper.class);
        SysNoticeTargetMapper targetMapper = mock(SysNoticeTargetMapper.class);
        SysNoticeRecipientMapper recipientMapper = mock(SysNoticeRecipientMapper.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        RealtimeMessageService realtimeMessageService = mock(RealtimeMessageService.class);
        NoticeService service = new NoticeService(noticeMapper, targetMapper, recipientMapper, jdbcTemplate, realtimeMessageService);
        SysNotice notice = notice(9L);
        when(noticeMapper.selectById(9L)).thenReturn(notice);
        when(targetMapper.selectList(any())).thenReturn(List.of(
                target("ALL", null, "INCLUDE"),
                target("USER", 2L, "EXCLUDE")
        ));
        when(jdbcTemplate.queryForList(NoticeService.SQL_ENABLED_USER_IDS, Long.class)).thenReturn(List.of(1L, 2L, 3L));

        service.publish(9L);

        ArgumentCaptor<SysNoticeRecipient> recipientCaptor = ArgumentCaptor.forClass(SysNoticeRecipient.class);
        verify(recipientMapper).deleteByNoticeId(9L);
        verify(recipientMapper, org.mockito.Mockito.times(2)).insert(recipientCaptor.capture());
        List<Long> recipientUserIds = recipientCaptor.getAllValues().stream()
                .map(SysNoticeRecipient::getUserId)
                .sorted()
                .toList();
        assertEquals(List.of(1L, 3L), recipientUserIds);
        verify(realtimeMessageService).sendUser(eq(1L), eq("notice-new"), any());
        verify(realtimeMessageService).sendUser(eq(3L), eq("notice-new"), any());
        assertEquals(1, notice.getStatus());
    }

    @Test
    void publishDeDuplicatesDepartmentRoleAndUserTargets() {
        SysNoticeMapper noticeMapper = mock(SysNoticeMapper.class);
        SysNoticeTargetMapper targetMapper = mock(SysNoticeTargetMapper.class);
        SysNoticeRecipientMapper recipientMapper = mock(SysNoticeRecipientMapper.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        RealtimeMessageService realtimeMessageService = mock(RealtimeMessageService.class);
        NoticeService service = new NoticeService(noticeMapper, targetMapper, recipientMapper, jdbcTemplate, realtimeMessageService);
        when(noticeMapper.selectById(10L)).thenReturn(notice(10L));
        when(targetMapper.selectList(any())).thenReturn(List.of(
                target("DEPT", 5L, "INCLUDE"),
                target("ROLE", 6L, "INCLUDE"),
                target("USER", 7L, "INCLUDE")
        ));
        when(jdbcTemplate.queryForList(eq(NoticeService.SQL_DEPT_USER_IDS), eq(Long.class), eq(5L), eq(5L))).thenReturn(List.of(7L, 8L));
        when(jdbcTemplate.queryForList(eq(NoticeService.SQL_ROLE_USER_IDS), eq(Long.class), eq(6L))).thenReturn(List.of(8L, 9L));

        service.publish(10L);

        ArgumentCaptor<SysNoticeRecipient> recipientCaptor = ArgumentCaptor.forClass(SysNoticeRecipient.class);
        verify(recipientMapper, org.mockito.Mockito.times(3)).insert(recipientCaptor.capture());
        List<Long> recipientUserIds = recipientCaptor.getAllValues().stream()
                .map(SysNoticeRecipient::getUserId)
                .sorted()
                .toList();
        assertEquals(List.of(7L, 8L, 9L), recipientUserIds);
    }

    @Test
    void markReadUpdatesOnlyCurrentUsersRecipientRow() {
        SysNoticeRecipientMapper recipientMapper = mock(SysNoticeRecipientMapper.class);
        NoticeService service = new NoticeService(
                mock(SysNoticeMapper.class),
                mock(SysNoticeTargetMapper.class),
                recipientMapper,
                mock(JdbcTemplate.class),
                mock(RealtimeMessageService.class)
        );

        service.markRead(12L, 34L);

        verify(recipientMapper).markRead(12L, 34L);
    }

    private SysNotice notice(Long id) {
        SysNotice notice = new SysNotice();
        notice.setId(id);
        notice.setTitle("测试公告");
        notice.setNoticeType("公告");
        notice.setPriority("普通");
        notice.setStatus(0);
        return notice;
    }

    private SysNoticeTarget target(String targetType, Long targetId, String effect) {
        SysNoticeTarget target = new SysNoticeTarget();
        target.setTargetType(targetType);
        target.setTargetId(targetId);
        target.setEffect(effect);
        return target;
    }
}
