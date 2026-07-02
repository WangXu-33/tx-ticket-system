# 头绪工单系统迁移实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `E:\code\gemini\RBAC-BASE` 的 Web 前端与 Java 后端迁移到 `E:\code\gemini\个人工单系统`，并完成首轮“头绪工单系统 / tx”命名改造。

**Architecture:** 目标目录继续保持前后端分离结构，直接落成 `rbac-java` 与 `rbac-vue` 两个工程，保留原有技术栈和接口边界，仅替换对外名称、工程标识、数据库名与文件存储前缀。整个迁移过程不触碰 `rbac-uniapp`，也不重构 Java 包路径与现有 `/rbac/...` 接口路径。

**Tech Stack:** Spring Boot 3、MyBatis-Plus、Sa-Token、Vue 3、Vite、Ant Design Vue、PowerShell

---

### Task 1: 复制干净的前后端工程与根目录资料

**Files:**
- Create: `E:\code\gemini\个人工单系统\rbac-java\**`
- Create: `E:\code\gemini\个人工单系统\rbac-vue\**`
- Create: `E:\code\gemini\个人工单系统\init.sql`
- Create: `E:\code\gemini\个人工单系统\README.md`
- Create: `E:\code\gemini\个人工单系统\PROJECT_GUIDE.md`
- Create: `E:\code\gemini\个人工单系统\scripts\**`

- [ ] **Step 1: 创建目标目录**

```powershell
New-Item -ItemType Directory -Force 'E:\code\gemini\个人工单系统\rbac-java' | Out-Null
New-Item -ItemType Directory -Force 'E:\code\gemini\个人工单系统\rbac-vue' | Out-Null
New-Item -ItemType Directory -Force 'E:\code\gemini\个人工单系统\scripts' | Out-Null
```

- [ ] **Step 2: 复制后端工程，排除缓存与产物**

```powershell
robocopy 'E:\code\gemini\RBAC-BASE\rbac-java' 'E:\code\gemini\个人工单系统\rbac-java' /E /XD target .idea /XF *.log *.tmp *.temp *.swp *.orig *.bak*
```

- [ ] **Step 3: 复制前端工程，排除缓存与产物**

```powershell
robocopy 'E:\code\gemini\RBAC-BASE\rbac-vue' 'E:\code\gemini\个人工单系统\rbac-vue' /E /XD node_modules dist .idea /XF *.log *.tmp *.temp *.swp *.orig *.bak*
```

- [ ] **Step 4: 复制初始化 SQL、脚本与文档**

```powershell
Copy-Item -LiteralPath 'E:\code\gemini\RBAC-BASE\init.sql' -Destination 'E:\code\gemini\个人工单系统\init.sql' -Force
Copy-Item -LiteralPath 'E:\code\gemini\RBAC-BASE\README.md' -Destination 'E:\code\gemini\个人工单系统\README.md' -Force
Copy-Item -LiteralPath 'E:\code\gemini\RBAC-BASE\PROJECT_GUIDE.md' -Destination 'E:\code\gemini\个人工单系统\PROJECT_GUIDE.md' -Force
Copy-Item -LiteralPath 'E:\code\gemini\RBAC-BASE\scripts\README.md' -Destination 'E:\code\gemini\个人工单系统\scripts\README.md' -Force
Copy-Item -LiteralPath 'E:\code\gemini\RBAC-BASE\scripts\schema_upgrade_all.sql' -Destination 'E:\code\gemini\个人工单系统\scripts\schema_upgrade_all.sql' -Force
```

- [ ] **Step 5: 核对目标目录结构**

```powershell
Get-ChildItem -Force 'E:\code\gemini\个人工单系统' | Select-Object Name,Mode
```

Expected: 输出 `docs`、`rbac-java`、`rbac-vue`、`scripts`、`init.sql`、`README.md`、`PROJECT_GUIDE.md`

### Task 2: 备份关键配置与展示文件

**Files:**
- Modify: `E:\code\gemini\个人工单系统\rbac-java\pom.xml`
- Modify: `E:\code\gemini\个人工单系统\rbac-java\src\main\resources\application.yml`
- Modify: `E:\code\gemini\个人工单系统\rbac-java\src\main\java\com\rbac\base\RbacApplication.java`
- Modify: `E:\code\gemini\个人工单系统\rbac-vue\package.json`
- Modify: `E:\code\gemini\个人工单系统\rbac-vue\index.html`
- Modify: `E:\code\gemini\个人工单系统\rbac-vue\src\layout\index.vue`
- Modify: `E:\code\gemini\个人工单系统\rbac-vue\src\views\login\index.vue`
- Modify: `E:\code\gemini\个人工单系统\init.sql`
- Modify: `E:\code\gemini\个人工单系统\scripts\schema_upgrade_all.sql`

- [ ] **Step 1: 生成同目录备份文件**

```powershell
$stamp = Get-Date -Format 'yyyyMMddHHmm'
Copy-Item 'E:\code\gemini\个人工单系统\rbac-java\pom.xml' "E:\code\gemini\个人工单系统\rbac-java\pom.xml.bak.$stamp"
Copy-Item 'E:\code\gemini\个人工单系统\rbac-java\src\main\resources\application.yml' "E:\code\gemini\个人工单系统\rbac-java\src\main\resources\application.yml.bak.$stamp"
Copy-Item 'E:\code\gemini\个人工单系统\rbac-java\src\main\java\com\rbac\base\RbacApplication.java' "E:\code\gemini\个人工单系统\rbac-java\src\main\java\com\rbac\base\RbacApplication.java.bak.$stamp"
Copy-Item 'E:\code\gemini\个人工单系统\rbac-vue\package.json' "E:\code\gemini\个人工单系统\rbac-vue\package.json.bak.$stamp"
Copy-Item 'E:\code\gemini\个人工单系统\rbac-vue\index.html' "E:\code\gemini\个人工单系统\rbac-vue\index.html.bak.$stamp"
Copy-Item 'E:\code\gemini\个人工单系统\rbac-vue\src\layout\index.vue' "E:\code\gemini\个人工单系统\rbac-vue\src\layout\index.vue.bak.$stamp"
Copy-Item 'E:\code\gemini\个人工单系统\rbac-vue\src\views\login\index.vue' "E:\code\gemini\个人工单系统\rbac-vue\src\views\login\index.vue.bak.$stamp"
Copy-Item 'E:\code\gemini\个人工单系统\init.sql' "E:\code\gemini\个人工单系统\init.sql.bak.$stamp"
Copy-Item 'E:\code\gemini\个人工单系统\scripts\schema_upgrade_all.sql' "E:\code\gemini\个人工单系统\scripts\schema_upgrade_all.sql.bak.$stamp"
```

- [ ] **Step 2: 确认备份已生成**

```powershell
Get-ChildItem 'E:\code\gemini\个人工单系统\rbac-java','E:\code\gemini\个人工单系统\rbac-vue','E:\code\gemini\个人工单系统','E:\code\gemini\个人工单系统\scripts' -Filter '*.bak.*' -Recurse
```

Expected: 输出上一步创建的 `.bak.时间戳` 文件

### Task 3: 执行系统名称与 tx 标识替换

**Files:**
- Modify: `E:\code\gemini\个人工单系统\rbac-java\pom.xml`
- Modify: `E:\code\gemini\个人工单系统\rbac-java\src\main\resources\application.yml`
- Modify: `E:\code\gemini\个人工单系统\rbac-java\src\main\java\com\rbac\base\RbacApplication.java`
- Modify: `E:\code\gemini\个人工单系统\rbac-vue\package.json`
- Modify: `E:\code\gemini\个人工单系统\rbac-vue\index.html`
- Modify: `E:\code\gemini\个人工单系统\rbac-vue\vite.config.js`
- Modify: `E:\code\gemini\个人工单系统\rbac-vue\src\layout\index.vue`
- Modify: `E:\code\gemini\个人工单系统\rbac-vue\src\views\login\index.vue`
- Modify: `E:\code\gemini\个人工单系统\rbac-vue\src\views\system\config\index.vue`
- Modify: `E:\code\gemini\个人工单系统\rbac-vue\src\views\rbac\analytics\index.vue`
- Modify: `E:\code\gemini\个人工单系统\init.sql`
- Modify: `E:\code\gemini\个人工单系统\scripts\schema_upgrade_all.sql`

- [ ] **Step 1: 替换工程标识与展示文案**

```text
后端 pom:
- artifactId: rbac-java -> tx-ticket-server
- name: rbac-java -> tx-ticket-server
- description: RBAC Base Project with Spring Boot 3 & Sa-Token -> TX Ticket System Server with Spring Boot 3 & Sa-Token

前端 package:
- name: rbac-vue -> tx-ticket-web

前端标题与文案:
- index.html title -> 头绪工单系统
- layout logo text -> 头绪工单系统
- login 标题相关文案 -> 头绪工单系统 / TX Ticket System

后端启动提示:
- RBAC-BASE 后端服务启动成功！ -> 头绪工单系统后端服务启动成功！
```

- [ ] **Step 2: 替换数据库名、存储目录与文件前缀**

```text
数据库:
- rbac_base -> tx_ticket

本地存储目录:
- E:/rbac_upload/ -> E:/tx_ticket_upload/

文件访问前缀:
- /rbac_files/ -> /tx_files/
- /rbac_files -> /tx_files

导出文件名:
- rbac-analytics.xlsx -> tx-ticket-analytics.xlsx
- rbac-analytics-report -> tx-ticket-analytics-report
```

- [ ] **Step 3: 保留包路径与接口路径，不做整库重构**

```text
不修改:
- com.rbac.base
- /rbac/...
- views/rbac/...
- 权限标识 sys:...
```

### Task 4: 核对迁移结果

**Files:**
- Verify: `E:\code\gemini\个人工单系统\rbac-java\pom.xml`
- Verify: `E:\code\gemini\个人工单系统\rbac-java\src\main\resources\application.yml`
- Verify: `E:\code\gemini\个人工单系统\rbac-vue\package.json`
- Verify: `E:\code\gemini\个人工单系统\rbac-vue\index.html`
- Verify: `E:\code\gemini\个人工单系统\init.sql`

- [ ] **Step 1: 搜索残留的关键旧标识**

```powershell
rg -n "RBAC-BASE|rbac-vue|rbac-java|rbac_base|rbac_upload|rbac_files|rbac-analytics" 'E:\code\gemini\个人工单系统'
```

Expected: 仅保留设计允许不动的 `com.rbac.base`、`/rbac/...`、`views/rbac/...` 等技术边界，不再出现对外展示和基础配置中的旧名称

- [ ] **Step 2: 查看关键文件确认结果**

```powershell
Get-Content -Raw 'E:\code\gemini\个人工单系统\rbac-java\pom.xml'
Get-Content -Raw 'E:\code\gemini\个人工单系统\rbac-java\src\main\resources\application.yml'
Get-Content -Raw 'E:\code\gemini\个人工单系统\rbac-vue\package.json'
Get-Content -Raw 'E:\code\gemini\个人工单系统\rbac-vue\index.html'
Get-Content -Raw 'E:\code\gemini\个人工单系统\init.sql'
```

Expected: 关键名称、数据库名和文件前缀已切换到 `头绪工单系统` / `tx`

- [ ] **Step 3: Git 状态说明**

```powershell
git -C 'E:\code\gemini\个人工单系统' status --short --branch
```

Expected: 若不是 Git 仓库，记录为“当前目录尚未初始化 Git 仓库”
