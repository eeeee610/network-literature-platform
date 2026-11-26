# 网络文学创作与阅读平台 · 技术开发规范

- 版本：v1.0
- 维护人：D
- 生效日期：2026-11-23
- 适用范围：A/B/C/D 前后端开发

本文只补充应用开发规则。文件命名、分支、提交及表结构变更继续遵循 `D_项目规范_v1.0_20261119.md`，冻结 SQL 不得由应用开发者直接修改。

## 1. 技术栈

| 层级 | 技术 |
|---|---|
| 后端 | Java 17、Spring Boot 3.5.7、MyBatis-Plus 3.5.12、MySQL Connector/J |
| 前端 | Vue 3、Vue Router 4、Element Plus、Axios |
| 构建 | Maven Wrapper 3.3.4 / Maven 3.9.9、Vite 6 |
| 数据库 | MySQL，表结构及公共数据库对象以已冻结 SQL 为准 |
| 架构 | 单体后端 + 单页面前端，不引入微服务 |

后端根包为 `com.novel.platform`。前端使用 JavaScript，开发地址为 `http://127.0.0.1:5173`，后端默认端口为 `8080`。

## 2. 项目目录

```text
backend/
├── .mvn/wrapper/               # Maven Wrapper 配置
├── src/main/java/com/novel/platform/
│   ├── common/
│   │   ├── api/                # 统一响应对象
│   │   └── exception/          # 业务异常与全局异常处理
│   ├── config/                 # Web、数据库等公共配置
│   ├── modules/
│   │   ├── user/               # A：用户与交易
│   │   ├── novel/              # B：作品与内容
│   │   ├── interaction/        # C：互动与榜单
│   │   └── admin/              # D：运营与后台
│   ├── utils/                  # 确有复用需要的公共工具
│   └── NovelPlatformApplication.java
├── src/main/resources/
│   ├── application.yml
│   └── mapper/                 # 需要 XML 时按模块组织
└── src/test/java/              # 后端测试

frontend/
├── src/
│   ├── api/                    # Axios 实例及按模块划分的接口
│   ├── components/             # 可复用组件
│   ├── layouts/                # 页面布局
│   ├── router/                 # 路由与页面入口
│   ├── styles/                 # 全局样式
│   ├── views/                  # 页面，按模块建立子目录
│   ├── App.vue
│   └── main.js
├── .env.example                # 仅保存无密钥的配置示例
├── index.html
├── package.json
└── vite.config.js
```

各业务模块内部按需要建立 `controller`、`service`、`mapper`、`entity`、`dto`。简单课程设计不强制为每个类增加多余分层，但 Controller 不直接编写 SQL，跨模块公共过程由 Service 统一调用。

## 3. 配置与密钥

- 数据库连接通过 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 注入，端口通过 `SERVER_PORT` 配置。
- 不在仓库提交真实密码、Token 或本机绝对路径；本地敏感配置使用已被 Git 忽略的文件或环境变量。
- `application.yml` 默认 URL 指向独立集成测试库，仅用于保留连接配置，不代表数据库已经连通。使用其他库时显式设置 `DB_URL`，不得默认连接正式库。
- 禁止在应用启动时自动执行初始化 SQL。表结构或公共过程变更必须走既有评审流程。
- 前端非敏感默认值写入 `.env.example`；`VITE_API_BASE_URL` 默认 `/api`，Vite 开发服务器将该前缀代理至后端。

## 4. API 命名与请求参数

### 4.1 地址与方法

- 统一前缀：`/api/{module}`，模块使用 `user`、`novel`、`interaction`、`admin`。
- 资源名使用小写英文单数或短横线，如 `/api/novel/chapter`、`/api/admin/audit-log`。
- 查询使用 `GET`，新增使用 `POST`，整体修改使用 `PUT`，删除使用 `DELETE`；审核、发布等明确业务动作使用 `POST`，如 `/api/admin/comment/{id}/audit`。
- 不在地址中放置密码等敏感信息，不随意混用中英文或下划线。

### 4.2 参数约定

- JSON 字段、路径变量及普通请求参数统一使用小驼峰，如 `userId`、`pageSize`。
- 数据库下划线字段由 MyBatis-Plus 转换为 Java 小驼峰属性，不直接暴露数据库行对象作为所有接口的固定契约。
- 数据库 `BIGINT` ID 在 JSON 中使用字符串，避免 JavaScript 精度丢失。
- GET 查询参数放在 Query；写操作 JSON 放在 Request Body；资源 ID 放在 Path。
- 列表查询统一使用 `page`（从 1 开始）和 `pageSize`；默认 `page=1`、`pageSize=10`，单页上限暂定为 100。
- 日期时间统一使用 ISO 8601 格式并明确时区；后端 Jackson 默认时区为 `Asia/Shanghai`。
- 必填、长度、取值范围由后端 Bean Validation 校验，前端校验不能替代后端校验。
- 涉及评论等公共写入过程时，遵守对应接口说明；调用方管理事务，审核状态与审核日志必须同事务提交或回滚。

## 5. JSON 响应格式

所有业务接口统一返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

- 成功：`code=0`、`message="success"`，`data` 为对象、数组或 `null`。
- 失败：`code` 为非零整数，`message` 为可供前端提示的简明中文，`data=null`。
- HTTP 状态码仍需正确设置；调用方不能仅凭 HTTP 200 判断业务成功。
- 分页数据的 `data` 建议固定为 `{ "list": [], "total": 0, "page": 1, "pageSize": 10 }`。
- 前端 `src/api/request.js` 统一校验响应结构，并在成功时只返回 `body.data`；页面不重复解析外层响应。

## 6. 异常与错误码

- 可预期业务失败抛出 `BusinessException`，不得在 Controller 中到处拼接不同错误结构。
- 参数绑定或校验失败：HTTP 400、业务码 `40000`。
- 接口不存在：HTTP 404、业务码 `40400`。
- 未处理服务器异常：HTTP 500、业务码 `50000`，统一记录服务端日志，不向客户端泄露堆栈和 SQL。
- 其他业务码由模块在接口文档中登记，建议采用五位数并避开公共码；同时注明对应 HTTP 状态。
- 写操作失败必须回滚事务。不得捕获异常后忽略，也不得返回成功但数据未保存。
- 接口文档统一使用 `api/D_统一接口文档模板_v1.0_20261123.md`，至少写明鉴权、参数、响应、错误、事务与幂等要求。

## 7. 前端开发

- 页面放入 `views`，可复用 UI 放入 `components`，HTTP 请求集中放入 `api`，不在组件中散写完整 URL。
- 路由统一在 `router/index.js` 注册；新增页面需同时补充导航入口和 API 文档。
- Axios 错误提示由响应拦截器统一处理，页面仍须维护加载状态，并为失败提供合理反馈或重试入口。
- 使用 Element Plus 组件和现有全局样式，不引入额外 UI 框架；图标使用已安装的 `@element-plus/icons-vue`。
- 后台五个预留入口目前仅为骨架和模拟数据，不得将其视为审核、用户、标签、榜单或统计业务已经完成。

## 8. 数据库与 MyBatis-Plus

- Mapper 只负责数据访问，业务规则及事务边界放在 Service。
- 多步写入使用 `@Transactional` 明确事务；调用不自行提交事务的数据库公共过程时，由调用方统一提交或回滚。
- 优先使用 MyBatis-Plus 通用能力；复杂 SQL 放入对应 Mapper XML，避免字符串拼接 SQL。
- 所有用户输入通过参数绑定传入，禁止拼接表名、条件或值来规避参数化查询。
- 不绕过已约定的公共过程直接修改评论、审核状态等受约束数据。

## 9. 启动与提交前检查

后端在 `backend/` 执行：

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

本机 Java 不可用时，可临时将 `JAVA_HOME` 指向便携 JDK 17，并把 `$JAVA_HOME\bin` 加入当前 PowerShell 会话的 `Path`；不得把个人临时路径提交到项目配置。

前端在 `frontend/` 执行：

```powershell
npm install
npm run build
npm run dev
```

提交前至少确认：

1. 后端测试及前端构建通过，实际改动涉及的页面或接口已验证。
2. 控制台无新增错误，接口格式、参数和错误说明与 `api/` 文档一致。
3. 未提交 `.env`、真实凭据、`target/`、`node_modules/`、`dist/` 或临时日志。
4. 未修改冻结 SQL 和他人模块文件；确需变更时先沟通并评审。
5. 一次提交只做一件事，提交信息继续使用 `[模块] 动作 + 对象 + 版本`；分支继续使用既有 `main`、`develop`、`feature/*` 规则。

## 10. 版本记录

| 版本 | 日期 | 修改人 | 修改内容 |
|---|---|---|---|
| v1.0 | 2026-11-23 | D | 初版，明确技术栈、目录、接口、异常及开发检查规则 |
