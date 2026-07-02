package com.rbac.base.modules.ticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rbac.base.core.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 代码生成/修改时间：2026-07-01。
 * 知识库和工单关联实体，用于追溯知识来源和复用效果。
 * 入参：articleId、ticketId 和关联类型。
 * 出参：用于知识库详情和 Agent 推荐依据展示。
 * 异常场景：关联的工单或知识不存在时不应创建关联。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tx_knowledge_ticket_link")
public class KnowledgeTicketLink extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long articleId;
    private Long ticketId;
    private String linkType;
}
