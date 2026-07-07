package com.rbac.base.modules.ticket.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rbac.base.modules.rbac.entity.SysUser;
import com.rbac.base.modules.rbac.mapper.SysUserMapper;
import com.rbac.base.modules.system.entity.SysDictData;
import com.rbac.base.modules.system.entity.SysFile;
import com.rbac.base.modules.system.mapper.SysDictDataMapper;
import com.rbac.base.modules.system.mapper.SysFileMapper;
import com.rbac.base.modules.ticket.dto.TicketAssignDTO;
import com.rbac.base.modules.ticket.dto.TicketActionDTO;
import com.rbac.base.modules.ticket.dto.TicketCreateDTO;
import com.rbac.base.modules.ticket.dto.TicketMergeDTO;
import com.rbac.base.modules.ticket.dto.TicketReplyDTO;
import com.rbac.base.modules.ticket.dto.TicketResolveDTO;
import com.rbac.base.modules.ticket.dto.TicketSystemSaveDTO;
import com.rbac.base.modules.ticket.entity.KnowledgeTicketLink;
import com.rbac.base.modules.ticket.entity.Ticket;
import com.rbac.base.modules.ticket.entity.TicketAttachment;
import com.rbac.base.modules.ticket.entity.TicketFlow;
import com.rbac.base.modules.ticket.mapper.KnowledgeTicketLinkMapper;
import com.rbac.base.modules.ticket.mapper.TicketAttachmentMapper;
import com.rbac.base.modules.ticket.mapper.TicketFlowMapper;
import com.rbac.base.modules.ticket.mapper.TicketMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 修改时间：2026-07-02
 * 功能说明：工单业务服务，负责工单草稿、提单、接单、分派、转派、处理留痕、知识沉淀前置元数据等能力。
 * 入参：Controller 传入的分页条件、工单 DTO、流程 DTO 与系统配置 DTO。
 * 出参：工单分页、详情、元数据、系统配置列表与保存结果。
 * 异常场景：工单状态不允许流转、当前用户无处理权限、草稿归属不正确或字典配置缺失时抛出 IllegalArgumentException。
 */
@Service
public class TicketService {
    private static final String STATUS_DRAFT = "draft";
    private static final String STATUS_PENDING_APPROVAL = "pending_approval";
    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_PROCESSING = "processing";
    private static final String STATUS_WAITING_CUSTOMER = "waiting_customer";
    private static final String STATUS_TRANSFERRED = "transferred";
    private static final String STATUS_RESOLVED = "resolved";
    private static final String STATUS_CLOSED = "closed";
    private static final String STATUS_REJECTED = "rejected";
    private static final String STATUS_CANCELLED = "cancelled";

    private static final String ACTION_CREATED = "created";
    private static final String ACTION_APPROVED = "approved";
    private static final String ACTION_RETURNED = "returned";
    private static final String ACTION_RECEIVED = "received";
    private static final String ACTION_REPLIED = "replied";
    private static final String ACTION_ASSIGNED = "assigned";
    private static final String ACTION_TRANSFERRED = "transferred";
    private static final String ACTION_RESOLVED = "resolved";
    private static final String ACTION_CLOSED = "closed";
    private static final String ACTION_REJECTED = "rejected";
    private static final String ACTION_MERGED = "merged";
    private static final String ACTION_REOPENED = "reopened";
    private static final String ACTION_EVALUATED = "evaluated";
    private static final String ACTION_SLA_WARNED = "sla_warned";
    private static final String ACTION_CANCELLED = "cancelled";

    private static final String SCOPE_PUBLIC = "public";
    private static final String SCOPE_INTERNAL = "internal";
    private static final String BUSINESS_TYPE_TICKET = "ticket";
    private static final String BUSINESS_TYPE_FLOW = "flow";

    private static final String DICT_TICKET_STATUS = "tx_ticket_status";
    private static final String DICT_TICKET_PRIORITY = "tx_ticket_priority";
    private static final String DICT_TICKET_CATEGORY = "tx_ticket_category";
    private static final String DICT_TICKET_ACTION = "tx_ticket_action";
    private static final String DICT_TICKET_SYSTEM = "tx_ticket_system";

    private static final Set<String> MANAGER_ROLES = Set.of("admin", "system_admin", "supervisor");
    private static final Set<String> HANDLER_ROLES = Set.of("admin", "system_admin", "supervisor", "operator", "support", "expert");

    private final TicketMapper ticketMapper;
    private final TicketFlowMapper ticketFlowMapper;
    private final TicketAttachmentMapper ticketAttachmentMapper;
    private final SysUserMapper userMapper;
    private final SysFileMapper sysFileMapper;
    private final SysDictDataMapper sysDictDataMapper;
    private final KnowledgeTicketLinkMapper knowledgeTicketLinkMapper;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public TicketService(TicketMapper ticketMapper,
                         TicketFlowMapper ticketFlowMapper,
                         TicketAttachmentMapper ticketAttachmentMapper,
                         SysUserMapper userMapper,
                         SysFileMapper sysFileMapper,
                         SysDictDataMapper sysDictDataMapper,
                         KnowledgeTicketLinkMapper knowledgeTicketLinkMapper,
                         JdbcTemplate jdbcTemplate,
                         ObjectMapper objectMapper) {
        this.ticketMapper = ticketMapper;
        this.ticketFlowMapper = ticketFlowMapper;
        this.ticketAttachmentMapper = ticketAttachmentMapper;
        this.userMapper = userMapper;
        this.sysFileMapper = sysFileMapper;
        this.sysDictDataMapper = sysDictDataMapper;
        this.knowledgeTicketLinkMapper = knowledgeTicketLinkMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 修改时间：2026-07-02
     * 功能说明：创建工单或保存草稿；草稿允许反复编辑，提交时自动转为待审批并补写创建留痕。
     * 入参：TicketCreateDTO，支持草稿标识与草稿主键。
     * 出参：当前工单主键。
     * 异常场景：草稿不属于当前用户、非草稿工单被二次编辑、必填字段缺失时抛出 IllegalArgumentException。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(TicketCreateDTO dto) {
        dto = requirePayload(dto, "工单内容不能为空");
        SysUser creator = currentUser();
        boolean draftMode = Boolean.TRUE.equals(dto.getDraft());
        Ticket ticket = prepareDraftTicket(dto, creator);
        boolean newTicket = ticket.getId() == null;
        boolean draftBeforeSubmit = !newTicket && STATUS_DRAFT.equals(ticket.getStatus());

        if (!draftMode) {
            validateText(dto.getTitle(), "工单标题不能为空");
            validateText(dto.getDescription(), "问题描述不能为空");
            validateText(dto.getSystemCode(), "所属系统不能为空");
        }

        applyTicketForm(ticket, dto, creator, draftMode);

        if (newTicket) {
            ticketMapper.insert(ticket);
        } else {
            ticketMapper.updateById(ticket);
        }

        if (draftMode || draftBeforeSubmit) {
            syncDraftAttachments(ticket.getId(), dto.getFileIds(), true);
        }
        if (!draftMode && newTicket) {
            TicketFlow flow = insertFlow(ticket, ACTION_CREATED, null, STATUS_PENDING_APPROVAL, null, null, "客户提交工单，等待审批人受理审核", SCOPE_PUBLIC);
            linkFiles(ticket.getId(), flow.getId(), null, dto.getFileIds(), SCOPE_PUBLIC, BUSINESS_TYPE_FLOW);
        } else if (!draftMode && draftBeforeSubmit) {
            TicketFlow flow = insertFlow(ticket, ACTION_CREATED, STATUS_DRAFT, STATUS_PENDING_APPROVAL, null, null, "客户提交工单，等待审批人受理审核", SCOPE_PUBLIC);
            bindDraftAttachmentsToFlow(ticket.getId(), flow.getId());
        }
        return ticket.getId();
    }

    /**
     * 修改时间：2026-07-02
     * 功能说明：分页查询工单，支持结构化筛选、状态集合筛选与客户/处理人视角隔离。
     * 入参：分页参数、结构化查询字段、状态集合与 owner 视角标记。
     * 出参：工单分页结果。
     * 异常场景：无。
     */
    public Page<Ticket> page(Integer pageNum, Integer pageSize, String keyword, String ticketNo,
                             String title, String creatorName, String handlerName, String status,
                             String statusList, String category, String systemCode, String priority, String owner) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        boolean customerOnly = isCustomerOnly();
        Page<Ticket> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();

        String normalizedTicketNo = trimToNull(ticketNo);
        String normalizedTitle = trimToNull(title);
        String normalizedCreatorName = trimToNull(creatorName);
        String normalizedHandlerName = trimToNull(handlerName);
        String normalizedKeyword = trimToNull(keyword);
        List<String> statusValues = splitCsv(statusList);

        wrapper.eq(normalizedTicketNo != null, Ticket::getTicketNo, normalizedTicketNo)
                .likeRight(normalizedTitle != null, Ticket::getTitle, normalizedTitle)
                .likeRight(normalizedCreatorName != null, Ticket::getCreatorName, normalizedCreatorName)
                .likeRight(normalizedHandlerName != null, Ticket::getHandlerName, normalizedHandlerName)
                .likeRight(normalizedKeyword != null
                                && normalizedTicketNo == null
                                && normalizedTitle == null
                                && normalizedCreatorName == null
                                && normalizedHandlerName == null,
                        Ticket::getTitle, normalizedKeyword)
                .eq(StringUtils.hasText(status), Ticket::getStatus, status)
                .in(!statusValues.isEmpty(), Ticket::getStatus, statusValues)
                .eq(StringUtils.hasText(category), Ticket::getCategory, category)
                .eq(StringUtils.hasText(systemCode), Ticket::getSystemCode, systemCode)
                .eq(StringUtils.hasText(priority), Ticket::getPriority, priority);

        if (customerOnly || "customer".equals(owner)) {
            wrapper.eq(Ticket::getCreatorId, currentUserId);
        } else if ("my".equals(owner)) {
            wrapper.eq(Ticket::getHandlerId, currentUserId);
        } else {
            wrapper.ne(Ticket::getStatus, STATUS_DRAFT);
        }

        wrapper.orderByDesc(Ticket::getId);
        return ticketMapper.selectPage(page, wrapper);
    }

    /**
     * 修改时间：2026-07-02
     * 功能说明：返回工单相关字典，前端页面统一从后端取状态、优先级、分类、动作与系统配置。
     * 入参：无。
     * 出参：包含 statusOptions、priorityOptions、categoryOptions、actionOptions、systemOptions 的元数据。
     * 异常场景：字典为空时返回空数组，不抛出异常。
     */
    public Map<String, Object> meta() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("statusOptions", listOptionsByType(DICT_TICKET_STATUS, false));
        data.put("priorityOptions", listOptionsByType(DICT_TICKET_PRIORITY, false));
        data.put("categoryOptions", listOptionsByType(DICT_TICKET_CATEGORY, false));
        data.put("actionOptions", listOptionsByType(DICT_TICKET_ACTION, false));
        data.put("systemOptions", listOptionsByType(DICT_TICKET_SYSTEM, true));
        return data;
    }

    /**
     * 代码生成/修改时间：2026-07-05。
     * 功能说明：按当前用户可见范围统计工单首页指标，并返回已发布知识数量。
     * 入参：无。
     * 出参：total、pendingApproval、pending、processing、waitingCustomer、transferred、resolved、closed、rejected、cancelled、active、knowledge。
     * 异常场景：知识表不存在或查询失败时由全局异常处理返回错误，便于部署阶段及时发现脚本缺失。
     */
    public Map<String, Object> statistics() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", countTicketsByStatuses(null));
        data.put("pendingApproval", countTicketsByStatuses(List.of(STATUS_PENDING_APPROVAL)));
        data.put("pending", countTicketsByStatuses(List.of(STATUS_PENDING)));
        data.put("processing", countTicketsByStatuses(List.of(STATUS_PROCESSING)));
        data.put("waitingCustomer", countTicketsByStatuses(List.of(STATUS_WAITING_CUSTOMER)));
        data.put("transferred", countTicketsByStatuses(List.of(STATUS_TRANSFERRED)));
        data.put("resolved", countTicketsByStatuses(List.of(STATUS_RESOLVED)));
        data.put("closed", countTicketsByStatuses(List.of(STATUS_CLOSED)));
        data.put("rejected", countTicketsByStatuses(List.of(STATUS_REJECTED)));
        data.put("cancelled", countTicketsByStatuses(List.of(STATUS_CANCELLED)));
        data.put("active", countTicketsByStatuses(List.of(STATUS_PENDING_APPROVAL, STATUS_PENDING, STATUS_PROCESSING, STATUS_WAITING_CUSTOMER, STATUS_TRANSFERRED)));
        data.put("slaWarned", countTicketsWithSlaWarning());
        data.put("knowledge", StpUtil.hasPermission("knowledge:list") ? countPublishedKnowledge() : 0L);
        return data;
    }

    /**
     * 修改时间：2026-07-02
     * 功能说明：查询工单详情与流程留痕，客户仅可查看对外可见内容。
     * 入参：工单主键。
     * 出参：包含 ticket、flows、attachments、files 的详情结构。
     * 异常场景：工单不存在或当前用户无权查看时抛出 IllegalArgumentException。
     */
    public Map<String, Object> detail(Long id) {
        Ticket ticket = requireTicket(id);
        assertTicketReadable(ticket);
        boolean customerOnly = isCustomerOnly();
        Map<String, Object> data = new HashMap<>();
        LambdaQueryWrapper<TicketFlow> flowWrapper = new LambdaQueryWrapper<TicketFlow>()
                .eq(TicketFlow::getTicketId, id)
                .orderByAsc(TicketFlow::getId);
        LambdaQueryWrapper<TicketAttachment> attachmentWrapper = new LambdaQueryWrapper<TicketAttachment>()
                .eq(TicketAttachment::getTicketId, id)
                .orderByAsc(TicketAttachment::getId);

        if (customerOnly) {
            flowWrapper.eq(TicketFlow::getVisibleScope, SCOPE_PUBLIC);
            attachmentWrapper.eq(TicketAttachment::getVisibleScope, SCOPE_PUBLIC);
        }

        List<TicketAttachment> attachments = ticketAttachmentMapper.selectList(attachmentWrapper);
        data.put("ticket", ticket);
        data.put("flows", ticketFlowMapper.selectList(flowWrapper));
        data.put("attachments", attachments);
        data.put("files", listTicketFiles(attachments));
        return data;
    }

    /**
     * 修改时间：2026-07-02
     * 功能说明：查询可分派或转派的处理人候选列表。
     * 入参：keyword，可按账号或昵称模糊匹配。
     * 出参：处理人候选列表。
     * 异常场景：无。
     */
    public List<Map<String, Object>> handlerOptions(String keyword) {
        String likeKeyword = "%" + defaultText(keyword, "") + "%";
        return jdbcTemplate.queryForList("""
                SELECT DISTINCT
                    u.id,
                    u.username,
                    u.nickname,
                    r.role_name AS roleName,
                    r.role_key AS roleKey
                FROM sys_user u
                JOIN sys_user_role ur ON u.id = ur.user_id
                JOIN sys_role r ON ur.role_id = r.id
                WHERE u.status = 1
                  AND u.del_flag = 0
                  AND r.del_flag = 0
                  AND r.status = 1
                  AND r.role_key IN ('admin','system_admin','operator','support','expert','supervisor')
                  AND (? = '%%' OR u.username LIKE ? OR u.nickname LIKE ?)
                ORDER BY u.id ASC
                """, likeKeyword, likeKeyword, likeKeyword);
    }

    /**
     * 修改时间：2026-07-02
     * 功能说明：查询系统配置列表，用于工单系统归属配置与人员维护。
     * 入参：keyword、status。
     * 出参：系统配置列表。
     * 异常场景：remark 不是合法 JSON 时自动忽略扩展字段。
     */
    public List<Map<String, Object>> systemList(String keyword, Integer status) {
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getTypeCode, DICT_TICKET_SYSTEM)
                .likeRight(StringUtils.hasText(keyword), SysDictData::getLabel, keyword == null ? null : keyword.trim())
                .eq(status != null, SysDictData::getStatus, status)
                .orderByAsc(SysDictData::getSort)
                .orderByAsc(SysDictData::getId);
        return sysDictDataMapper.selectList(wrapper).stream()
                .map(this::buildSystemRecord)
                .toList();
    }

    /**
     * 修改时间：2026-07-02
     * 功能说明：保存系统配置，供提单页选择所属系统与后续派单策略使用。
     * 入参：TicketSystemSaveDTO。
     * 出参：系统配置主键。
     * 异常场景：系统编码或名称缺失时抛出 IllegalArgumentException。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long saveSystem(TicketSystemSaveDTO dto) {
        dto = requirePayload(dto, "系统配置不能为空");
        assertManagerRole("维护系统配置");
        validateText(dto.getCode(), "系统编码不能为空");
        validateText(dto.getName(), "系统名称不能为空");

        SysDictData system = dto.getId() == null ? findSystemByCode(dto.getCode()) : sysDictDataMapper.selectById(dto.getId());
        if (system == null) {
            system = new SysDictData();
            system.setTypeCode(DICT_TICKET_SYSTEM);
            system.setParentId(0L);
            system.setSort(nextSystemSort());
        }

        system.setLabel(dto.getName().trim());
        system.setValue(dto.getCode().trim());
        system.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        system.setRemark(writeSystemRemark(dto));

        if (system.getId() == null) {
            sysDictDataMapper.insert(system);
        } else {
            sysDictDataMapper.updateById(system);
        }
        return system.getId();
    }

    /**
     * 修改时间：2026-07-02
     * 功能说明：启停单个系统配置。
     * 入参：系统配置主键。
     * 出参：无。
     * 异常场景：系统配置不存在时抛出 IllegalArgumentException。
     */
    @Transactional(rollbackFor = Exception.class)
    public void toggleSystem(Long id) {
        assertManagerRole("启停系统配置");
        SysDictData system = sysDictDataMapper.selectById(id);
        if (system == null || !DICT_TICKET_SYSTEM.equals(system.getTypeCode())) {
            throw new IllegalArgumentException("系统配置不存在");
        }
        system.setStatus(Objects.equals(system.getStatus(), 1) ? 0 : 1);
        sysDictDataMapper.updateById(system);
    }

    /**
     * 修改时间：2026-07-02
     * 功能说明：接单，接单后当前登录人变为第一任处理人。
     * 入参：工单主键。
     * 出参：无。
     * 异常场景：工单不是待受理、当前用户不具备处理角色时抛出 IllegalArgumentException。
     */
    @Transactional(rollbackFor = Exception.class)
    public void receive(Long id) {
        Ticket ticket = requireTicket(id);
        assertWorkerRole("接单");
        assertStatusIn(ticket, "接单", STATUS_PENDING);
        SysUser operator = currentUser();
        String fromStatus = ticket.getStatus();
        ticket.setStatus(STATUS_PROCESSING);
        ticket.setHandlerId(operator.getId());
        ticket.setHandlerName(displayName(operator));
        ticketMapper.updateById(ticket);
        insertFlow(ticket, ACTION_RECEIVED, fromStatus, STATUS_PROCESSING, null, operator.getId(), "处理人接单", SCOPE_INTERNAL);
    }

    /**
     * 修改时间：2026-07-02
     * 功能说明：保存处理记录或客户补充信息。
     * 入参：TicketReplyDTO。
     * 出参：无。
     * 异常场景：工单已结束、当前用户不是客户本人也不是当前处理人时抛出 IllegalArgumentException。
     */
    @Transactional(rollbackFor = Exception.class)
    public void reply(TicketReplyDTO dto) {
        dto = requirePayload(dto, "回复内容不能为空");
        validateText(dto.getContent(), "回复内容不能为空");
        Ticket ticket = requireTicket(dto.getTicketId());
        assertNotTerminal(ticket, "回复");
        assertReplyPermission(ticket);

        String fromStatus = ticket.getStatus();
        String targetStatus = resolveReplyTargetStatus(dto, fromStatus);
        if (!Objects.equals(fromStatus, targetStatus)) {
            ticket.setStatus(targetStatus);
            ticketMapper.updateById(ticket);
        }

        String scope = resolveReplyScope(dto);
        TicketFlow flow = insertFlow(ticket, ACTION_REPLIED, fromStatus, targetStatus, ticket.getHandlerId(), ticket.getHandlerId(), dto.getContent().trim(), scope);
        linkFiles(ticket.getId(), flow.getId(), null, dto.getFileIds(), scope, BUSINESS_TYPE_FLOW);
    }

    /**
     * 修改时间：2026-07-04
     * 功能说明：审批通过工单，待审批工单通过后进入待受理大厅。
     * 入参：TicketActionDTO，包含工单 ID 和审批说明。
     * 出参：无。
     * 异常场景：非管理角色或工单状态不允许审批时抛出 IllegalArgumentException。
     */
    @Transactional(rollbackFor = Exception.class)
    public void approve(TicketActionDTO dto) {
        dto = requirePayload(dto, "审批参数不能为空");
        Ticket ticket = requireTicket(dto.getTicketId());
        assertManagerRole("审批通过");
        assertStatusIn(ticket, "审批通过", STATUS_PENDING_APPROVAL, STATUS_WAITING_CUSTOMER);
        String fromStatus = ticket.getStatus();
        ticket.setStatus(STATUS_PENDING);
        ticketMapper.updateById(ticket);
        insertFlow(ticket, ACTION_APPROVED, fromStatus, STATUS_PENDING, ticket.getHandlerId(), ticket.getHandlerId(),
                defaultText(dto.getContent(), "审批通过，进入待受理大厅"), SCOPE_PUBLIC);
    }

    /**
     * 修改时间：2026-07-04
     * 功能说明：审批退回补充，要求客户补齐材料后再次进入审批判断。
     * 入参：TicketActionDTO，包含退回原因。
     * 出参：无。
     * 异常场景：退回说明为空或状态不允许退回时抛出 IllegalArgumentException。
     */
    @Transactional(rollbackFor = Exception.class)
    public void returnForSupplement(TicketActionDTO dto) {
        dto = requirePayload(dto, "退回补充参数不能为空");
        validateText(dto.getContent(), "退回补充原因不能为空");
        Ticket ticket = requireTicket(dto.getTicketId());
        assertManagerRole("退回补充");
        assertStatusIn(ticket, "退回补充", STATUS_PENDING_APPROVAL, STATUS_PENDING);
        String fromStatus = ticket.getStatus();
        ticket.setStatus(STATUS_WAITING_CUSTOMER);
        ticketMapper.updateById(ticket);
        insertFlow(ticket, ACTION_RETURNED, fromStatus, STATUS_WAITING_CUSTOMER, ticket.getHandlerId(), ticket.getHandlerId(),
                dto.getContent().trim(), SCOPE_PUBLIC);
    }

    /**
     * 修改时间：2026-07-04
     * 功能说明：撤销尚未进入处理环节的工单，保留流程留痕，不执行物理删除。
     * 入参：TicketActionDTO，包含撤销原因。
     * 出参：无。
     * 异常场景：当前用户不是创建人或管理角色、工单已被处理时抛出 IllegalArgumentException。
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(TicketActionDTO dto) {
        dto = requirePayload(dto, "撤销参数不能为空");
        Ticket ticket = requireTicket(dto.getTicketId());
        assertCreatorOrManager(ticket, "撤销");
        assertStatusIn(ticket, "撤销", STATUS_DRAFT, STATUS_PENDING_APPROVAL, STATUS_PENDING, STATUS_WAITING_CUSTOMER);
        if (ticket.getHandlerId() != null && !STATUS_DRAFT.equals(ticket.getStatus())) {
            throw new IllegalArgumentException("已进入处理环节的工单不能撤销，请走关闭或驳回流程");
        }
        String fromStatus = ticket.getStatus();
        ticket.setStatus(STATUS_CANCELLED);
        ticketMapper.updateById(ticket);
        insertFlow(ticket, ACTION_CANCELLED, fromStatus, STATUS_CANCELLED, ticket.getHandlerId(), ticket.getHandlerId(),
                defaultText(dto.getContent(), "客户撤销工单"), SCOPE_PUBLIC);
    }

    /**
     * 修改时间：2026-07-04
     * 功能说明：客户或主管对未认可的解决结果发起重开，工单回到处理中。
     * 入参：TicketActionDTO，包含重开原因。
     * 出参：无。
     * 异常场景：重开原因为空、工单状态不允许重开或当前用户无权操作时抛出 IllegalArgumentException。
     */
    @Transactional(rollbackFor = Exception.class)
    public void reopen(TicketActionDTO dto) {
        dto = requirePayload(dto, "重开参数不能为空");
        validateText(dto.getContent(), "重开原因不能为空");
        Ticket ticket = requireTicket(dto.getTicketId());
        assertCreatorOrManager(ticket, "重开");
        assertStatusIn(ticket, "重开", STATUS_RESOLVED, STATUS_CLOSED);
        String fromStatus = ticket.getStatus();
        ticket.setStatus(STATUS_PROCESSING);
        ticket.setReopenCount(ticket.getReopenCount() == null ? 1 : ticket.getReopenCount() + 1);
        ticketMapper.updateById(ticket);
        insertFlow(ticket, ACTION_REOPENED, fromStatus, STATUS_PROCESSING, ticket.getHandlerId(), ticket.getHandlerId(),
                dto.getContent().trim(), SCOPE_PUBLIC);
    }

    /**
     * 修改时间：2026-07-04
     * 功能说明：客户在工单关闭后评价服务质量。
     * 入参：TicketActionDTO，包含 1-5 分评分和评价内容。
     * 出参：无。
     * 异常场景：评分越界、工单未关闭或非创建人评价时抛出 IllegalArgumentException。
     */
    @Transactional(rollbackFor = Exception.class)
    public void evaluate(TicketActionDTO dto) {
        dto = requirePayload(dto, "评价参数不能为空");
        Ticket ticket = requireTicket(dto.getTicketId());
        assertCreator(ticket, "评价");
        assertStatusIn(ticket, "评价", STATUS_CLOSED);
        Integer rating = dto.getRating();
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("评价分值必须在 1 到 5 之间");
        }
        ticket.setEvaluationScore(rating);
        ticket.setEvaluationContent(trimToNull(dto.getContent()));
        ticket.setEvaluationTime(LocalDateTime.now());
        ticketMapper.updateById(ticket);
        String content = "客户评价：" + rating + "分" + (StringUtils.hasText(dto.getContent()) ? "，" + dto.getContent().trim() : "");
        insertFlow(ticket, ACTION_EVALUATED, STATUS_CLOSED, STATUS_CLOSED, ticket.getHandlerId(), ticket.getHandlerId(), content, SCOPE_PUBLIC);
    }

    /**
     * 修改时间：2026-07-04
     * 功能说明：主管手工触发 SLA 提醒或升级留痕。
     * 入参：TicketActionDTO，包含 SLA 类型和提醒说明。
     * 出参：无。
     * 异常场景：非管理角色或终态工单触发提醒时抛出 IllegalArgumentException。
     */
    @Transactional(rollbackFor = Exception.class)
    public void warnSla(TicketActionDTO dto) {
        dto = requirePayload(dto, "SLA 提醒参数不能为空");
        Ticket ticket = requireTicket(dto.getTicketId());
        assertManagerRole("SLA 提醒");
        assertStatusIn(ticket, "SLA 提醒", STATUS_PENDING_APPROVAL, STATUS_PENDING, STATUS_PROCESSING,
                STATUS_WAITING_CUSTOMER, STATUS_TRANSFERRED, STATUS_RESOLVED);
        ticket.setSlaWarnTime(LocalDateTime.now());
        ticketMapper.updateById(ticket);
        String slaType = defaultText(dto.getSlaType(), "manual");
        String content = "SLA 提醒[" + slaType + "]：" + defaultText(dto.getContent(), "请关注响应或解决时限");
        insertFlow(ticket, ACTION_SLA_WARNED, ticket.getStatus(), ticket.getStatus(), ticket.getHandlerId(), ticket.getHandlerId(),
                content, SCOPE_INTERNAL);
    }

    /**
     * 修改时间：2026-07-02
     * 功能说明：分派工单，仅允许管理角色执行。
     * 入参：TicketAssignDTO。
     * 出参：无。
     * 异常场景：当前用户不是管理角色、工单状态不允许分派或目标处理人无效时抛出 IllegalArgumentException。
     */
    @Transactional(rollbackFor = Exception.class)
    public void assign(TicketAssignDTO dto) {
        dto = requirePayload(dto, "分派参数不能为空");
        assertManagerRole("分派");
        changeHandler(dto, ACTION_ASSIGNED, STATUS_PROCESSING, true);
    }

    /**
     * 修改时间：2026-07-02
     * 功能说明：转派工单，仅允许当前处理人或管理角色执行。
     * 入参：TicketAssignDTO。
     * 出参：无。
     * 异常场景：当前用户不是当前处理人、目标处理人无效或状态不允许转派时抛出 IllegalArgumentException。
     */
    @Transactional(rollbackFor = Exception.class)
    public void transfer(TicketAssignDTO dto) {
        dto = requirePayload(dto, "转派参数不能为空");
        changeHandler(dto, ACTION_TRANSFERRED, STATUS_TRANSFERRED, false);
    }

    /**
     * 修改时间：2026-07-02
     * 功能说明：标记工单为已解决。
     * 入参：TicketResolveDTO。
     * 出参：无。
     * 异常场景：解决方案为空、当前用户不是当前处理人或工单状态不允许解决时抛出 IllegalArgumentException。
     */
    @Transactional(rollbackFor = Exception.class)
    public void resolve(TicketResolveDTO dto) {
        dto = requirePayload(dto, "解决参数不能为空");
        validateText(dto.getSolution(), "解决方案不能为空");
        Ticket ticket = requireTicket(dto.getTicketId());
        assertHandlerOrManager(ticket, "解决");
        assertStatusIn(ticket, "解决", STATUS_PROCESSING, STATUS_WAITING_CUSTOMER, STATUS_TRANSFERRED);

        String fromStatus = ticket.getStatus();
        ticket.setStatus(STATUS_RESOLVED);
        ticket.setSolution(dto.getSolution().trim());
        ticket.setResolvedTime(LocalDateTime.now());
        ticketMapper.updateById(ticket);
        TicketFlow flow = insertFlow(ticket, ACTION_RESOLVED, fromStatus, STATUS_RESOLVED, ticket.getHandlerId(), ticket.getHandlerId(), dto.getSolution().trim(), SCOPE_PUBLIC);
        linkFiles(ticket.getId(), flow.getId(), null, dto.getFileIds(), SCOPE_PUBLIC, BUSINESS_TYPE_FLOW);
    }

    /**
     * 修改时间：2026-07-02
     * 功能说明：关闭工单，客户只能关闭自己的已解决工单，运维侧遵循原权限入口。
     * 入参：工单主键。
     * 出参：无。
     * 异常场景：工单非已解决状态、客户尝试关闭他人工单时抛出 IllegalArgumentException。
     */
    @Transactional(rollbackFor = Exception.class)
    public void close(Long id) {
        Ticket ticket = requireTicket(id);
        assertTicketReadable(ticket);
        assertClosePermission(ticket);
        assertStatusIn(ticket, "关闭", STATUS_RESOLVED);
        String fromStatus = ticket.getStatus();
        ticket.setStatus(STATUS_CLOSED);
        ticket.setClosedTime(LocalDateTime.now());
        ticketMapper.updateById(ticket);
        insertFlow(ticket, ACTION_CLOSED, fromStatus, STATUS_CLOSED, ticket.getHandlerId(), ticket.getHandlerId(), "工单关闭", SCOPE_PUBLIC);
    }

    /**
     * 修改时间：2026-07-02
     * 功能说明：驳回工单。
     * 入参：TicketReplyDTO。
     * 出参：无。
     * 异常场景：当前用户不是当前处理人或管理角色、驳回原因为空时抛出 IllegalArgumentException。
     */
    @Transactional(rollbackFor = Exception.class)
    public void reject(TicketReplyDTO dto) {
        dto = requirePayload(dto, "驳回参数不能为空");
        validateText(dto.getContent(), "驳回原因不能为空");
        Ticket ticket = requireTicket(dto.getTicketId());
        assertHandlerOrManager(ticket, "驳回");
        assertStatusIn(ticket, "驳回", STATUS_PENDING_APPROVAL, STATUS_PENDING, STATUS_PROCESSING, STATUS_WAITING_CUSTOMER, STATUS_TRANSFERRED);
        String fromStatus = ticket.getStatus();
        ticket.setStatus(STATUS_REJECTED);
        ticketMapper.updateById(ticket);
        insertFlow(ticket, ACTION_REJECTED, fromStatus, STATUS_REJECTED, ticket.getHandlerId(), ticket.getHandlerId(), dto.getContent().trim(), SCOPE_PUBLIC);
    }

    /**
     * 修改时间：2026-07-03
     * 功能说明：将重复工单合并到主工单，被合并工单关闭并保留流程留痕，主工单继续作为处理入口。
     * 入参：TicketMergeDTO，包含主工单、重复工单、合并原因和可选知识库文章。
     * 出参：无。
     * 异常场景：当前用户不是管理角色、工单不存在、合并自身或状态不允许合并时抛出 IllegalArgumentException。
     */
    @Transactional(rollbackFor = Exception.class)
    public void merge(TicketMergeDTO dto) {
        assertManagerRole("合并");
        dto = requirePayload(dto, "合并参数不能为空");
        if (dto.getMainTicketId() == null) {
            throw new IllegalArgumentException("主工单不能为空");
        }
        List<Long> duplicateTicketIds = dto.getDuplicateTicketIds() == null ? List.of() : dto.getDuplicateTicketIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (duplicateTicketIds.isEmpty()) {
            throw new IllegalArgumentException("请选择需要合并的重复工单");
        }

        Ticket mainTicket = requireTicket(dto.getMainTicketId());
        assertStatusIn(mainTicket, "作为主工单合并", STATUS_PENDING_APPROVAL, STATUS_PENDING, STATUS_PROCESSING, STATUS_WAITING_CUSTOMER, STATUS_TRANSFERRED, STATUS_RESOLVED);
        String reason = defaultText(dto.getReason(), "重复问题合并");

        for (Long duplicateTicketId : duplicateTicketIds) {
            if (Objects.equals(mainTicket.getId(), duplicateTicketId)) {
                throw new IllegalArgumentException("主工单不能合并自身");
            }
            Ticket duplicateTicket = requireTicket(duplicateTicketId);
            assertStatusIn(duplicateTicket, "作为重复工单合并", STATUS_PENDING_APPROVAL, STATUS_PENDING, STATUS_PROCESSING, STATUS_WAITING_CUSTOMER, STATUS_TRANSFERRED, STATUS_RESOLVED);
            if (duplicateTicket.getMergeParentId() != null) {
                throw new IllegalArgumentException("工单 " + duplicateTicket.getTicketNo() + " 已经合并过");
            }

            String fromStatus = duplicateTicket.getStatus();
            duplicateTicket.setStatus(STATUS_CLOSED);
            duplicateTicket.setClosedTime(LocalDateTime.now());
            duplicateTicket.setMergeParentId(mainTicket.getId());
            duplicateTicket.setMergeReason(reason);
            ticketMapper.updateById(duplicateTicket);

            String duplicateContent = "重复问题已合并到主工单 " + mainTicket.getTicketNo() + "：" + reason;
            insertFlow(duplicateTicket, ACTION_MERGED, fromStatus, STATUS_CLOSED, duplicateTicket.getHandlerId(), mainTicket.getHandlerId(), duplicateContent, SCOPE_PUBLIC);

            String mainContent = "合并重复工单 " + duplicateTicket.getTicketNo() + "：" + reason;
            insertFlow(mainTicket, ACTION_MERGED, mainTicket.getStatus(), mainTicket.getStatus(), duplicateTicket.getHandlerId(), mainTicket.getHandlerId(), mainContent, SCOPE_INTERNAL);

            linkKnowledgeTicket(dto.getArticleId(), mainTicket.getId(), "merged");
            linkKnowledgeTicket(dto.getArticleId(), duplicateTicket.getId(), "merged");
        }
    }

    private Ticket prepareDraftTicket(TicketCreateDTO dto, SysUser creator) {
        if (dto.getId() == null) {
            Ticket ticket = new Ticket();
            ticket.setTicketNo(generateTicketNo());
            ticket.setCreatorId(creator.getId());
            ticket.setCreatorName(displayName(creator));
            return ticket;
        }
        Ticket ticket = requireTicket(dto.getId());
        if (!Objects.equals(ticket.getCreatorId(), creator.getId())) {
            throw new IllegalArgumentException("只能编辑自己的草稿");
        }
        if (!STATUS_DRAFT.equals(ticket.getStatus())) {
            throw new IllegalArgumentException("当前工单不是草稿，不能继续编辑");
        }
        return ticket;
    }

    private void applyTicketForm(Ticket ticket, TicketCreateDTO dto, SysUser creator, boolean draftMode) {
        String title = trimToNull(dto.getTitle());
        String description = trimToNull(dto.getDescription());
        ticket.setTitle(title == null ? "" : title);
        ticket.setDescription(description == null ? "" : description);
        ticket.setCategory(defaultText(dto.getCategory(), "general"));
        ticket.setSystemCode(defaultText(dto.getSystemCode(), ""));
        ticket.setPriority(defaultText(dto.getPriority(), "normal"));
        ticket.setStatus(draftMode ? STATUS_DRAFT : STATUS_PENDING_APPROVAL);
        ticket.setCreatorId(creator.getId());
        ticket.setCreatorName(displayName(creator));
        ticket.setCreatorPhone(defaultText(dto.getContactPhone(), creator.getPhone()));
        ticket.setCreatorEmail(defaultText(dto.getContactEmail(), creator.getEmail()));
        if (!draftMode && ticket.getTicketNo() == null) {
            ticket.setTicketNo(generateTicketNo());
        }
    }

    private void syncDraftAttachments(Long ticketId, List<Long> fileIds, boolean allowDraftAttachmentSync) {
        if (!allowDraftAttachmentSync || ticketId == null) {
            return;
        }
        List<Long> desiredFileIds = fileIds == null ? List.of() : fileIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        LambdaQueryWrapper<TicketAttachment> wrapper = new LambdaQueryWrapper<TicketAttachment>()
                .eq(TicketAttachment::getTicketId, ticketId)
                .eq(TicketAttachment::getBusinessType, BUSINESS_TYPE_TICKET)
                .isNull(TicketAttachment::getFlowId)
                .isNull(TicketAttachment::getArticleId);
        List<TicketAttachment> existingAttachments = ticketAttachmentMapper.selectList(wrapper);
        Set<Long> existingFileIds = existingAttachments.stream()
                .map(TicketAttachment::getFileId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (TicketAttachment attachment : existingAttachments) {
            if (!desiredFileIds.contains(attachment.getFileId())) {
                attachment.setDelFlag(1);
                ticketAttachmentMapper.updateById(attachment);
            }
        }

        List<Long> appendFileIds = desiredFileIds.stream()
                .filter(fileId -> !existingFileIds.contains(fileId))
                .toList();
        linkFiles(ticketId, null, null, appendFileIds, SCOPE_PUBLIC, BUSINESS_TYPE_TICKET);
    }

    private void bindDraftAttachmentsToFlow(Long ticketId, Long flowId) {
        LambdaQueryWrapper<TicketAttachment> wrapper = new LambdaQueryWrapper<TicketAttachment>()
                .eq(TicketAttachment::getTicketId, ticketId)
                .eq(TicketAttachment::getBusinessType, BUSINESS_TYPE_TICKET)
                .isNull(TicketAttachment::getFlowId)
                .isNull(TicketAttachment::getArticleId);
        List<TicketAttachment> attachments = ticketAttachmentMapper.selectList(wrapper);
        for (TicketAttachment attachment : attachments) {
            attachment.setFlowId(flowId);
            attachment.setBusinessType(BUSINESS_TYPE_FLOW);
            ticketAttachmentMapper.updateById(attachment);
        }
    }

    private List<Map<String, Object>> listOptionsByType(String typeCode, boolean enabledOnly) {
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getTypeCode, typeCode)
                .orderByAsc(SysDictData::getSort)
                .orderByAsc(SysDictData::getId);
        if (enabledOnly) {
            wrapper.eq(SysDictData::getStatus, 1);
        }
        return sysDictDataMapper.selectList(wrapper).stream()
                .map(item -> buildOption(typeCode, item))
                .toList();
    }

    private Map<String, Object> buildOption(String typeCode, SysDictData item) {
        Map<String, Object> option = new LinkedHashMap<>();
        option.put("id", item.getId());
        option.put("label", item.getLabel());
        option.put("value", item.getValue());
        option.put("status", item.getStatus());
        option.put("sort", item.getSort());
        option.put("color", resolveOptionColor(typeCode, item.getValue()));
        if (DICT_TICKET_SYSTEM.equals(typeCode)) {
            option.putAll(buildSystemRecord(item));
        }
        return option;
    }

    private Long countTicketsByStatuses(List<String> statuses) {
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();
        applyReadableTicketScope(wrapper);
        if (statuses != null && !statuses.isEmpty()) {
            wrapper.in(Ticket::getStatus, statuses);
        }
        return ticketMapper.selectCount(wrapper);
    }

    private Long countTicketsWithSlaWarning() {
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();
        applyReadableTicketScope(wrapper);
        wrapper.isNotNull(Ticket::getSlaWarnTime);
        return ticketMapper.selectCount(wrapper);
    }

    private Long countPublishedKnowledge() {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM tx_knowledge_article
                WHERE status = 'published'
                """, Long.class);
        return count == null ? 0L : count;
    }

    private void applyReadableTicketScope(LambdaQueryWrapper<Ticket> wrapper) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (isCustomerOnly()) {
            wrapper.eq(Ticket::getCreatorId, currentUserId);
        } else {
            wrapper.ne(Ticket::getStatus, STATUS_DRAFT);
        }
    }

    private Map<String, Object> buildSystemRecord(SysDictData item) {
        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Object> remarkMap = parseRemark(item.getRemark());
        data.put("id", item.getId());
        data.put("code", item.getValue());
        data.put("name", item.getLabel());
        data.put("ownerGroup", defaultText((String) remarkMap.get("ownerGroup"), ""));
        data.put("defaultPriority", defaultText((String) remarkMap.get("defaultPriority"), "normal"));
        data.put("remark", defaultText((String) remarkMap.get("remark"), ""));
        data.put("status", item.getStatus());
        data.put("color", resolveOptionColor(DICT_TICKET_SYSTEM, item.getValue()));
        return data;
    }

    private Map<String, Object> parseRemark(String remark) {
        if (!StringUtils.hasText(remark)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(remark, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private String writeSystemRemark(TicketSystemSaveDTO dto) {
        Map<String, Object> remark = new LinkedHashMap<>();
        remark.put("ownerGroup", defaultText(dto.getOwnerGroup(), ""));
        remark.put("defaultPriority", defaultText(dto.getDefaultPriority(), "normal"));
        remark.put("remark", defaultText(dto.getRemark(), ""));
        try {
            return objectMapper.writeValueAsString(remark);
        } catch (Exception ignored) {
            return "{\"ownerGroup\":\"\",\"defaultPriority\":\"normal\",\"remark\":\"\"}";
        }
    }

    private String resolveOptionColor(String typeCode, String value) {
        if (DICT_TICKET_STATUS.equals(typeCode)) {
            return switch (defaultText(value, "")) {
                case STATUS_DRAFT -> "default";
                case STATUS_PENDING_APPROVAL -> "gold";
                case STATUS_PENDING -> "orange";
                case STATUS_PROCESSING -> "blue";
                case STATUS_WAITING_CUSTOMER -> "purple";
                case STATUS_TRANSFERRED -> "cyan";
                case STATUS_RESOLVED -> "green";
                case STATUS_CLOSED -> "default";
                case STATUS_REJECTED -> "red";
                case STATUS_CANCELLED -> "default";
                default -> "default";
            };
        }
        if (DICT_TICKET_PRIORITY.equals(typeCode)) {
            return switch (defaultText(value, "")) {
                case "low" -> "default";
                case "normal" -> "blue";
                case "high" -> "orange";
                case "urgent" -> "red";
                default -> "default";
            };
        }
        return "default";
    }

    private SysDictData findSystemByCode(String code) {
        return sysDictDataMapper.selectOne(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getTypeCode, DICT_TICKET_SYSTEM)
                .eq(SysDictData::getValue, trimToNull(code))
                .last("LIMIT 1"));
    }

    private Integer nextSystemSort() {
        List<SysDictData> systems = sysDictDataMapper.selectList(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getTypeCode, DICT_TICKET_SYSTEM)
                .orderByDesc(SysDictData::getSort)
                .last("LIMIT 1"));
        if (systems.isEmpty() || systems.get(0).getSort() == null) {
            return 1;
        }
        return systems.get(0).getSort() + 1;
    }

    private void changeHandler(TicketAssignDTO dto, String action, String targetStatus, boolean managerOnly) {
        if (dto.getHandlerId() == null) {
            throw new IllegalArgumentException("处理人不能为空");
        }
        Ticket ticket = requireTicket(dto.getTicketId());
        if (managerOnly) {
            assertManagerRole("分派");
        } else {
            assertHandlerOrManager(ticket, "转派");
        }
        assertHandlerChangeAllowed(ticket, action);

        SysUser handler = userMapper.selectById(dto.getHandlerId());
        if (handler == null) {
            throw new IllegalArgumentException("处理人不存在");
        }
        if (!isValidHandler(handler.getId())) {
            throw new IllegalArgumentException("处理人必须是启用状态的运维、专家、主管或管理员账号");
        }

        String fromStatus = ticket.getStatus();
        Long fromHandlerId = ticket.getHandlerId();
        ticket.setStatus(targetStatus);
        ticket.setHandlerId(handler.getId());
        ticket.setHandlerName(displayName(handler));
        ticketMapper.updateById(ticket);
        insertFlow(ticket, action, fromStatus, targetStatus, fromHandlerId, handler.getId(), defaultText(dto.getReason(), "工单处理人变更"), SCOPE_INTERNAL);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private List<String> splitCsv(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
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
            if (fileId == null || sysFileMapper.selectById(fileId) == null) {
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

    private void linkKnowledgeTicket(Long articleId, Long ticketId, String linkType) {
        if (articleId == null || ticketId == null) {
            return;
        }
        Long existing = knowledgeTicketLinkMapper.selectCount(new LambdaQueryWrapper<KnowledgeTicketLink>()
                .eq(KnowledgeTicketLink::getArticleId, articleId)
                .eq(KnowledgeTicketLink::getTicketId, ticketId)
                .eq(KnowledgeTicketLink::getLinkType, linkType));
        if (existing != null && existing > 0) {
            return;
        }
        KnowledgeTicketLink link = new KnowledgeTicketLink();
        link.setArticleId(articleId);
        link.setTicketId(ticketId);
        link.setLinkType(linkType);
        knowledgeTicketLinkMapper.insert(link);
    }

    private List<SysFile> listTicketFiles(List<TicketAttachment> attachments) {
        List<Long> fileIds = attachments.stream()
                .map(TicketAttachment::getFileId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (fileIds.isEmpty()) {
            return List.of();
        }
        return sysFileMapper.selectBatchIds(fileIds);
    }

    private String resolveReplyTargetStatus(TicketReplyDTO dto, String fromStatus) {
        if (isCustomerOnly()) {
            if (STATUS_WAITING_CUSTOMER.equals(fromStatus) && dto.getTicketId() != null) {
                Ticket ticket = ticketMapper.selectById(dto.getTicketId());
                return ticket != null && ticket.getHandlerId() == null ? STATUS_PENDING_APPROVAL : STATUS_PROCESSING;
            }
            return STATUS_WAITING_CUSTOMER.equals(fromStatus) ? STATUS_PROCESSING : fromStatus;
        }
        if (!StringUtils.hasText(dto.getTargetStatus())) {
            return fromStatus;
        }
        String targetStatus = dto.getTargetStatus().trim();
        boolean allowed = List.of(STATUS_PROCESSING, STATUS_WAITING_CUSTOMER).contains(targetStatus);
        if (!allowed) {
            throw new IllegalArgumentException("回复只能切换为处理中或待客户补充");
        }
        return targetStatus;
    }

    private String resolveReplyScope(TicketReplyDTO dto) {
        if (isCustomerOnly()) {
            return SCOPE_PUBLIC;
        }
        return SCOPE_INTERNAL.equals(dto.getVisibleScope()) ? SCOPE_INTERNAL : SCOPE_PUBLIC;
    }

    private void assertReplyPermission(Ticket ticket) {
        if (isCustomerOnly()) {
            assertTicketReadable(ticket);
            return;
        }
        assertHandlerOrManager(ticket, "回复");
    }

    private void assertHandlerChangeAllowed(Ticket ticket, String action) {
        if (ACTION_ASSIGNED.equals(action)) {
            assertStatusIn(ticket, "分派", STATUS_PENDING, STATUS_PROCESSING, STATUS_WAITING_CUSTOMER, STATUS_TRANSFERRED);
            return;
        }
        assertStatusIn(ticket, "转派", STATUS_PROCESSING, STATUS_WAITING_CUSTOMER, STATUS_TRANSFERRED);
    }

    private void assertStatusIn(Ticket ticket, String actionName, String... allowedStatuses) {
        boolean allowed = Arrays.asList(allowedStatuses).contains(ticket.getStatus());
        if (!allowed) {
            throw new IllegalArgumentException("当前状态不允许" + actionName);
        }
    }

    private void assertNotTerminal(Ticket ticket, String actionName) {
        if (STATUS_CLOSED.equals(ticket.getStatus()) || STATUS_REJECTED.equals(ticket.getStatus()) || STATUS_CANCELLED.equals(ticket.getStatus())) {
            throw new IllegalArgumentException("已结束工单不允许" + actionName);
        }
    }

    private void assertManagerRole(String actionName) {
        boolean matched = StpUtil.getRoleList().stream().anyMatch(MANAGER_ROLES::contains);
        if (!matched) {
            throw new IllegalArgumentException("当前账号没有" + actionName + "权限");
        }
    }

    private void assertWorkerRole(String actionName) {
        boolean matched = StpUtil.getRoleList().stream().anyMatch(HANDLER_ROLES::contains);
        if (!matched) {
            throw new IllegalArgumentException("当前账号没有" + actionName + "权限");
        }
    }

    private void assertHandlerOrManager(Ticket ticket, String actionName) {
        if (StpUtil.getRoleList().stream().anyMatch(MANAGER_ROLES::contains)) {
            return;
        }
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!Objects.equals(ticket.getHandlerId(), currentUserId)) {
            throw new IllegalArgumentException("只有当前处理人才能执行" + actionName);
        }
    }

    private void assertCreatorOrManager(Ticket ticket, String actionName) {
        if (StpUtil.getRoleList().stream().anyMatch(MANAGER_ROLES::contains)) {
            return;
        }
        assertCreator(ticket, actionName);
    }

    private void assertCreator(Ticket ticket, String actionName) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!Objects.equals(ticket.getCreatorId(), currentUserId)) {
            throw new IllegalArgumentException("只有工单创建人才能执行" + actionName);
        }
    }

    private void assertClosePermission(Ticket ticket) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        boolean allowed = Objects.equals(ticket.getCreatorId(), currentUserId)
                || Objects.equals(ticket.getHandlerId(), currentUserId)
                || StpUtil.getRoleList().stream().anyMatch(MANAGER_ROLES::contains);
        if (!allowed) {
            throw new IllegalArgumentException("只有工单创建人、当前处理人或主管才能关闭工单");
        }
    }

    private boolean isValidHandler(Long userId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM sys_user u
                JOIN sys_user_role ur ON u.id = ur.user_id
                JOIN sys_role r ON ur.role_id = r.id
                WHERE u.id = ?
                  AND u.status = 1
                  AND u.del_flag = 0
                  AND r.del_flag = 0
                  AND r.status = 1
                  AND r.role_key IN ('admin','system_admin','operator','support','expert','supervisor')
                """, Integer.class, userId);
        return count != null && count > 0;
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

    private void assertTicketReadable(Ticket ticket) {
        long currentUserId = StpUtil.getLoginIdAsLong();
        if (isCustomerOnly() && !Objects.equals(ticket.getCreatorId(), currentUserId)) {
            throw new IllegalArgumentException("无权访问该工单");
        }
        if (!isCustomerOnly() && STATUS_DRAFT.equals(ticket.getStatus())
                && !Objects.equals(ticket.getCreatorId(), currentUserId)
                && StpUtil.getRoleList().stream().noneMatch(MANAGER_ROLES::contains)) {
            throw new IllegalArgumentException("无权访问该草稿");
        }
    }

    private boolean isCustomerOnly() {
        List<String> roles = StpUtil.getRoleList();
        return roles.contains("customer")
                && roles.stream().noneMatch(HANDLER_ROLES::contains);
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

    private <T> T requirePayload(T payload, String message) {
        if (payload == null) {
            throw new IllegalArgumentException(message);
        }
        return payload;
    }
}
