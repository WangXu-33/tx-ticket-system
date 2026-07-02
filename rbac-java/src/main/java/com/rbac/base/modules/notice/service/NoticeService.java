package com.rbac.base.modules.notice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rbac.base.modules.notice.dto.NoticeSaveDTO;
import com.rbac.base.modules.notice.dto.NoticeTargetDTO;
import com.rbac.base.modules.notice.entity.SysNotice;
import com.rbac.base.modules.notice.entity.SysNoticeRecipient;
import com.rbac.base.modules.notice.entity.SysNoticeTarget;
import com.rbac.base.modules.notice.mapper.SysNoticeMapper;
import com.rbac.base.modules.notice.mapper.SysNoticeRecipientMapper;
import com.rbac.base.modules.notice.mapper.SysNoticeTargetMapper;
import com.rbac.base.modules.notice.vo.NoticeListVO;
import com.rbac.base.modules.notice.vo.NoticeMyVO;
import com.rbac.base.modules.notice.vo.NoticeStatsVO;
import com.rbac.base.modules.realtime.service.RealtimeMessageService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class NoticeService {

    public static final String TARGET_ALL = "ALL";
    public static final String TARGET_DEPT = "DEPT";
    public static final String TARGET_ROLE = "ROLE";
    public static final String TARGET_USER = "USER";
    public static final String EFFECT_INCLUDE = "INCLUDE";
    public static final String EFFECT_EXCLUDE = "EXCLUDE";
    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_PUBLISHED = 1;
    public static final int STATUS_WITHDRAWN = 2;
    public static final String SQL_ENABLED_USER_IDS = "SELECT id FROM sys_user WHERE del_flag = 0 AND status = 1";
    public static final String SQL_DEPT_USER_IDS = "SELECT u.id FROM sys_user u LEFT JOIN sys_dept d ON d.id = u.dept_id WHERE u.del_flag = 0 AND u.status = 1 AND (u.dept_id = ? OR d.dept_code LIKE CONCAT((SELECT dept_code FROM sys_dept WHERE id = ?), '%'))";
    public static final String SQL_ROLE_USER_IDS = "SELECT DISTINCT user_id FROM sys_user_role WHERE role_id = ?";

    private final SysNoticeMapper noticeMapper;
    private final SysNoticeTargetMapper targetMapper;
    private final SysNoticeRecipientMapper recipientMapper;
    private final JdbcTemplate jdbcTemplate;
    private final RealtimeMessageService realtimeMessageService;

    public NoticeService(SysNoticeMapper noticeMapper,
                         SysNoticeTargetMapper targetMapper,
                         SysNoticeRecipientMapper recipientMapper,
                         JdbcTemplate jdbcTemplate,
                         RealtimeMessageService realtimeMessageService) {
        this.noticeMapper = noticeMapper;
        this.targetMapper = targetMapper;
        this.recipientMapper = recipientMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.realtimeMessageService = realtimeMessageService;
    }

    public List<NoticeListVO> listManage(String keyword, Integer status) {
        return noticeMapper.selectManageList(trimToNull(keyword), status);
    }

    public List<NoticeMyVO> listMy(Long userId, Integer readFlag, String keyword) {
        return noticeMapper.selectMyList(userId, readFlag, trimToNull(keyword));
    }

    public NoticeStatsVO getStats(Long noticeId) {
        NoticeStatsVO stats = noticeMapper.selectStats(noticeId);
        if (stats == null) {
            stats = new NoticeStatsVO();
            stats.setNoticeId(noticeId);
            stats.setRecipientTotal(0L);
            stats.setReadTotal(0L);
            stats.setUnreadTotal(0L);
        }
        return stats;
    }

    public Long countUnread(Long userId) {
        Long count = recipientMapper.countUnreadByUserId(userId);
        return count == null ? 0L : count;
    }

    public SysNotice detail(Long id) {
        return noticeMapper.selectById(id);
    }

    public List<SysNoticeTarget> targets(Long noticeId) {
        return targetMapper.selectList(new LambdaQueryWrapper<SysNoticeTarget>()
                .eq(SysNoticeTarget::getNoticeId, noticeId)
                .orderByAsc(SysNoticeTarget::getId));
    }

    @Transactional
    public Long saveDraft(NoticeSaveDTO dto) {
        validateNotice(dto);
        SysNotice notice = dto.getId() == null ? new SysNotice() : noticeMapper.selectById(dto.getId());
        if (notice == null) {
            throw new IllegalArgumentException("公告不存在");
        }
        if (notice.getStatus() != null && notice.getStatus() == STATUS_PUBLISHED) {
            throw new IllegalArgumentException("已发布公告不能直接编辑");
        }
        notice.setTitle(dto.getTitle().trim());
        notice.setContent(dto.getContent().trim());
        notice.setNoticeType(StringUtils.hasText(dto.getNoticeType()) ? dto.getNoticeType().trim() : "公告");
        notice.setPriority(StringUtils.hasText(dto.getPriority()) ? dto.getPriority().trim() : "普通");
        notice.setRemark(dto.getRemark());
        if (notice.getStatus() == null) {
            notice.setStatus(STATUS_DRAFT);
        }
        if (dto.getId() == null) {
            noticeMapper.insert(notice);
        } else {
            noticeMapper.updateById(notice);
        }
        saveTargets(notice.getId(), dto.getTargets());
        return notice.getId();
    }

    @Transactional
    public void publish(Long noticeId) {
        SysNotice notice = noticeMapper.selectById(noticeId);
        if (notice == null) {
            throw new IllegalArgumentException("公告不存在");
        }
        List<SysNoticeTarget> targets = targets(noticeId);
        Set<Long> recipientUserIds = resolveRecipients(targets);
        recipientMapper.deleteByNoticeId(noticeId);
        LocalDateTime now = LocalDateTime.now();
        for (Long userId : recipientUserIds) {
            SysNoticeRecipient recipient = new SysNoticeRecipient();
            recipient.setNoticeId(noticeId);
            recipient.setUserId(userId);
            recipient.setReadFlag(0);
            recipient.setReceiveTime(now);
            recipientMapper.insert(recipient);
        }
        notice.setStatus(STATUS_PUBLISHED);
        notice.setPublishTime(now);
        noticeMapper.updateById(notice);
        Map<String, Object> payload = Map.of(
                "noticeId", notice.getId(),
                "title", notice.getTitle(),
                "noticeType", notice.getNoticeType(),
                "priority", notice.getPriority(),
                "publishTime", now.toString()
        );
        recipientUserIds.forEach(userId -> realtimeMessageService.sendUser(userId, "notice-new", payload));
    }

    @Transactional
    public void withdraw(Long noticeId) {
        SysNotice notice = noticeMapper.selectById(noticeId);
        if (notice == null) {
            throw new IllegalArgumentException("公告不存在");
        }
        notice.setStatus(STATUS_WITHDRAWN);
        noticeMapper.updateById(notice);
    }

    @Transactional
    public void delete(Long noticeId) {
        recipientMapper.deleteByNoticeId(noticeId);
        targetMapper.deleteByNoticeId(noticeId);
        noticeMapper.deleteById(noticeId);
    }

    public void markRead(Long noticeId, Long userId) {
        recipientMapper.markRead(noticeId, userId);
    }

    private void saveTargets(Long noticeId, List<NoticeTargetDTO> targets) {
        targetMapper.deleteByNoticeId(noticeId);
        if (targets == null || targets.isEmpty()) {
            SysNoticeTarget target = new SysNoticeTarget();
            target.setNoticeId(noticeId);
            target.setTargetType(TARGET_ALL);
            target.setEffect(EFFECT_INCLUDE);
            targetMapper.insert(target);
            return;
        }
        for (NoticeTargetDTO dto : targets) {
            SysNoticeTarget target = new SysNoticeTarget();
            target.setNoticeId(noticeId);
            target.setTargetType(dto.getTargetType());
            target.setTargetId(dto.getTargetId());
            target.setEffect(dto.getEffect());
            targetMapper.insert(target);
        }
    }

    private Set<Long> resolveRecipients(List<SysNoticeTarget> targets) {
        Set<Long> includes = new LinkedHashSet<>();
        Set<Long> excludes = new LinkedHashSet<>();
        for (SysNoticeTarget target : targets) {
            Set<Long> userIds = resolveTargetUsers(target);
            if (EFFECT_EXCLUDE.equalsIgnoreCase(target.getEffect())) {
                excludes.addAll(userIds);
            } else {
                includes.addAll(userIds);
            }
        }
        includes.removeAll(excludes);
        return includes;
    }

    private Set<Long> resolveTargetUsers(SysNoticeTarget target) {
        List<Long> userIds = switch (target.getTargetType()) {
            case TARGET_ALL -> jdbcTemplate.queryForList(SQL_ENABLED_USER_IDS, Long.class);
            case TARGET_DEPT -> jdbcTemplate.queryForList(SQL_DEPT_USER_IDS, Long.class, target.getTargetId(), target.getTargetId());
            case TARGET_ROLE -> jdbcTemplate.queryForList(SQL_ROLE_USER_IDS, Long.class, target.getTargetId());
            case TARGET_USER -> target.getTargetId() == null ? List.of() : List.of(target.getTargetId());
            default -> List.of();
        };
        return new LinkedHashSet<>(userIds);
    }

    private void validateNotice(NoticeSaveDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getTitle()) || !StringUtils.hasText(dto.getContent())) {
            throw new IllegalArgumentException("公告标题和正文不能为空");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
