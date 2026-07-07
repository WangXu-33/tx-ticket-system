package com.rbac.base.modules.ticket.dto;

import lombok.Data;

/**
 * 修改时间：2026-07-02
 * 功能说明：系统配置保存入参，承载工单所属系统的基础信息。
 * 入参：系统主键、编码、名称、负责人组、默认优先级、说明和状态。
 * 出参：无。
 * 异常场景：编码或名称缺失时由服务层拒绝保存。
 */
@Data
public class TicketSystemSaveDTO {
    private Long id;
    private String code;
    private String name;
    private String ownerGroup;
    private String defaultPriority;
    private String remark;
    private Integer status;
}
