# 头绪工单系统业务模块 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有 RBAC 基座上新增头绪工单系统 MVP，包括客户注册、工单处理流转、附件留痕和知识库基础能力。

**Architecture:** 后端新增 `modules.ticket` 独立业务模块，复用 Sa-Token、MyBatis-Plus、`Result`、`BaseEntity`、`sys_file` 和现有权限体系。前端新增 `views/ticket` 页面并通过静态路由接入，初始化 SQL 新增业务表、菜单、权限、字典和默认角色授权。

**Tech Stack:** Spring Boot、Sa-Token、MyBatis-Plus、MySQL、Vue 3、Ant Design Vue、Axios。

---

## File Structure

后端新增文件：

- `rbac-java/src/main/java/com/rbac/base/modules/ticket/entity/Ticket.java`：工单主表实体。
- `rbac-java/src/main/java/com/rbac/base/modules/ticket/entity/TicketFlow.java`：工单流程记录实体。
- `rbac-java/src/main/java/com/rbac/base/modules/ticket/entity/TicketAttachment.java`：工单附件关联实体。
- `rbac-java/src/main/java/com/rbac/base/modules/ticket/entity/KnowledgeArticle.java`：知识库文章实体。
- `rbac-java/src/main/java/com/rbac/base/modules/ticket/entity/KnowledgeTicketLink.java`：知识库和工单关联实体。
- `rbac-java/src/main/java/com/rbac/base/modules/ticket/mapper/*.java`：MyBatis-Plus Mapper。
- `rbac-java/src/main/java/com/rbac/base/modules/ticket/dto/*.java`：创建、回复、分派、转派、解决、知识库保存 DTO。
- `rbac-java/src/main/java/com/rbac/base/modules/ticket/service/TicketService.java`：工单业务服务。
- `rbac-java/src/main/java/com/rbac/base/modules/ticket/service/KnowledgeService.java`：知识库业务服务。
- `rbac-java/src/main/java/com/rbac/base/modules/ticket/controller/TicketController.java`：工单接口。
- `rbac-java/src/main/java/com/rbac/base/modules/ticket/controller/KnowledgeController.java`：知识库接口。
- 修改 `rbac-java/src/main/java/com/rbac/base/modules/rbac/controller/AuthController.java`：新增客户注册接口。
- 修改 `rbac-java/src/main/java/com/rbac/base/core/config/SaTokenConfig.java`：放行客户注册接口。

前端新增和修改文件：

- `rbac-vue/src/views/ticket/workbench/index.vue`：工单工作台。
- `rbac-vue/src/views/ticket/my/index.vue`：我的工单。
- `rbac-vue/src/views/ticket/detail/index.vue`：工单详情和流程记录。
- `rbac-vue/src/views/ticket/knowledge/index.vue`：知识库列表。
- `rbac-vue/src/views/ticket/knowledge/detail.vue`：知识库编辑和详情。
- 修改 `rbac-vue/src/router/index.js`：新增工单和知识库路由。
- 修改 `rbac-vue/src/views/login/index.vue`：增加客户注册入口。

脚本和文档：

- 修改 `init.sql`：新增工单业务表、字典、菜单、权限和授权。
- 新增 `docs/ticket-business.md`：业务说明和操作流程。

## Task 1: Backend Domain Model

**Files:**
- Create: `rbac-java/src/main/java/com/rbac/base/modules/ticket/entity/Ticket.java`
- Create: `rbac-java/src/main/java/com/rbac/base/modules/ticket/entity/TicketFlow.java`
- Create: `rbac-java/src/main/java/com/rbac/base/modules/ticket/entity/TicketAttachment.java`
- Create: `rbac-java/src/main/java/com/rbac/base/modules/ticket/entity/KnowledgeArticle.java`
- Create: `rbac-java/src/main/java/com/rbac/base/modules/ticket/entity/KnowledgeTicketLink.java`
- Create: `rbac-java/src/main/java/com/rbac/base/modules/ticket/mapper/TicketMapper.java`
- Create: `rbac-java/src/main/java/com/rbac/base/modules/ticket/mapper/TicketFlowMapper.java`
- Create: `rbac-java/src/main/java/com/rbac/base/modules/ticket/mapper/TicketAttachmentMapper.java`
- Create: `rbac-java/src/main/java/com/rbac/base/modules/ticket/mapper/KnowledgeArticleMapper.java`
- Create: `rbac-java/src/main/java/com/rbac/base/modules/ticket/mapper/KnowledgeTicketLinkMapper.java`

- [ ] **Step 1: Create entity classes with UTF-8 comments**

Use `BaseEntity` for common audit fields and `@TableLogic` support. Status fields use string codes to keep dictionaries readable.

- [ ] **Step 2: Create mapper interfaces**

Each mapper extends `BaseMapper<Entity>` and contains no custom SQL unless needed by later tasks.

- [ ] **Step 3: Compile backend**

Run: `mvn -q -DskipTests compile` in `rbac-java`.

Expected: compile succeeds when a JDK is configured. If it fails with “No compiler is provided”, install or select JDK instead of JRE.

## Task 2: Backend Ticket Service

**Files:**
- Create: `rbac-java/src/main/java/com/rbac/base/modules/ticket/dto/TicketCreateDTO.java`
- Create: `rbac-java/src/main/java/com/rbac/base/modules/ticket/dto/TicketReplyDTO.java`
- Create: `rbac-java/src/main/java/com/rbac/base/modules/ticket/dto/TicketAssignDTO.java`
- Create: `rbac-java/src/main/java/com/rbac/base/modules/ticket/dto/TicketResolveDTO.java`
- Create: `rbac-java/src/main/java/com/rbac/base/modules/ticket/service/TicketService.java`
- Create: `rbac-java/src/main/java/com/rbac/base/modules/ticket/controller/TicketController.java`

- [ ] **Step 1: Implement create ticket**

Validate title and description, generate ticket number with `TXyyyyMMddHHmmssSSS`, insert main row, insert `created` flow, link uploaded file IDs.

- [ ] **Step 2: Implement ticket page query**

Support filters: keyword, status, category, priority, owner type `my/customer/all`, pageNum, pageSize. Customer role can only view created tickets.

- [ ] **Step 3: Implement detail query**

Return ticket, flow records, attachment associations and file IDs. Do not use physical delete logic.

- [ ] **Step 4: Implement process actions**

Support receive, reply, assign, transfer, resolve, close and reject. Every action must insert `TicketFlow` before or with the status update in one transaction.

- [ ] **Step 5: Compile backend**

Run: `mvn -q -DskipTests compile` in `rbac-java`.

## Task 3: Backend Knowledge Service

**Files:**
- Create: `rbac-java/src/main/java/com/rbac/base/modules/ticket/dto/KnowledgeSaveDTO.java`
- Create: `rbac-java/src/main/java/com/rbac/base/modules/ticket/service/KnowledgeService.java`
- Create: `rbac-java/src/main/java/com/rbac/base/modules/ticket/controller/KnowledgeController.java`

- [ ] **Step 1: Implement knowledge CRUD**

Support list, detail, add, edit, publish, withdraw. Use logical deletion only.

- [ ] **Step 2: Implement generate draft from ticket**

Use resolved ticket title, description and solution to create article draft and link it to the source ticket.

- [ ] **Step 3: Compile backend**

Run: `mvn -q -DskipTests compile` in `rbac-java`.

## Task 4: Customer Registration

**Files:**
- Modify: `rbac-java/src/main/java/com/rbac/base/modules/rbac/controller/AuthController.java`
- Modify: `rbac-java/src/main/java/com/rbac/base/core/config/SaTokenConfig.java`
- Modify: `rbac-vue/src/views/login/index.vue`

- [ ] **Step 1: Add `/auth/register` backend endpoint**

Validate username, password and phone/email. Create a normal customer user and bind default customer role.

- [ ] **Step 2: Allow anonymous access**

Add `/auth/register` to Sa-Token exclude paths.

- [ ] **Step 3: Add frontend register form**

Login page toggles between login and register. Register success returns to login form.

## Task 5: Frontend Ticket Pages

**Files:**
- Create: `rbac-vue/src/views/ticket/workbench/index.vue`
- Create: `rbac-vue/src/views/ticket/my/index.vue`
- Create: `rbac-vue/src/views/ticket/detail/index.vue`
- Modify: `rbac-vue/src/router/index.js`

- [ ] **Step 1: Implement ticket workbench**

Show filters, status tags, priority tags, current handler, creator, created time and operations.

- [ ] **Step 2: Implement my ticket page**

Customer can submit ticket and view owned tickets.

- [ ] **Step 3: Implement ticket detail page**

Show base info, flow timeline, reply box, transfer/resolve/close actions, and upload controls.

- [ ] **Step 4: Build frontend**

Run: `pnpm build` in `rbac-vue`.

Expected: build succeeds with Node 18+.

## Task 6: Frontend Knowledge Pages

**Files:**
- Create: `rbac-vue/src/views/ticket/knowledge/index.vue`
- Create: `rbac-vue/src/views/ticket/knowledge/detail.vue`
- Modify: `rbac-vue/src/router/index.js`

- [ ] **Step 1: Implement knowledge list**

Support keyword, category, status filters and publish/withdraw actions.

- [ ] **Step 2: Implement knowledge detail editor**

Capture phenomenon, cause analysis, solution steps, tags, applicable scope and linked ticket IDs.

- [ ] **Step 3: Build frontend**

Run: `pnpm build` in `rbac-vue`.

## Task 7: Database Initialization

**Files:**
- Modify: `init.sql`
- Create: `docs/ticket-business.md`

- [ ] **Step 1: Add business tables**

Create `tx_ticket`, `tx_ticket_flow`, `tx_ticket_attachment`, `tx_knowledge_article`, `tx_knowledge_ticket_link`.

- [ ] **Step 2: Add dictionary data**

Add ticket status, priority, category and flow action dictionaries.

- [ ] **Step 3: Add menus and permissions**

Add 工单工作台、我的工单、知识库菜单 and permission keys `ticket:*` and `knowledge:*`.

- [ ] **Step 4: Add default customer role**

Add customer role and authorize customer menus and permissions.

## Verification

Because the user explicitly disallowed proactive unit tests, do not create new test classes. Verification uses:

1. Backend compile: `mvn -q -DskipTests compile`.
2. Frontend build: `pnpm build`.
3. SQL syntax inspection for `init.sql`.
4. Manual API smoke check after the user starts MySQL and backend service.

