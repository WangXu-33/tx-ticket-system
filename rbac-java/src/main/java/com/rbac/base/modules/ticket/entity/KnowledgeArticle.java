package com.rbac.base.modules.ticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rbac.base.core.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 代码生成/修改时间：2026-07-01。
 * 知识库文章实体，结构化保存问题现象、原因分析和解决步骤。
 * 入参：专家或主管维护的知识内容。
 * 出参：用于知识库检索、工单引用和后续 Agent 推荐。
 * 异常场景：未审核内容保持 draft 状态，不参与客户侧推荐。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tx_knowledge_article")
public class KnowledgeArticle extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String category;
    private String tags;
    private String phenomenon;
    private String causeAnalysis;
    private String solutionSteps;
    private String applicableScope;
    private String status;
    private Long sourceTicketId;
    private Integer usefulCount;
    private Integer uselessCount;
    private LocalDateTime publishTime;
}
