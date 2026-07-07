package com.rbac.base.modules.ticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rbac.base.core.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 代码生成/修改时间：2026-07-04。
 * 工单主表实体，保存工单当前状态、客户信息、当前处理人、最终解决方案、评价和 SLA 留痕字段。
 * 入参：由 Controller/Service 填充字段后持久化。
 * 出参：用于工单列表、详情和统计展示。
 * 异常场景：状态流转必须配合 TicketFlow 留痕，禁止只更新主表状态。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tx_ticket")
public class Ticket extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String ticketNo;
    private String title;
    private String description;
    private String category;
    private String systemCode;
    private String priority;
    private String status;
    private Long creatorId;
    private String creatorName;
    private String creatorPhone;
    private String creatorEmail;
    private Long handlerId;
    private String handlerName;
    private String solution;
    private Long mergeParentId;
    private String mergeReason;
    private Integer reopenCount;
    private Integer evaluationScore;
    private String evaluationContent;
    private LocalDateTime resolvedTime;
    private LocalDateTime closedTime;
    private LocalDateTime evaluationTime;
    private LocalDateTime slaWarnTime;
}
