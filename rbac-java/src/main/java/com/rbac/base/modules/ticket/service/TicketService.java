package com.rbac.base.modules.ticket.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rbac.base.modules.rbac.entity.SysUser;
import com.rbac.base.modules.rbac.mapper.SysUserMapper;
import com.rbac.base.modules.ticket.dto.TicketAssignDTO;
import com.rbac.base.modules.ticket.dto.TicketCreateDTO;
import com.rbac.base.modules.ticket.dto.TicketReplyDTO;
import com.rbac.base.modules.ticket.dto.TicketResolveDTO;
import com.rbac.base.modules.ticket.entity.Ticket;
import com.rbac.base.modules.ticket.entity.TicketAttachment;
import com.rbac.base.modules.ticket.entity.TicketFlow;
import com.rbac.base.modules.ticket.mapper.TicketAttachmentMapper;
import com.rbac.base.modules.ticket.mapper.TicketFlowMapper;
import com.rbac.base.modules.ticket.mapper.TicketMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 代码生成/修改时间：2026-07-01。
 * 工单业务服务，封装工单创建、查询、回复、分派、转派、解决和关闭。
 * 入参：Controller 传入的 DTO 和筛选条件。
 * 出参：工单分页、详情或动作结果。
 * 异常场景：参数缺失、工单不存在、处理人不存在时抛出 IllegalArgumentException 并由全局异常处理返回。
 */
@Service
public class TicketService {
    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_PROCESSING = "processing";
    private static final String STATUS_WAITING_CUSTOMER = "waiting_customer";
    private static final String STATUS_TRANSFERRED = "transferred";
    private static final String STATUS_RESOLVED = "resolved";
    private static final String STATUS_CLOSED = "closed";
    private static final String STATUS_REJECTED = "rejected";
    private static final String SCOPE_PUBLIC = "public";
    private static final String SCOPE_INTERNAL = "internal";

    private final TicketMapper ticketMapper;
    private final TicketFlowMapper ticketFlowMapper;
    private final TicketAttachmentMapper ticketAttachmentMapper;
    private final SysUserMapper userMapper;

    public TicketService(TicketMapper ticketMapper,
                         TicketFlowMapper ticketFlowMapper,
                         TicketAttachmentMapper ticketAttachmentMapper,
                         SysUserMapper userMapper) {
        this.ticketMapper = ticketMapper;
        this.ticketFlowMapper = ticketFlowMapper;
        this.ticketAttachmentMapper = ticketAttachmentMapper;
        this.userMapper = userMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(TicketCreateDTO dto) {
        validateText(dto.getTitle(), "工单标题不能为空");
        validateText(dto.getDescription(), "问题描述不能为空");

        SysUser creator = currentUser();
        Ticket ticket = new Ticket();
        ticket.setTicketNo(generateTicketNo());
        ticket.setTitle(dto.getTitle().trim());
        ticket.setDescription(dto.getDescription().trim());
        ticket.setCategory(defaultText(dto.getCategory(), "general"));
        ticket.setPriority(defaultText(dto.getPriority(), "normal"));
        ticket.setStatus(STATUS_PENDING);
        ticket.setCreatorId(creator.getId());
        ticket.setCreatorName(displayName(creator));
        ticket.setCreatorPhone(defaultText(dto.getContactPhone(), creator.getPhone()));
        ticket.setCreatorEmail(defaultText(dto.getContactEmail(), creator.getEmail()));
        ticketMapper.insert(ticket);

        TicketFlow flow = insertFlow(ticket, "created", null, STATUS_PENDING, null, null,
                "客户提交工单", SCOPE_PUBLIC);
        linkFiles(ticket.getId(), flow.getId(), null, dto.getFileIds(), SCOPE_PUBLIC, "ticket");
        return ticket.getId();
    }

    public Page<Ticket> page(Integer pageNum, Integer pageSize, String keyword, String status,
                             String category, String priority, String owner) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        boolean customerOnly = isCustomerOnly();
        Page<Ticket> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like(Ticket::getTicketNo, keyword)
                    .or()
                    .like(Ticket::getTitle, keyword)
                    .or()
                    .like(Ticket::getDescription, keyword));
        }
        wrapper.eq(StringUtils.hasText(status), Ticket::getStatus, status)
                .eq(StringUtils.hasText(category), Ticket::getCategory, category)
                .eq(StringUtils.hasText(priority), Ticket::getPriority, priority);

        if (customerOnly || "customer".equals(owner)) {
            wrapper.eq(Ticket::getCreatorId, currentUserId);
        } else if ("my".equals(owner)) {
            wrapper.eq(Ticket::getHandlerId, currentUserId);
        }

        wrapper.orderByDesc(Ticket::getId);
        return ticketMapper.selectPage(page, wrapper);
    }

    public Map<String, Object> detail(Long id) {
        Ticket ticket = requireTicket(id);
        assertTicketVisible(ticket);
        Map<String, Object> data = new HashMap<>();
        data.put("ticket", ticket);
        data.put("flows", ticketFlowMapper.selectList(new LambdaQueryWrapper<TicketFlow>()
                .eq(TicketFlow::getTicketId, id)
                .orderByAsc(TicketFlow::getId)));
        data.put("attachments", ticketAttachmentMapper.selectList(new LambdaQueryWrapper<TicketAttachment>()
                .eq(TicketAttachment::getTicketId, id)
                .orderByAsc(TicketAttachment::getId)));
        return data;
    }

    @Transactional(rollbackFor = Exception.class)
    public void receive(Long id) {
        Ticket ticket = requireTicket(id);
        SysUser operator = currentUser();
        String fromStatus = ticket.getStatus();
        ticket.setStatus(STATUS_PROCESSING);
        ticket.setHandlerId(operator.getId());
        ticket.setHandlerName(displayName(operator));
        ticketMapper.updateById(ticket);
        insertFlow(ticket, "received", fromStatus, STATUS_PROCESSING, null, operator.getId(),
                "处理人接单", SCOPE_INTERNAL);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reply(TicketReplyDTO dto) {
        validateText(dto.getContent(), "回复内容不能为空");
        Ticket ticket = requireTicket(dto.getTicketId());
        assertTicketVisible(ticket);
        String fromStatus = ticket.getStatus();
        String targetStatus = StringUtils.hasText(dto.getTargetStatus()) ? dto.getTargetStatus() : fromStatus;
        if (!fromStatus.equals(targetStatus)) {
            ticket.setStatus(targetStatus);
            ticketMapper.updateById(ticket);
        }
        String scope = StringUtils.hasText(dto.getVisibleScope()) ? dto.getVisibleScope() : SCOPE_PUBLIC;
        TicketFlow flow = insertFlow(ticket, "replied", fromStatus, targetStatus, ticket.getHandlerId(),
                ticket.getHandlerId(), dto.getContent(), scope);
        linkFiles(ticket.getId(), flow.getId(), null, dto.getFileIds(), scope, "flow");
    }

    @Transactional(rollbackFor = Exception.class)
    public void assign(TicketAssignDTO dto) {
        changeHandler(dto, "assigned", STATUS_PROCESSING);
    }

    @Transactional(rollbackFor = Exception.class)
    public void transfer(TicketAssignDTO dto) {
        changeHandler(dto, "transferred", STATUS_TRANSFERRED);
    }

    @Transactional(rollbackFor = Exception.class)
    public void resolve(TicketResolveDTO dto) {
        validateText(dto.getSolution(), "解决方案不能为空");
        Ticket ticket = requireTicket(dto.getTicketId());
        String fromStatus = ticket.getStatus();
        ticket.setStatus(STATUS_RESOLVED);
        ticket.setSolution(dto.getSolution().trim());
        ticket.setResolvedTime(LocalDateTime.now());
        ticketMapper.updateById(ticket);
        TicketFlow flow = insertFlow(ticket, "resolved", fromStatus, STATUS_RESOLVED, ticket.getHandlerId(),
                ticket.getHandlerId(), dto.getSolution(), SCOPE_PUBLIC);
        linkFiles(ticket.getId(), flow.getId(), null, dto.getFileIds(), SCOPE_PUBLIC, "flow");
    }

    @Transactional(rollbackFor = Exception.class)
    public void close(Long id) {
        Ticket ticket = requireTicket(id);
        assertTicketVisible(ticket);
        String fromStatus = ticket.getStatus();
        ticket.setStatus(STATUS_CLOSED);
        ticket.setClosedTime(LocalDateTime.now());
        ticketMapper.updateById(ticket);
        insertFlow(ticket, "closed", fromStatus, STATUS_CLOSED, ticket.getHandlerId(), ticket.getHandlerId(),
                "工单关闭", SCOPE_PUBLIC);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reject(TicketReplyDTO dto) {
        validateText(dto.getContent(), "驳回原因不能为空");
        Ticket ticket = requireTicket(dto.getTicketId());
        String fromStatus = ticket.getStatus();
        ticket.setStatus(STATUS_REJECTED);
        ticketMapper.updateById(ticket);
        insertFlow(ticket, "rejected", fromStatus, STATUS_REJECTED, ticket.getHandlerId(), ticket.getHandlerId(),
                dto.getContent(), SCOPE_PUBLIC);
    }

    private void changeHandler(TicketAssignDTO dto, String action, String targetStatus) {
        if (dto.getHandlerId() == null) {
            throw new IllegalArgumentException("处理人不能为空");
        }
        Ticket ticket = requireTicket(dto.getTicketId());
        SysUser handler = userMapper.selectById(dto.getHandlerId());
        if (handler == null) {
            throw new IllegalArgumentException("处理人不存在");
        }
        String fromStatus = ticket.getStatus();
        Long fromHandlerId = ticket.getHandlerId();
        ticket.setStatus(targetStatus);
        ticket.setHandlerId(handler.getId());
        ticket.setHandlerName(displayName(handler));
        ticketMapper.updateById(ticket);
        insertFlow(ticket, action, fromStatus, targetStatus, fromHandlerId, handler.getId(),
                defaultText(dto.getReason(), "工单处理人变更"), SCOPE_INTERNAL);
    }

    private TicketFlow insertFlow(Ticket ticket, String action, String fromStatus, String toStatus,
                                  Long fromHandlerId, Long toHandlerId, String content, String visibleScope) {
        SysUser operator = currentUser();
        TicketFlow flow = new TicketFlow();
        flow.setTicketId(ticket.getId());
        flow.setAction(action);
        flow.setFromStatus(fromStatus);
        flow.setToStatus(toStatus);
        flow.setOperatorId(operator.getId());
        flow.setOperatorName(displayName(operator));
        flow.setFromHandlerId(fromHandlerId);
        flow.setToHandlerId(toHandlerId);
        flow.setContent(content);
        flow.setVisibleScope(StringUtils.hasText(visibleScope) ? visibleScope : SCOPE_PUBLIC);
        ticketFlowMapper.insert(flow);
        return flow;
    }

    private void linkFiles(Long ticketId, Long flowId, Long articleId, List<Long> fileIds, String visibleScope, String businessType) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        Long userId = StpUtil.getLoginIdAsLong();
        for (Long fileId : fileIds) {
            if (fileId == null) {
                continue;
            }
            TicketAttachment attachment = new TicketAttachment();
            attachment.setBusinessType(businessType);
            attachment.setTicketId(ticketId);
            attachment.setFlowId(flowId);
            attachment.setArticleId(articleId);
            attachment.setFileId(fileId);
            attachment.setVisibleScope(StringUtils.hasText(visibleScope) ? visibleScope : SCOPE_PUBLIC);
            attachment.setUploadUserId(userId);
            ticketAttachmentMapper.insert(attachment);
        }
    }

    private Ticket requireTicket(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("工单不能为空");
        }
        Ticket ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw new IllegalArgumentException("工单不存在");
        }
        return ticket;
    }

    private SysUser currentUser() {
        SysUser user = userMapper.selectById(StpUtil.getLoginIdAsLong());
        if (user == null) {
            throw new IllegalArgumentException("当前用户不存在");
        }
        return user;
    }

    private void assertTicketVisible(Ticket ticket) {
        if (isCustomerOnly() && !StpUtil.getLoginIdAsLong().equals(ticket.getCreatorId())) {
            throw new IllegalArgumentException("无权访问该工单");
        }
    }

    private boolean isCustomerOnly() {
        List<String> roles = StpUtil.getRoleList();
        return roles.contains("customer") && !roles.contains("admin") && !roles.contains("support")
                && !roles.contains("operator") && !roles.contains("expert") && !roles.contains("supervisor");
    }

    private String generateTicketNo() {
        return "TX" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private String displayName(SysUser user) {
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private void validateText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
    }
}
