# 工单系统下一阶段问题记录

更新时间：2026-07-05

## 当前已预留的位置

1. 后端 AI/Agent 接口：
   - `POST /ticket/ai/precheck`：提单前整理客户原话。
   - `POST /ticket/ai/diagnose`：处理阶段生成排查建议和知识候选。
   - 当前实现为本地规则兜底，不调用外部模型。

2. AI 配置：
   - `ticket.ai.enabled`
   - `ticket.ai.provider`
   - `ticket.ai.model`
   - `ticket.ai.api-key`
   - `ticket.ai.base-url`

3. 前端调用：
   - 提单页“按内容整理”优先调用后端接口，失败后使用本地规则。
   - 处理页“AI 排查助手”优先展示后端建议，失败后展示本地规则。

4. 首页统计：
   - `GET /ticket/statistics` 返回当前账号可见范围内的工单统计。
   - 首页指标已改为后端真实统计，不再用最近工单推导总量。

5. 提单入口：
   - 客户使用 `ticket:my:add` 提交工单。
   - 运维或管理员可使用 `ticket:add` 代提交工单。
   - 提交后按权限返回“我的工单”或“工单工作台”。

## 本阶段已处理问题

1. 修正提交工单路由权限，从仅依赖 `ticket:my:list` 调整为 `ticket:add` / `ticket:my:add`。
2. 补齐菜单权限绑定：提交工单菜单绑定内部新增权限，待接工单菜单绑定驳回权限。
3. AI/Agent 接口先保持本地规则兜底，不新增外部依赖，避免 key 和依赖版本阻塞主流程。
4. 收紧知识库审核状态：只有待审核文章可以审核通过发布，只有已发布文章可以下架。
5. 收紧已撤销工单处理入口：直连处理页时不再显示回复入口。
6. 补齐升级脚本中的工单菜单、权限、客户角色和角色授权，避免老库升级后入口不完整。
7. 收紧知识库编辑：只有新建、草稿、审核驳回状态可以保存修改，待审核和已发布内容不能绕过审核直接改。
8. 收敛前端权限显示：系统配置页只读账号不显示新增/编辑/停用动作，我的工单页按提交权限显示提交按钮。
9. 补齐后端 DTO 空请求体校验，避免外部接口传空 body 时出现空指针。
10. 收敛知识库列表权限显示：列表只读账号只显示查看，维护和发布动作按 `knowledge:edit` / `knowledge:publish` 展示。
11. 知识详情路由允许 `knowledge:list` 只读查看，保存按钮严格要求 `knowledge:edit`。
12. “我的工单”允许 `ticket:add` 进入，避免运维代提交草稿无入口可继续编辑。
13. 补充 `supervisor` 工单主管角色种子，和后端/前端审批人判断保持一致。
14. 收紧系统配置管理边界：除权限码外，还必须是管理员或工单主管角色。
15. 收紧关闭工单边界：只有工单创建人、当前处理人、管理员或工单主管可以关闭已解决工单。
16. 升级脚本补齐工单主表、流程、附件、知识库、知识关联表的 `CREATE TABLE IF NOT EXISTS` 兜底，避免非工单版本老库升级时因表不存在中断。
17. 补齐“所属系统”结构化字段：提单页 `systemCode` 会落到 `tx_ticket.system_code`，后端支持按系统编码筛选，草稿回显优先读取结构化字段。
18. 修复 `schema_upgrade_all.sql` 历史基础结构段非幂等问题，重复执行时不再因已有字段、已有索引或废弃的 `sys_permission.menu_id` 字段中断。

## 数据库执行记录

1. 2026-07-05 已在本机 `localhost:3306/tx_ticket` 执行 `scripts/schema_upgrade_all.sql`。
2. 首次执行发现脚本中历史 `ALTER TABLE` 不是幂等写法，已改成 `information_schema` 判断后动态执行。
3. 修复后已无 `--force` 重跑成功；当前只剩 MySQL 命令行密码提示和 `VALUES()` 未来弃用提示，不影响本阶段运行。

## 下一阶段 TODO

1. TODO(agent)：确定接入方案，二选一即可：
   - Spring AI：适合 Spring Boot 项目统一配置、后续接多个模型供应商。
   - LangChain4j：适合做多 Agent、工具调用、知识库检索增强。

2. TODO(agent)：确定模型供应商后补充环境变量：
   - `TICKET_AI_API_KEY`
   - `TICKET_AI_BASE_URL`
   - `TICKET_AI_MODEL`
   - 如供应商需要额外参数，再补 `TICKET_AI_PROVIDER`。

3. TODO(agent)：把 `TicketAiService` 中的本地规则替换为适配器模式：
   - `TicketAiClient` 接口：负责调用模型。
   - `LocalRuleTicketAiClient`：当前规则兜底。
   - `SpringAiTicketAiClient` 或 `LangChain4jTicketAiClient`：真实模型调用。

4. TODO(agent)：知识库增强：
   - 当前只按分类和标题/标签做候选。
   - 后续可加入向量检索、相似工单召回、解决方案自动草稿。

5. TODO(sla)：SLA 自动化：
   - 当前支持人工 SLA 提醒。
   - 后续补定时任务，根据优先级、创建时间、处理状态自动预警和升级。

6. TODO(approval)：审批人配置化：
   - 当前审批权限依赖主管/管理员角色。
   - 后续可按系统负责人、部门、优先级配置审批人和多级审批规则。

7. TODO(permission)：联调后复核历史库角色授权：
   - 新脚本不再给业务操作员新增 `ticket:assign` / `ticket:reject`。
   - 如果老库之前已经授予这些权限，建议在权限管理页面人工回收，避免影响已有自定义授权数据。

## 已知环境问题

1. 当前本机 Java 环境只有 JRE 1.8，没有 JDK 编译器。
   - Maven 编译会报：`No compiler is provided in this environment`。
   - 项目 `pom.xml` 配置为 Java 21，后续测试需要安装 JDK 21 并配置 `JAVA_HOME`。

2. 本阶段按要求不执行测试。
   - 下一阶段你测试后给出问题，我再按实际反馈修复。
