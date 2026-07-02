package com.rbac.base.modules.ticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rbac.base.core.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 代码生成/修改时间：2026-07-01。
 * 工单附件关联实体，复用 sys_file 文件资产并记录业务归属。
 * 入参：上传完成后的 fileId、业务类型和业务 ID。
 * 出参：用于工单详情、处理记录和知识库附件展示。
 * 异常场景：文件不存在时业务记录不应创建附件关联。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tx_ticket_attachment")
public class TicketAttachment extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String businessType;
    private Long ticketId;
    private Long flowId;
    private Long articleId;
    private Long fileId;
    private String visibleScope;
    private Long uploadUserId;
}
