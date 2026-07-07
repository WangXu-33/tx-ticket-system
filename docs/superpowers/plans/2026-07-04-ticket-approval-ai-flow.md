# Ticket Approval AI Flow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. The user has already approved inline execution.

**Goal:** 补齐工单审批、重开、评价、SLA 提醒、知识库审核与前端 AI 辅助体验。

**Architecture:** 后端沿用现有 `TicketService` 状态机和 `tx_ticket_flow` 留痕，不引入外部工作流依赖。前端沿用 Vue + Ant Design Vue 页面结构，在详情页、待接大厅、工作台、处理页、知识库编辑页做最小增量入口。

**Tech Stack:** Spring Boot、MyBatis Plus、Vue 3、Ant Design Vue、现有 RBAC 权限体系。

---

### Task 1: 后端状态与接口

**Files:**
- Modify: `rbac-java/src/main/java/com/rbac/base/modules/ticket/entity/Ticket.java`
- Modify: `rbac-java/src/main/java/com/rbac/base/modules/ticket/service/TicketService.java`
- Modify: `rbac-java/src/main/java/com/rbac/base/modules/ticket/controller/TicketController.java`
- Create: `rbac-java/src/main/java/com/rbac/base/modules/ticket/dto/TicketActionDTO.java`

**Steps:**
- 新增 `pending_approval`、`cancelled` 状态和 `approved`、`returned`、`reopened`、`evaluated`、`sla_warned`、`cancelled` 动作。
- 工单提交进入待审批；审批通过进入待受理；退回补充进入待客户补充；驳回进入终态。
- 支持客户撤销未接单工单、已解决工单重开、关闭后评价、SLA 手工提醒。
- 继续使用 `tx_ticket_flow` 记录动作，不物理删除业务数据。

### Task 2: 知识库审核

**Files:**
- Modify: `rbac-java/src/main/java/com/rbac/base/modules/ticket/service/KnowledgeService.java`
- Modify: `rbac-java/src/main/java/com/rbac/base/modules/ticket/controller/KnowledgeController.java`

**Steps:**
- 新增 `reviewing`、`rejected` 知识库状态。
- 保存草稿后可提交审核；主管/专家发布前先审核；驳回保留草稿内容和原因。

### Task 3: SQL 字典与字段

**Files:**
- Modify: `init.sql`
- Modify: `scripts/schema_upgrade_all.sql`

**Steps:**
- 增补工单状态、动作字典和知识库状态字典。
- 为 `tx_ticket` 增加评价、SLA、重开次数等字段，使用 `ALTER TABLE ... ADD COLUMN` 兼容升级。

### Task 4: 前端流程入口与样式

**Files:**
- Modify: `rbac-vue/src/api/ticket.js`
- Modify: `rbac-vue/src/constants/ticket.js`
- Modify: `rbac-vue/src/views/ticket/pending/index.vue`
- Modify: `rbac-vue/src/views/ticket/workbench/index.vue`
- Modify: `rbac-vue/src/views/ticket/detail/index.vue`
- Modify: `rbac-vue/src/views/ticket/process.vue`
- Modify: `rbac-vue/src/views/ticket/create.vue`
- Modify: `rbac-vue/src/views/ticket/my/index.vue`
- Modify: `rbac-vue/src/views/ticket/knowledge/detail.vue`
- Modify: `rbac-vue/src/views/ticket/knowledge/index.vue`

**Steps:**
- 待接大厅展示待审批和待受理，提供审批通过、退回补充、驳回、接单、分派入口。
- 详情页提供撤销、重开、评价、SLA 提醒、知识沉淀入口。
- 处理页增加 AI 排查助手提示卡和解决方案结构化提示，保持工作台风格克制、信息密度清晰。
- 知识库编辑页增加审核流按钮和状态提示。

### Task 5: 文档与验证

**Files:**
- Modify: `docs/ticket-workflow-design.md`
- Modify: `docs/diagrams/tx-ticket-workflow.drawio`

**Steps:**
- 更新流程图与说明，明确审批、重开、评价、SLA、知识审核。
- 运行后端编译和前端构建；不新增测试文件，遵守用户 AGENTS.md 测试约束。
