# 网络文学创作与阅读平台 · 项目协作规范

- 版本：v1.1
- 维护人：D
- 生效日期：2026-11-24
- 适用范围：A/B/C/D 全体成员

v1.1 在不改变既有协作规则的前提下，补充当前前后端目录、目录职责和已落地功能。

---

## 1. 文件命名格式

统一格式：

```
模块_内容_v主.次_20261119.后缀
```

- **模块**：`A`（用户与交易）、`B`（作品与内容）、`C`（互动与榜单）、`D`（运营与集成）
- **内容**：简短中文或英文，不用空格，可用下划线连接
- **版本**：`v0.1` 草案、`v1.0` 评审冻结、`v1.1` 修订
- **日期**：交付或修订当天，8 位数字
- **后缀**：`.sql` `.md` `.docx` `.xlsx` `.drawio` `.pptx` `.png`

正确示例：

```
A_用户交易_建表_v1.0_20261127.sql
B_作品内容_ER子图_v0.1_20261120.drawio
C_评分触发器_v1.0_20261204.sql
D_数据字典_v1.0_20261120.xlsx
D_分工核对表_v1.0_20261120.xlsx
```

错误示例（禁止）：

```
用户表.sql                  ← 缺模块、缺版本、缺日期
A建表最终版.sql             ← 版本写“最终版”，无法追溯
B_ER图(改).drawio           ← 含括号和“改”字
C 评分 触发器.sql           ← 含空格
```

---

## 2. 目录用途

| 目录 | 放什么 | 谁负责 | 禁止放什么 |
|---|---|---|---|
| `docs/` | 设计文档、数据字典、项目规范、评审记录 | D 维护，A/B/C 提交各自文档 | 代码、SQL 脚本 |
| `docs/er/` | A/B/C 的 ER 子图 `.drawio` 或 `.png` | A/B/C 各自 | 全局 ER 图（放 `docs/` 根） |
| `sql/ddl/` | 各成员建表 SQL，按模块命名 | A/B/C/D 各自 | 存储过程、触发器 |
| `sql/objects/` | 存储过程、触发器、视图脚本 | A/B/C 各自 | 建表语句 |
| `sql/`（根） | `D_schema_all_v*.sql` 合并脚本、初始化数据 | D | 个人零散脚本 |
| `api/` | 接口说明 `.md` | A/B/C 提交，D 统一维护总接口文档 | 后端代码 |
| `test/` | 测试脚本、测试数据、测试报告 | A/B/C/D 各自 | 生产数据、密码 |
| `backend/` | 后端代码 | 按技术栈分工 | 数据库导出文件 |
| `frontend/` | 前端代码 | 按页面分工 | 设计稿源文件 |
| `report/` | 开题报告、章节 docx、PPT | D 统稿，A/B/C 交章节 | 临时草稿 |
| `manage/` | 分工核对表、进度周报、风险清单、评审记录 | D | 业务文档 |

### 2.1 当前后端目录与功能

后端根目录为 `backend/`，根包为 `com.novel.platform`，采用单体 Spring Boot 结构。

```text
backend/
├── .mvn/wrapper/                       # Maven Wrapper 配置
├── mvnw、mvnw.cmd                     # 跨平台 Maven Wrapper 启动脚本
├── pom.xml                            # Java 17、Spring Boot、MyBatis-Plus 等依赖
└── src/
    ├── main/java/com/novel/platform/
    │   ├── NovelPlatformApplication.java
    │   ├── common/
    │   │   ├── api/                   # 统一 JSON 响应
    │   │   └── exception/             # 业务异常与全局异常处理
    │   ├── config/                    # Web、CORS 等公共配置
    │   ├── modules/
    │   │   ├── user/                  # A：用户与交易模块
    │   │   ├── novel/                 # B：作品与内容模块
    │   │   ├── interaction/           # C：互动与榜单模块
    │   │   └── admin/                 # D：运营后台模块
    │   └── utils/                     # 公共工具，仅放置确有复用需要的代码
    ├── main/resources/
    │   ├── application.yml            # 服务端口、数据库和 MyBatis-Plus 配置
    │   └── mapper/                    # 后续按需要存放 Mapper XML
    └── test/java/                     # 后端自动化测试
```

| 目录或文件 | 当前功能 | 当前状态 |
|---|---|---|
| `NovelPlatformApplication.java` | Spring Boot 应用启动入口 | 已完成 |
| `common/api/` | 提供 `ApiResponse<T>`，统一 `code`、`message`、`data` 响应结构 | 已完成 |
| `common/exception/` | 提供业务异常、参数错误、404 和 500 的统一处理 | 已完成 |
| `config/` | 配置 `/api/**` 跨域访问，允许前端开发服务器调用 | 已完成 |
| `modules/user/` | 用户、订阅和交易相关代码归属目录 | A 模块预留 |
| `modules/novel/` | 小说、章节、标签和检索相关代码归属目录 | B 模块预留 |
| `modules/interaction/` | 评论、评分、收藏和榜单相关代码归属目录 | C 模块预留 |
| `modules/admin/` | 后台统计、审核和运营配置相关代码归属目录 | 已实现统计 Mock 接口，其余待开发 |
| `utils/` | 跨模块复用工具 | 预留，禁止堆放仅使用一次的包装代码 |
| `application.yml` | 配置 8080 端口、环境变量数据库连接和 MyBatis-Plus | 已完成；真实数据库连接需另行配置和验证 |
| `src/test/java/` | 当前包含后台统计接口及统一 404 测试 | 已完成基础测试 |

当前后台 Mock 接口为 `GET /api/admin/dashboard/summary`，返回 4 项模拟统计和 3 条待审核摘要。该接口不查询真实数据库，不得作为统计业务已经完成的依据。

### 2.2 当前前端目录与功能

前端根目录为 `frontend/`，使用 Vue 3、Vue Router、Element Plus、Axios 和 Vite 6。

```text
frontend/
├── index.html                         # HTML 入口
├── package.json、package-lock.json    # 依赖及启动、构建脚本
├── vite.config.js                     # Vue 插件、5173 开发服务及 /api 代理
├── .env.example                       # 无密钥环境配置示例
└── src/
    ├── main.js                        # Vue、路由和 Element Plus 初始化
    ├── App.vue                        # 根组件与路由出口
    ├── api/
    │   ├── request.js                 # Axios 实例、统一响应解析和错误提示
    │   └── admin.js                   # 后台统计接口封装
    ├── router/
    │   └── index.js                   # 后台五个页面路由及默认重定向
    ├── layouts/
    │   └── AdminLayout.vue            # 后台整体布局与导航菜单
    ├── components/
    │   └── PlaceholderPanel.vue       # 预留页面公共说明组件
    ├── views/admin/
    │   ├── DashboardView.vue          # 数据统计与待审核摘要
    │   ├── AuditManagementView.vue    # 审核管理入口
    │   ├── UserManagementView.vue     # 用户管理入口
    │   ├── TagManagementView.vue      # 标签管理入口
    │   └── RankingConfigView.vue      # 榜单配置入口
    └── styles/
        └── main.css                   # 全局、响应式及可访问性样式
```

| 目录或文件 | 当前功能 | 当前状态 |
|---|---|---|
| `api/request.js` | 统一请求前缀、8 秒超时、响应格式校验、成功数据提取及错误提示 | 已完成 |
| `api/admin.js` | 请求后台统计摘要 | 已接通 Mock 接口 |
| `router/index.js` | 注册后台路由，根路径和未知地址重定向到统计页 | 已完成 |
| `AdminLayout.vue` | 提供后台侧栏、五个导航入口及 Mock 状态标识 | 已完成 |
| `DashboardView.vue` | 展示 4 项统计、3 条待审核摘要、加载失败提示和重新加载入口 | 已完成 Mock 联调 |
| `PlaceholderPanel.vue` | 说明预留页面的模块归属和后续开发状态 | 已完成 |
| 其余四个管理页面 | 提供审核、用户、标签和榜单配置入口 | 仅预留页面，未开发业务 |

### 2.3 当前后台路由

| 菜单 | 路由 | 功能边界 |
|---|---|---|
| 数据统计 | `/admin/dashboard` | 展示后端 Mock 统计数据 |
| 审核管理 | `/admin/audits` | 预留入口，未接真实审核业务 |
| 用户管理 | `/admin/users` | 预留入口，未接真实用户业务 |
| 标签管理 | `/admin/tags` | 预留入口，未接真实标签业务 |
| 榜单配置 | `/admin/rankings` | 预留入口，未接真实榜单配置 |

当前只有数据统计页与后端 Mock 接口通信。A/B/C 的模块目录和其余后台页面均为后续开发边界，不得将“目录或页面入口已经建立”描述为“业务功能已经完成”。

---

## 3. 提交信息格式

统一格式：

```
[模块] 动作 + 对象 + 版本
```

- **模块**：`[A]` `[B]` `[C]` `[D]`，多人协作的合并提交写 `[D][合并]`
- **动作**：添加 / 修改 / 删除 / 修复 / 合并 / 评审
- **对象**：具体文件或表名，不要写“更新一下”“改了点东西”
- **版本**：对应文件版本号，没有版本可省略

正确示例：

```
[A] 添加用户表建表 SQL v1.0
[A] 修改订阅记录表联合主键，增加章节ID v1.1
[B] 添加章节检索索引脚本 v1.0
[C] 修复评分触发器平均分计算错误 v1.1
[D] 合并 A/B/C 建表脚本生成 schema_all v1.0
[D] 评审表结构 v1，冻结用户表与小说表
[D] 更新分工核对表 W5 交付状态
```

错误示例（禁止）：

```
update
修改
final
aaa
[A] 改了一下
```

**提交前检查**：GitHub Desktop 左下角 `Summary` 填上面格式，`Description` 可留空或写一句补充；一次提交只做一件事，不要把建表和改文档混在一个提交里。

---

## 4. 表结构冻结与改表评审规则

### 4.1 冻结节点

- W5 周五表结构评审通过后，D 发布 **表结构冻结 v1**，包括：全局 ER 图、数据字典、表名、主外键、状态枚举。
- 冻结后，A/B/C 不得直接修改已冻结表的字段名、类型、主外键、枚举值。

### 4.2 改表流程

1. **提出**：需要改表的成员在群里说明——改哪张表、改什么、为什么、影响谁。
2. **评审**：D 组织至少 A/B/C 中受影响的成员确认，必要时开 15 分钟短会。
3. **记录**：D 在 `manage/D_表结构评审记录_v*.md` 中登记，格式见 4.3。
4. **执行**：提出人修改自己的 DDL，版本号 +0.1（如 v1.0 → v1.1），提交信息写 `[X] 修改XX表XX字段 v1.1`。
5. **同步**：D 更新数据字典和 `D_schema_all_v*.sql`，通知受影响成员拉取最新 `develop`。

### 4.3 评审记录格式

在 `manage/D_表结构评审记录_v1.0_YYYYMMDD.md` 中按下面格式追加：

```markdown
## 评审记录 001

- 日期：2026-11-19
- 提出人：A
- 涉及表：subscription（订阅记录表）
- 变更内容：联合主键由 (user_id, chapter_id) 保持不变，新增字段 `price DECIMAL(10,2) NOT NULL`
- 变更原因：扣费时需要记录当时章节单价，便于对账
- 影响范围：A 的建表 SQL、订阅扣费存储过程；D 的数据字典；C 无需改动
- 评审结论：通过 / 退回修改
- 参与确认：A、B、C、D
- 执行版本：A_用户交易_建表_v1.1_20261119.sql
```

### 4.4 不允许的行为

- 不写评审记录直接改已冻结表；
- 在 `develop` 上直接改别人的 DDL；
- 改表后不通知 D 更新数据字典和合并脚本；
- 用“最终版”“最终版2”代替版本号。

---

## 5. 分支与合并规则（补充）

- `main`：稳定交付分支，只在 W9、W12、W14、W15 由 D 从 `develop` 合并。
- `develop`：日常集成分支，A/B/C 的 `feature/*` 合并到这里。
- `feature/A_user_trade`、`feature/B_content`、`feature/C_interaction`、`feature/D_ops`：各自日常开发，从 `develop` 拉出，完成后合并回 `develop`。
- 每次开工前先 `Fetch origin` / `Pull origin`。
- 冲突统一在 VS Code 中解决，不在 GitHub Desktop 里硬解。

---

## 6. 版本记录

| 版本 | 日期 | 修改人 | 修改内容 |
|---|---|---|---|
| v1.0 | 2026-11-19 | D | 初版，明确命名、目录、提交、评审规则 |
| v1.1 | 2026-11-24 | D | 补充当前前后端目录、目录职责、后台路由及功能完成边界 |
