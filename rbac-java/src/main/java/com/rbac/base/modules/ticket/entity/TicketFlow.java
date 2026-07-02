package com.rbac.base.modules.ticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rbac.base.core.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 代码生成/修改时间：2026-07-01。
 * 工单流程记录实体，保存每次创建、接单、回复、转派、解决、关闭等动作。
 * 入参：工单动作发生时由 TicketService 写入。
 * 出参：用于详情页时间线和审计追溯。
 * 异常场景：流程记录写入失败时应回滚对应工单状态变更。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tx_ticket_flow")
public class TicketFlow extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ticketId;
    private String action;
    private String fromStatus;
    private String toStatus;
    private Long operatorId;
    private String operatorName;
    private Long fromHandlerId;
    private Long toHandlerId;
    private String content;
    private String visibleScope;
}
