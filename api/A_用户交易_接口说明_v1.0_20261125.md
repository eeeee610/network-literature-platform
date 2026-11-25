# A 用户与交易 · 接口说明 v1.0

- 文档版本：v1.0
- 接口负责人：A（注册登录、书架、订阅、打赏、余额）
- 最近更新：2026-11-25
- 开发状态：开发完成（接口契约已确定，待联调）
- 关联页面或模块：登录页、注册页、我的书架、章节订阅页、作品打赏页、余额与流水页；向 B 阅读页提供 VIP 章节订阅校验
- 依据文件：`sql/ddl/A_用户交易_建表_v1.1_20261123.sql`、`D_数据字典_v1.0_20261120.xlsx`、`D_统一接口文档模板_v1.0_20261123.md`、`B_作品与内容_接口说明_v1.1_20261124.md`
- 服务前缀：`/api`
- 数据格式：`application/json; charset=utf-8`

## 1. 通用约定

### 1.1 鉴权与角色

- 公开接口：`POST /api/auth/register`、`POST /api/auth/login`。
- 其余接口必须携带请求头 `Authorization: Bearer <JWT>`。未携带、格式错误、签名不合法或已过期，统一返回 HTTP 401、业务码 `40100`。
- JWT 载荷包含 `userId`（`sub`）、`username`、`role`，有效期 12 小时，由后端 `jwt.secret` 签名。
- `role` 取值 `READER`（读者）、`AUTHOR`（作者）、`ADMIN`（管理员）。读者与作者均可使用本模块全部交易接口；`GET /api/user/admin/users` 仅允许 `ADMIN`，越权返回 HTTP 403、业务码 `40300`。
- 用户身份只从令牌解析，请求体与 Query 中不接收 `userId` 或 `fromUserId`，即使传入也一律忽略，防止越权操作他人账户。

### 1.2 响应结构

所有接口统一返回 `ApiResponse<T>`：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| `code` | Integer | 成功固定为 `0`，失败为非零业务码 |
| `message` | String | 成功固定为 `success`，失败为可直接展示的中文提示 |
| `data` | Object / Array / null | 业务数据；失败时固定为 `null` |

HTTP 状态码按语义正确设置，调用方不能只凭 HTTP 200 判定业务成功。

### 1.3 分页

分页接口的 `data` 固定为：

```json
{
  "list": [],
  "total": 0,
  "page": 1,
  "pageSize": 10
}
```

| 参数 | 类型 | 默认值 | 约束 |
|---|---|---|---|
| `page` | Integer | 1 | 从 1 开始；小于 1 返回 `40000` |
| `pageSize` | Integer | 10 | 取值 1～100，超出返回 `40000` |

非分页接口 `data` 直接返回对象，不套分页结构。

### 1.4 时间、ID 与金额

- 所有数据库 `BIGINT` ID 在 JSON 中一律用字符串，如 `"1001"`，避免 JavaScript 精度丢失。
- 日期时间一律 ISO 8601 带时区，后端时区 `Asia/Shanghai`，如 `2026-11-25T10:00:00+08:00`。
- 金额以数值返回，单位元，保留两位小数，如 `12.00`；入参金额必须为数字且最多两位小数。

### 1.5 枚举含义（以冻结 DDL 为准）

| 字段 | 取值与含义 |
|---|---|
| `app_user.role` | `READER` 读者；`AUTHOR` 作者；`ADMIN` 管理员 |
| `app_user.status` | `ACTIVE` 正常；`LOCKED` 锁定；`DISABLED` 禁用 |
| `chapter_subscription.status` | `ACTIVE` 生效中；`REFUNDED` 已退款 |
| `reward_tip.status` | `SUCCESS` 成功；`REFUNDED` 已退款 |
| `account_transaction.biz_type` | `RECHARGE` 充值；`SUBSCRIBE` 订阅；`TIP_OUT` 打赏支出；`TIP_IN` 打赏收入；`REFUND` 退款；`WITHDRAW` 提现；`ADJUST_IN` 后台入账；`ADJUST_OUT` 后台扣减 |
| `account_transaction.direction` | `IN` 收入；`OUT` 支出 |
| `account_transaction.related_type` | `CHAPTER` 章节；`TIP` 打赏；`RECHARGE_ORDER` 充值单；`WITHDRAW_ORDER` 提现单；`SYSTEM` 系统 |

### 1.6 交易、幂等与流水规则

- **余额只能通过流水变更**：`user_account.balance` 与 `account_transaction` 在同一事务内更新，流水只追加，不允许修改或删除。
- **订阅**：写 `chapter_subscription` 一条，扣减余额，写一条 `SUBSCRIBE / OUT` 流水，`biz_key = SUB:{userId}:{chapterId}`。
- **打赏**：写 `reward_tip` 一条，扣减打赏者余额并增加作者余额，写 `TIP_OUT / OUT`、`TIP_IN / IN` 两条流水，`biz_key = TIP_OUT:{tipId}`、`TIP_IN:{tipId}`。
- **充值**：课程设计范围内为模拟充值，不接入第三方支付；写一条 `RECHARGE / IN` 流水，`biz_key = RECHARGE:{requestNo}`。
- **幂等**：订阅、打赏、充值的写接口都必须提交 `requestNo`（≤64 位，建议前端使用「时间戳-随机串」）。同一 `requestNo` 重复提交时返回首次结果，不重复扣款：
  - 订阅：以 `chapter_subscription.request_no` 唯一键兜底；同一用户对同一章节重复订阅直接返回已有订阅结果。
  - 打赏：以 `reward_tip.request_no` 唯一键兜底。
  - 充值：以 `account_transaction.biz_key` 唯一键兜底。
  - 若 `requestNo` 已被同一用户的其他业务占用，返回 `40808`（HTTP 409）。
- **并发**：账户扣款前对 `user_account` 执行 `SELECT ... FOR UPDATE`；打赏涉及两个账户时按 `userId` 升序加锁，避免死锁。
- **余额不足**：返回 `40803`（HTTP 400），`message` 为可展示提示，调用方引导用户充值。
- **账号状态**：`status != ACTIVE` 的账号不能订阅、打赏或充值，返回 `40807`（HTTP 403）。

### 1.7 可见性规则

| 对象 | 可交易条件 |
|---|---|
| 作品 | `novel.status IN (0,1)` 且 `novel.audit_status = 1` |
| 章节 | 所属作品可见，且 `chapter.status = 1`、`chapter.audit_status = 1`、`chapter.publish_time IS NOT NULL` |
| 可订阅章节 | 在章节可见基础上，`chapter.is_vip = 1` 且 `chapter.vip_price > 0` |

A 模块对 B 模块的 `novel`、`chapter` 表**只读**，不写入、不修改 B 的数据。

## 2. 接口清单

| 序号 | 方法 | 路径 | 鉴权 | 说明 | 数据来源 |
|---:|---|---|---|---|---|
| 1 | POST | `/api/auth/register` | 公开 | 读者注册 | 真实数据库（`app_user`、`user_account`） |
| 2 | POST | `/api/auth/login` | 公开 | 登录并签发 JWT | 真实数据库 |
| 3 | GET | `/api/auth/me` | 登录用户 | 当前登录用户信息 | 真实数据库 |
| 4 | GET | `/api/user/admin/users` | 管理员 | 用户列表（角色权限验证） | 真实数据库 |
| 5 | GET | `/api/user/bookshelf` | 登录用户 | 我的书架列表 | 真实数据库 |
| 6 | POST | `/api/user/bookshelf` | 登录用户 | 加入书架 | 真实数据库 |
| 7 | GET | `/api/user/bookshelf/{novelId}` | 登录用户 | 书架状态查询 | 真实数据库 |
| 8 | PUT | `/api/user/bookshelf/{novelId}` | 登录用户 | 置顶 / 更新提醒设置 | 真实数据库 |
| 9 | PUT | `/api/user/bookshelf/{novelId}/reading` | 登录用户 | 更新最近阅读进度 | 真实数据库 |
| 10 | DELETE | `/api/user/bookshelf/{novelId}` | 登录用户 | 移出书架 | 真实数据库 |
| 11 | GET | `/api/user/subscription/chapter/{chapterId}` | 登录用户 | 章节订阅信息 | 真实数据库 |
| 12 | POST | `/api/user/subscription` | 登录用户 | 订阅 VIP 章节 | 真实数据库（后端事务） |
| 13 | GET | `/api/user/subscription` | 登录用户 | 我的订阅记录 | 真实数据库 |
| 14 | GET | `/api/user/subscription/check` | 登录用户 | 章节订阅校验（供 B 阅读页） | 真实数据库 |
| 15 | GET | `/api/user/reward/novel/{novelId}` | 登录用户 | 打赏页作品信息 | 真实数据库 |
| 16 | POST | `/api/user/reward` | 登录用户 | 作品/章节打赏 | 真实数据库（后端事务） |
| 17 | GET | `/api/user/reward/sent` | 登录用户 | 我发出的打赏 | 真实数据库 |
| 18 | GET | `/api/user/reward/received` | 登录用户 | 我收到的打赏 | 真实数据库 |
| 19 | GET | `/api/user/account` | 登录用户 | 账户余额 | 真实数据库 |
| 20 | POST | `/api/user/account/recharge` | 登录用户 | 模拟充值 | 真实数据库（后端事务） |
| 21 | GET | `/api/user/account/transactions` | 登录用户 | 账户流水 | 真实数据库 |

## 3. 错误码

### 3.1 公共错误码

| HTTP 状态 | 业务码 | 触发条件 | message 示例 | 调用方处理 |
|---|---:|---|---|---|
| 400 | 40000 | 缺少必填参数、长度/格式/取值校验失败 | `pageSize: 必须在 1～100 之间` | 修正输入后重试 |
| 401 | 40100 | 未携带令牌、令牌格式错误或已失效 | `未登录或登录已失效` | 清理本地令牌并跳转登录 |
| 401 | 40101 | 用户名或密码错误 | `用户名或密码错误` | 提示重新输入 |
| 403 | 40300 | 已登录但角色不足，或账号被锁定/禁用 | `无权执行该操作` | 停止重试，提示权限不足 |
| 404 | 40400 | 请求路径不存在 | `接口不存在` | 检查请求地址 |
| 409 | 40900 | 唯一键冲突（用户名重复、并发重复提交） | `用户名已被注册` | 修正后重试，不盲目重试 |
| 500 | 50000 | 未处理服务器异常 | `服务器内部错误` | 记录问题并联系后端，不展示堆栈 |

### 3.2 A 模块业务码

| HTTP 状态 | 业务码 | 触发条件 | message 示例 | 调用方处理 |
|---|---:|---|---|---|
| 404 | 40801 | 作品不存在、未过审或已下架 | `作品不存在或已下架` | 返回书城或刷新作品信息 |
| 404 | 40802 | 章节不存在、未发布、未过审、已下架，或不属于指定作品 | `章节不存在或暂不可购买` | 刷新章节目录 |
| 400 | 40803 | 可用余额小于应付金额 | `余额不足，请先充值` | 引导充值后再试 |
| 400 | 40804 | 对免费章节发起订阅 | `该章节为免费章节，无需订阅` | 直接阅读，不调用订阅接口 |
| 400 | 40806 | 打赏自己创作的作品 | `不能打赏自己的作品` | 前端隐藏打赏入口 |
| 403 | 40807 | 账号状态为 `LOCKED` / `DISABLED` 时发起交易 | `账号已被锁定或禁用，无法进行交易` | 停止交易并提示联系管理员 |
| 409 | 40808 | `requestNo` 已被同一用户的其他业务使用 | `requestNo 已被其他订阅请求使用` | 更换 `requestNo` 或查询原结果 |
| 404 | 40809 | 书架中不存在该作品 | `书架中不存在该作品` | 刷新书架列表 |

说明：`408xx` 为 A 模块编号段，HTTP 状态码按语义单独设置；调用方以 HTTP 状态 + `code` 共同判断。

## 4. 接口详情

### 4.1 读者注册

#### 基本信息

| 项目 | 内容 |
|---|---|
| 接口名称 | 读者注册 |
| 请求地址 | `/api/auth/register` |
| 请求方式 | POST |
| 接口说明 | 创建读者账号并初始化余额账户，注册后需登录获取令牌 |
| 鉴权要求 | 公开 |
| 内容类型 | `application/json` |
| 数据来源 | 真实数据库（`app_user`、`user_account`，同一事务） |
| 幂等要求 | 用户名唯一；重复用户名返回 `40900` |
| 事务要求 | 用户与账户必须同时成功或回滚 |

#### 请求参数

| 字段 | 类型 | 必填 | 示例 | 校验与说明 |
|---|---|---|---|---|
| `username` | String | 是 | `"reader01"` | 3～50 位，仅字母、数字、下划线，全局唯一 |
| `password` | String | 是 | `"reader123"` | 6～64 位，使用 bcrypt 存储 |
| `nickname` | String | 否 | `"测试读者"` | ≤50 位，为空时默认与用户名一致 |
| `email` | String | 否 | `"reader01@novel.local"` | 合法邮箱，≤254 位，可空且唯一 |
| `phone` | String | 否 | `"13800138000"` | 11 位手机号，可为空字符串 |

#### 成功响应

| 字段 | 类型 | 必返 | 说明 |
|---|---|---|---|
| `data.userId` | String | 是 | 新用户 ID |
| `data.username` | String | 是 | 登录名 |
| `data.nickname` | String | 是 | 昵称 |
| `data.role` | String | 是 | 固定为 `READER` |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "userId": "2",
    "username": "reader01",
    "nickname": "测试读者",
    "role": "READER"
  }
}
```

#### 错误场景

| HTTP 状态 | 业务码 | 场景 |
|---|---:|---|
| 400 | 40000 | 用户名、密码、昵称、邮箱或手机号格式不合法 |
| 409 | 40900 | 用户名已被注册 |

### 4.2 登录

#### 基本信息

| 项目 | 内容 |
|---|---|
| 接口名称 | 登录并签发 JWT |
| 请求地址 | `/api/auth/login` |
| 请求方式 | POST |
| 接口说明 | 校验用户名密码，返回令牌与用户信息；读者、作者、管理员共用 |
| 鉴权要求 | 公开 |
| 内容类型 | `application/json` |
| 数据来源 | 真实数据库（`app_user`） |
| 幂等要求 | 幂等，可重复登录 |
| 事务要求 | 更新 `last_login_at` 单表写入 |

#### 请求参数

| 字段 | 类型 | 必填 | 示例 | 校验与说明 |
|---|---|---|---|---|
| `username` | String | 是 | `"reader"` | 登录名 |
| `password` | String | 是 | `"reader123"` | 明文密码，服务端 bcrypt 比对 |

#### 成功响应

| 字段 | 类型 | 必返 | 说明 |
|---|---|---|---|
| `data.token` | String | 是 | JWT |
| `data.tokenType` | String | 是 | 固定为 `Bearer` |
| `data.expiresIn` | Long | 是 | 有效期秒数，默认 43200 |
| `data.user.userId` | String | 是 | 用户 ID |
| `data.user.username` | String | 是 | 登录名 |
| `data.user.nickname` | String | 是 | 昵称 |
| `data.user.avatarUrl` | String | 否 | 头像地址，可为 `null` |
| `data.user.role` | String | 是 | `READER` / `AUTHOR` / `ADMIN` |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 43200,
    "user": {
      "userId": "2",
      "username": "reader",
      "nickname": "测试读者",
      "avatarUrl": null,
      "role": "READER"
    }
  }
}
```

#### 错误场景

| HTTP 状态 | 业务码 | 场景 |
|---|---:|---|
| 400 | 40000 | 用户名或密码为空 |
| 401 | 40101 | 用户名不存在或密码错误（不区分，防枚举） |
| 403 | 40300 | 账号状态为 `LOCKED` / `DISABLED` |

### 4.3 当前登录用户信息

#### 基本信息

| 项目 | 内容 |
|---|---|
| 接口名称 | 当前登录用户信息 |
| 请求地址 | `/api/auth/me` |
| 请求方式 | GET |
| 接口说明 | 页面初始化时校验令牌有效性并回显用户信息 |
| 鉴权要求 | 登录用户 |
| 内容类型 | 无 |
| 数据来源 | 真实数据库（`app_user`） |
| 幂等要求 | 幂等 |
| 事务要求 | 只读 |

#### 请求参数

无路径参数、无 Query 参数、无请求体；需携带 `Authorization` 请求头。

#### 成功响应

| 字段 | 类型 | 必返 | 说明 |
|---|---|---|---|
| `data.userId` | String | 是 | 用户 ID |
| `data.username` | String | 是 | 登录名 |
| `data.nickname` | String | 是 | 昵称 |
| `data.avatarUrl` | String | 否 | 头像地址 |
| `data.role` | String | 是 | 角色 |

#### 错误场景

| HTTP 状态 | 业务码 | 场景 |
|---|---:|---|
| 401 | 40100 | 未携带令牌、令牌无效或用户已被删除/禁用 |

### 4.4 用户列表（管理员）

#### 基本信息

| 项目 | 内容 |
|---|---|
| 接口名称 | 用户列表 |
| 请求地址 | `/api/user/admin/users` |
| 请求方式 | GET |
| 接口说明 | 管理端查看平台用户；同时用于验证 JWT 角色权限区分 |
| 鉴权要求 | 管理员（`role=ADMIN`） |
| 内容类型 | 无 |
| 数据来源 | 真实数据库（`app_user`） |
| 幂等要求 | 幂等 |
| 事务要求 | 只读 |

#### 请求参数

| 参数 | 类型 | 必填 | 默认值 | 示例 | 校验与说明 |
|---|---|---|---|---|---|
| `page` | Integer | 否 | 1 | 1 | 从 1 开始 |
| `pageSize` | Integer | 否 | 10 | 10 | 1～100 |
| `keyword` | String | 否 | 无 | `"reader"` | 模糊匹配用户名或昵称 |

#### 成功响应

分页结构，`list` 元素字段：

| 字段 | 类型 | 必返 | 说明 |
|---|---|---|---|
| `userId` | String | 是 | 用户 ID |
| `username` | String | 是 | 登录名 |
| `nickname` | String | 是 | 昵称 |
| `email` | String | 否 | 邮箱 |
| `role` | String | 是 | 角色 |
| `status` | String | 是 | 账号状态 |
| `createdAt` | String | 是 | 注册时间 |

#### 错误场景

| HTTP 状态 | 业务码 | 场景 |
|---|---:|---|
| 401 | 40100 | 未登录 |
| 403 | 40300 | 非管理员访问 |

### 4.5 我的书架列表

#### 基本信息

| 项目 | 内容 |
|---|---|
| 接口名称 | 我的书架列表 |
| 请求地址 | `/api/user/bookshelf` |
| 请求方式 | GET |
| 接口说明 | 按置顶、更新时间倒序返回当前用户收藏的作品 |
| 鉴权要求 | 登录用户 |
| 内容类型 | 无 |
| 数据来源 | 真实数据库（`user_bookshelf`、`novel`、`chapter`，B 表只读） |
| 幂等要求 | 幂等 |
| 事务要求 | 只读 |

#### 请求参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `page` | Integer | 否 | 1 | 从 1 开始 |
| `pageSize` | Integer | 否 | 10 | 1～100 |

#### 成功响应

分页结构，`list` 元素字段：

| 字段 | 类型 | 必返 | 说明 |
|---|---|---|---|
| `novelId` | String | 是 | 作品 ID |
| `title` | String | 是 | 书名 |
| `coverUrl` | String | 否 | 封面 |
| `status` | Integer | 是 | 作品状态（0 连载 / 1 完结 / 2 下架） |
| `totalWords` | Integer | 是 | 总字数 |
| `latestChapterId` | String | 否 | 最新章节 ID |
| `latestChapterTitle` | String | 否 | 最新章节标题 |
| `lastReadChapterId` | String | 否 | 最近阅读章节 ID |
| `lastReadTitle` | String | 否 | 最近阅读章节标题 |
| `lastReadAt` | String | 否 | 最近阅读时间 |
| `isTop` | Boolean | 是 | 是否置顶 |
| `isNotify` | Boolean | 是 | 是否接收更新提醒 |
| `updatedAt` | String | 是 | 书架更新时间 |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "novelId": "1001",
        "title": "星海归途",
        "coverUrl": null,
        "status": 0,
        "totalWords": 7200,
        "latestChapterId": "2002",
        "latestChapterTitle": "第二章 折跃点",
        "lastReadChapterId": "2001",
        "lastReadTitle": "第一章 废墟信号",
        "lastReadAt": "2026-11-25T10:00:00+08:00",
        "isTop": true,
        "isNotify": true,
        "updatedAt": "2026-11-25T10:00:00+08:00"
      }
    ],
    "total": 1,
    "page": 1,
    "pageSize": 10
  }
}
```

### 4.6 加入书架

#### 基本信息

| 项目 | 内容 |
|---|---|
| 接口名称 | 加入书架 |
| 请求地址 | `/api/user/bookshelf` |
| 请求方式 | POST |
| 接口说明 | 收藏公开作品；重复加入按幂等成功处理 |
| 鉴权要求 | 登录用户 |
| 内容类型 | `application/json` |
| 数据来源 | 真实数据库（`user_bookshelf`，`novel` 只读） |
| 幂等要求 | 幂等：`(user_id, novel_id)` 已存在时直接成功 |
| 事务要求 | 单表写入 |

#### 请求参数

| 字段 | 类型 | 必填 | 示例 | 校验与说明 |
|---|---|---|---|---|
| `novelId` | String | 是 | `"1001"` | 必须为正整数，作品需公开可见 |

#### 成功响应

`data` 为 `null`。

#### 错误场景

| HTTP 状态 | 业务码 | 场景 |
|---|---:|---|
| 400 | 40000 | `novelId` 为空或非数字 |
| 404 | 40801 | 作品不存在、未过审或已下架 |

### 4.7 书架状态查询

#### 基本信息

| 项目 | 内容 |
|---|---|
| 接口名称 | 书架状态查询 |
| 请求地址 | `/api/user/bookshelf/{novelId}` |
| 请求方式 | GET |
| 接口说明 | 作品详情页判断是否已收藏及阅读进度 |
| 鉴权要求 | 登录用户 |
| 内容类型 | 无 |
| 数据来源 | 真实数据库（`user_bookshelf`） |
| 幂等要求 | 幂等 |
| 事务要求 | 只读 |

#### 请求参数

| 参数 | 类型 | 必填 | 示例 | 说明 |
|---|---|---|---|---|
| `novelId` | String | 是 | `"1001"` | 作品 ID |

#### 成功响应

| 字段 | 类型 | 必返 | 说明 |
|---|---|---|---|
| `data.inShelf` | Boolean | 是 | 是否已在书架 |
| `data.isTop` | Boolean | 否 | 未收藏时为 `null` |
| `data.isNotify` | Boolean | 否 | 未收藏时为 `null` |
| `data.lastReadChapterId` | String | 否 | 最近阅读章节 ID |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "inShelf": true,
    "isTop": false,
    "isNotify": true,
    "lastReadChapterId": "2001"
  }
}
```

### 4.8 书架设置（置顶 / 更新提醒）

#### 基本信息

| 项目 | 内容 |
|---|---|
| 接口名称 | 书架设置 |
| 请求地址 | `/api/user/bookshelf/{novelId}` |
| 请求方式 | PUT |
| 接口说明 | 修改置顶或更新提醒开关，至少提交一项 |
| 鉴权要求 | 登录用户 |
| 内容类型 | `application/json` |
| 数据来源 | 真实数据库（`user_bookshelf`） |
| 幂等要求 | 幂等，重复提交同一值结果一致 |
| 事务要求 | 单表写入 |

#### 请求参数

| 字段 | 类型 | 必填 | 示例 | 校验与说明 |
|---|---|---|---|---|
| `isTop` | Boolean | 否 | `true` | 是否置顶 |
| `isNotify` | Boolean | 否 | `false` | 是否接收更新提醒 |

`isTop` 与 `isNotify` 至少提交一项，否则返回 `40000`。

#### 成功响应

`data` 为 `null`。

#### 错误场景

| HTTP 状态 | 业务码 | 场景 |
|---|---:|---|
| 400 | 40000 | 两项均未提交 |
| 404 | 40809 | 书架中不存在该作品 |

### 4.9 更新最近阅读进度

#### 基本信息

| 项目 | 内容 |
|---|---|
| 接口名称 | 更新最近阅读进度 |
| 请求地址 | `/api/user/bookshelf/{novelId}/reading` |
| 请求方式 | PUT |
| 接口说明 | 阅读页翻章时由 B 阅读页调用，记录最近阅读章节与时间 |
| 鉴权要求 | 登录用户 |
| 内容类型 | `application/json` |
| 数据来源 | 真实数据库（`user_bookshelf`，`chapter` 只读） |
| 幂等要求 | 幂等，重复提交结果一致 |
| 事务要求 | 单表写入 |

#### 请求参数

| 字段 | 类型 | 必填 | 示例 | 校验与说明 |
|---|---|---|---|---|
| `chapterId` | String | 是 | `"2001"` | 必须属于 `novelId` 对应作品 |

#### 成功响应

`data` 为 `null`。

#### 错误场景

| HTTP 状态 | 业务码 | 场景 |
|---|---:|---|
| 404 | 40809 | 书架中不存在该作品 |
| 404 | 40802 | 章节不存在或不属于该作品 |

### 4.10 移出书架

#### 基本信息

| 项目 | 内容 |
|---|---|
| 接口名称 | 移出书架 |
| 请求地址 | `/api/user/bookshelf/{novelId}` |
| 请求方式 | DELETE |
| 接口说明 | 删除书架记录，不影响订阅与打赏记录 |
| 鉴权要求 | 登录用户 |
| 内容类型 | 无 |
| 数据来源 | 真实数据库（`user_bookshelf`） |
| 幂等要求 | 重复删除返回 `40809` |
| 事务要求 | 单表写入 |

#### 请求参数

| 参数 | 类型 | 必填 | 示例 | 说明 |
|---|---|---|---|---|
| `novelId` | String | 是 | `"1001"` | 作品 ID |

#### 成功响应

`data` 为 `null`。

#### 错误场景

| HTTP 状态 | 业务码 | 场景 |
|---|---:|---|
| 404 | 40809 | 书架中不存在该作品 |

### 4.11 章节订阅信息

#### 基本信息

| 项目 | 内容 |
|---|---|
| 接口名称 | 章节订阅信息 |
| 请求地址 | `/api/user/subscription/chapter/{chapterId}` |
| 请求方式 | GET |
| 接口说明 | 订阅页展示作品、章节、价格及当前用户是否已订阅 |
| 鉴权要求 | 登录用户 |
| 内容类型 | 无 |
| 数据来源 | 真实数据库（`chapter`、`novel` 只读，`chapter_subscription`） |
| 幂等要求 | 幂等 |
| 事务要求 | 只读 |

#### 请求参数

| 参数 | 类型 | 必填 | 示例 | 说明 |
|---|---|---|---|---|
| `chapterId` | String | 是 | `"2002"` | 章节 ID |

#### 成功响应

| 字段 | 类型 | 必返 | 说明 |
|---|---|---|---|
| `data.chapterId` | String | 是 | 章节 ID |
| `data.novelId` | String | 是 | 作品 ID |
| `data.novelTitle` | String | 是 | 书名 |
| `data.chapterTitle` | String | 是 | 章节标题 |
| `data.chapterNo` | Integer | 是 | 章节序号 |
| `data.isVip` | Integer | 是 | 0 免费 / 1 VIP |
| `data.vipPrice` | Number | 是 | 订阅价格（元） |
| `data.subscribed` | Boolean | 是 | 当前用户是否已订阅 |
| `data.subscribedAt` | String | 否 | 订阅时间，未订阅为 `null` |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "chapterId": "2002",
    "novelId": "1001",
    "novelTitle": "星海归途",
    "chapterTitle": "第二章 折跃点",
    "chapterNo": 2,
    "isVip": 1,
    "vipPrice": 12.00,
    "subscribed": false,
    "subscribedAt": null
  }
}
```

#### 错误场景

| HTTP 状态 | 业务码 | 场景 |
|---|---:|---|
| 404 | 40802 | 章节不存在、未发布、未过审、已下架或所属作品不可见 |

### 4.12 订阅 VIP 章节

#### 基本信息

| 项目 | 内容 |
|---|---|
| 接口名称 | 订阅 VIP 章节 |
| 请求地址 | `/api/user/subscription` |
| 请求方式 | POST |
| 接口说明 | 按章节标价扣减余额，生成订阅记录与支出流水 |
| 鉴权要求 | 登录用户 |
| 内容类型 | `application/json` |
| 数据来源 | 真实数据库（`chapter_subscription`、`user_account`、`account_transaction`） |
| 幂等要求 | `requestNo` 幂等；同章节已订阅直接返回首次结果 |
| 事务要求 | 订阅记录、余额、流水同一事务；扣款前 `SELECT ... FOR UPDATE` 锁定账户 |

#### 请求参数

| 字段 | 类型 | 必填 | 示例 | 校验与说明 |
|---|---|---|---|---|
| `novelId` | String | 是 | `"1001"` | 必须与章节所属作品一致 |
| `chapterId` | String | 是 | `"2002"` | 必须为可见的 VIP 章节 |
| `requestNo` | String | 是 | `"1764036000000-a1b2c3d4"` | ≤64 位，幂等键 |

#### 成功响应

| 字段 | 类型 | 必返 | 说明 |
|---|---|---|---|
| `data.chapterId` | String | 是 | 章节 ID |
| `data.novelId` | String | 是 | 作品 ID |
| `data.paidAmount` | Number | 是 | 实付金额 |
| `data.balance` | Number | 是 | 扣款后余额 |
| `data.subscribedAt` | String | 是 | 订阅时间 |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "chapterId": "2002",
    "novelId": "1001",
    "paidAmount": 12.00,
    "balance": 88.00,
    "subscribedAt": "2026-11-25T10:05:00+08:00"
  }
}
```

#### 错误场景

| HTTP 状态 | 业务码 | 场景 |
|---|---:|---|
| 400 | 40000 | 参数缺失、ID 非法或 `requestNo` 超长 |
| 404 | 40802 | 章节不存在、不可购买或 `novelId` 与章节不匹配 |
| 400 | 40804 | 章节为免费章节 |
| 400 | 40803 | 余额不足 |
| 403 | 40807 | 账号被锁定或禁用 |
| 409 | 40808 | `requestNo` 已被同一用户的其他订阅占用 |

### 4.13 我的订阅记录

#### 基本信息

| 项目 | 内容 |
|---|---|
| 接口名称 | 我的订阅记录 |
| 请求地址 | `/api/user/subscription` |
| 请求方式 | GET |
| 接口说明 | 按订阅时间倒序分页返回当前用户订阅记录 |
| 鉴权要求 | 登录用户 |
| 内容类型 | 无 |
| 数据来源 | 真实数据库（`chapter_subscription`、`novel`、`chapter`） |
| 幂等要求 | 幂等 |
| 事务要求 | 只读 |

#### 请求参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `page` | Integer | 否 | 1 | 从 1 开始 |
| `pageSize` | Integer | 否 | 10 | 1～100 |

#### 成功响应

分页结构，`list` 元素字段：

| 字段 | 类型 | 必返 | 说明 |
|---|---|---|---|
| `chapterId` | String | 是 | 章节 ID |
| `novelId` | String | 是 | 作品 ID |
| `novelTitle` | String | 是 | 书名 |
| `chapterTitle` | String | 是 | 章节标题 |
| `chapterNo` | Integer | 是 | 章节序号 |
| `unitPrice` | Number | 是 | 订阅时标价 |
| `paidAmount` | Number | 是 | 实付金额 |
| `status` | String | 是 | `ACTIVE` / `REFUNDED` |
| `subscribedAt` | String | 是 | 订阅时间 |

### 4.14 章节订阅校验

#### 基本信息

| 项目 | 内容 |
|---|---|
| 接口名称 | 章节订阅校验 |
| 请求地址 | `/api/user/subscription/check` |
| 请求方式 | GET |
| 接口说明 | 供 B 阅读页在返回 VIP 正文前校验订阅状态 |
| 鉴权要求 | 登录用户 |
| 内容类型 | 无 |
| 数据来源 | 真实数据库（`chapter_subscription`） |
| 幂等要求 | 幂等 |
| 事务要求 | 只读 |

#### 请求参数

| 参数 | 类型 | 必填 | 示例 | 说明 |
|---|---|---|---|---|
| `chapterId` | String | 是 | `"2002"` | 章节 ID |

#### 成功响应

| 字段 | 类型 | 必返 | 说明 |
|---|---|---|---|
| `data.chapterId` | String | 是 | 章节 ID |
| `data.subscribed` | Boolean | 是 | 是否已订阅 |
| `data.subscribedAt` | String | 否 | 订阅时间 |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "chapterId": "2002",
    "subscribed": true,
    "subscribedAt": "2026-11-25T10:05:00+08:00"
  }
}
```

### 4.15 打赏页作品信息

#### 基本信息

| 项目 | 内容 |
|---|---|
| 接口名称 | 打赏页作品信息 |
| 请求地址 | `/api/user/reward/novel/{novelId}` |
| 请求方式 | GET |
| 接口说明 | 打赏页展示书名、封面与作者昵称 |
| 鉴权要求 | 登录用户 |
| 内容类型 | 无 |
| 数据来源 | 真实数据库（`novel`、`app_user` 只读） |
| 幂等要求 | 幂等 |
| 事务要求 | 只读 |

#### 请求参数

| 参数 | 类型 | 必填 | 示例 | 说明 |
|---|---|---|---|---|
| `novelId` | String | 是 | `"1001"` | 作品 ID |

#### 成功响应

| 字段 | 类型 | 必返 | 说明 |
|---|---|---|---|
| `data.novelId` | String | 是 | 作品 ID |
| `data.novelTitle` | String | 是 | 书名 |
| `data.coverUrl` | String | 否 | 封面 |
| `data.authorId` | String | 是 | 作者用户 ID |
| `data.authorName` | String | 是 | 作者昵称，缺失时返回「未知作者」 |

#### 错误场景

| HTTP 状态 | 业务码 | 场景 |
|---|---:|---|
| 404 | 40801 | 作品不存在、未过审或已下架 |

### 4.16 作品打赏

#### 基本信息

| 项目 | 内容 |
|---|---|
| 接口名称 | 作品打赏 |
| 请求地址 | `/api/user/reward` |
| 请求方式 | POST |
| 接口说明 | 扣减打赏者余额、增加作者余额，记录打赏单与两条流水 |
| 鉴权要求 | 登录用户 |
| 内容类型 | `application/json` |
| 数据来源 | 真实数据库（`reward_tip`、`user_account`、`account_transaction`） |
| 幂等要求 | `requestNo` 幂等，重复提交返回首次结果 |
| 事务要求 | 打赏单、两侧余额、两条流水同一事务；两账户按 `userId` 升序加锁 |

#### 请求参数

| 字段 | 类型 | 必填 | 示例 | 校验与说明 |
|---|---|---|---|---|
| `novelId` | String | 是 | `"1001"` | 作品需公开可见，且作者不能是当前用户 |
| `chapterId` | String | 否 | `"2002"` | 选填；填写时必须属于该作品 |
| `amount` | Number | 是 | `10.00` | ≥0.01，最多两位小数 |
| `message` | String | 否 | `"催更！"` | ≤200 位，默认空字符串 |
| `requestNo` | String | 是 | `"1764036100000-e5f6g7h8"` | ≤64 位，幂等键 |

#### 成功响应

| 字段 | 类型 | 必返 | 说明 |
|---|---|---|---|
| `data.tipId` | String | 是 | 打赏单 ID |
| `data.amount` | Number | 是 | 打赏金额 |
| `data.balance` | Number | 是 | 打赏后余额 |
| `data.createdAt` | String | 是 | 打赏时间 |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "tipId": "9",
    "amount": 10.00,
    "balance": 78.00,
    "createdAt": "2026-11-25T10:10:00+08:00"
  }
}
```

#### 错误场景

| HTTP 状态 | 业务码 | 场景 |
|---|---:|---|
| 400 | 40000 | 参数缺失、金额非法或留言超长 |
| 404 | 40801 | 作品不存在、未过审或已下架 |
| 404 | 40802 | 章节不存在或不属于该作品 |
| 400 | 40806 | 打赏自己的作品 |
| 400 | 40803 | 余额不足 |
| 403 | 40807 | 账号被锁定或禁用 |
| 409 | 40808 | `requestNo` 已被其他打赏请求占用 |

### 4.17 我发出的打赏

#### 基本信息

| 项目 | 内容 |
|---|---|
| 接口名称 | 我发出的打赏 |
| 请求地址 | `/api/user/reward/sent` |
| 请求方式 | GET |
| 接口说明 | 按打赏时间倒序分页返回当前用户发出的打赏 |
| 鉴权要求 | 登录用户 |
| 内容类型 | 无 |
| 数据来源 | 真实数据库（`reward_tip`、`novel`、`chapter`、`app_user`） |
| 幂等要求 | 幂等 |
| 事务要求 | 只读 |

#### 请求参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `page` | Integer | 否 | 1 | 从 1 开始 |
| `pageSize` | Integer | 否 | 10 | 1～100 |

#### 成功响应

分页结构，`list` 元素字段：

| 字段 | 类型 | 必返 | 说明 |
|---|---|---|---|
| `tipId` | String | 是 | 打赏单 ID |
| `novelId` | String | 是 | 作品 ID |
| `novelTitle` | String | 是 | 书名 |
| `chapterId` | String | 否 | 章节 ID，整本打赏为 `null` |
| `chapterTitle` | String | 否 | 章节标题 |
| `fromUserId` | String | 是 | 打赏者 ID |
| `fromNickname` | String | 是 | 打赏者昵称 |
| `toUserId` | String | 是 | 作者 ID |
| `toNickname` | String | 是 | 作者昵称 |
| `amount` | Number | 是 | 金额 |
| `message` | String | 是 | 留言，无则为空字符串 |
| `status` | String | 是 | `SUCCESS` / `REFUNDED` |
| `createdAt` | String | 是 | 打赏时间 |

### 4.18 我收到的打赏

#### 基本信息

| 项目 | 内容 |
|---|---|
| 接口名称 | 我收到的打赏 |
| 请求地址 | `/api/user/reward/received` |
| 请求方式 | GET |
| 接口说明 | 作者视角查看收到的打赏 |
| 鉴权要求 | 登录用户 |
| 内容类型 | 无 |
| 数据来源 | 真实数据库（`reward_tip`、`novel`、`chapter`、`app_user`） |
| 幂等要求 | 幂等 |
| 事务要求 | 只读 |

#### 请求参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `page` | Integer | 否 | 1 | 从 1 开始 |
| `pageSize` | Integer | 否 | 10 | 1～100 |

#### 成功响应

与 4.17 字段一致，`list` 为当前用户作为作者的收款记录。

### 4.19 账户余额

#### 基本信息

| 项目 | 内容 |
|---|---|
| 接口名称 | 账户余额 |
| 请求地址 | `/api/user/account` |
| 请求方式 | GET |
| 接口说明 | 余额页与订阅/打赏前的余额展示 |
| 鉴权要求 | 登录用户 |
| 内容类型 | 无 |
| 数据来源 | 真实数据库（`user_account`） |
| 幂等要求 | 幂等 |
| 事务要求 | 只读 |

#### 请求参数

无。

#### 成功响应

| 字段 | 类型 | 必返 | 说明 |
|---|---|---|---|
| `data.userId` | String | 是 | 用户 ID |
| `data.balance` | Number | 是 | 可用余额（元） |
| `data.updatedAt` | String | 是 | 余额最近更新时间 |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "userId": "2",
    "balance": 88.00,
    "updatedAt": "2026-11-25T10:05:00+08:00"
  }
}
```

### 4.20 模拟充值

#### 基本信息

| 项目 | 内容 |
|---|---|
| 接口名称 | 模拟充值 |
| 请求地址 | `/api/user/account/recharge` |
| 请求方式 | POST |
| 接口说明 | 课程设计范围为余额充值，不接入第三方支付，直接写入 `RECHARGE` 流水 |
| 鉴权要求 | 登录用户 |
| 内容类型 | `application/json` |
| 数据来源 | 真实数据库（`user_account`、`account_transaction`） |
| 幂等要求 | `requestNo` 幂等，重复提交返回首次结果 |
| 事务要求 | 余额与流水同一事务；扣款前锁定账户行 |

#### 请求参数

| 字段 | 类型 | 必填 | 示例 | 校验与说明 |
|---|---|---|---|---|
| `amount` | Number | 是 | `50.00` | ≥0.01，最多两位小数 |
| `requestNo` | String | 是 | `"1764036200000-i9j0k1l2"` | ≤64 位，幂等键 |

#### 成功响应

| 字段 | 类型 | 必返 | 说明 |
|---|---|---|---|
| `data.amount` | Number | 是 | 本次充值金额 |
| `data.balance` | Number | 是 | 充值后余额 |
| `data.txnNo` | String | 是 | 流水号（32 位） |
| `data.createdAt` | String | 是 | 记账时间 |

#### 错误场景

| HTTP 状态 | 业务码 | 场景 |
|---|---:|---|
| 400 | 40000 | 金额缺失、≤0 或超过两位小数 |
| 403 | 40807 | 账号被锁定或禁用 |

### 4.21 账户流水

#### 基本信息

| 项目 | 内容 |
|---|---|
| 接口名称 | 账户流水 |
| 请求地址 | `/api/user/account/transactions` |
| 请求方式 | GET |
| 接口说明 | 按时间倒序分页查询当前用户全部资金流水，可按业务类型与方向筛选 |
| 鉴权要求 | 登录用户 |
| 内容类型 | 无 |
| 数据来源 | 真实数据库（`account_transaction`） |
| 幂等要求 | 幂等 |
| 事务要求 | 只读 |

#### 请求参数

| 参数 | 类型 | 必填 | 默认值 | 示例 | 校验与说明 |
|---|---|---|---|---|---|
| `page` | Integer | 否 | 1 | 1 | 从 1 开始 |
| `pageSize` | Integer | 否 | 10 | 10 | 1～100 |
| `bizType` | String | 否 | 无 | `"SUBSCRIBE"` | 枚举见 1.5 |
| `direction` | String | 否 | 无 | `"OUT"` | `IN` / `OUT` |

#### 成功响应

分页结构，`list` 元素字段：

| 字段 | 类型 | 必返 | 说明 |
|---|---|---|---|
| `txnNo` | String | 是 | 流水号 |
| `bizType` | String | 是 | 业务类型 |
| `direction` | String | 是 | `IN` / `OUT` |
| `amount` | Number | 是 | 发生额（正数） |
| `balanceBefore` | Number | 是 | 记账前余额 |
| `balanceAfter` | Number | 是 | 记账后余额 |
| `bizKey` | String | 是 | 业务唯一键 |
| `relatedType` | String | 是 | 关联业务类型 |
| `relatedId` | String | 否 | 关联业务 ID |
| `requestNo` | String | 否 | 上游请求号 |
| `remark` | String | 是 | 流水说明 |
| `createdAt` | String | 是 | 记账时间 |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "txnNo": "7d1f2c9b8a4e4f0d9c3b2a1f0e9d8c7b",
        "bizType": "SUBSCRIBE",
        "direction": "OUT",
        "amount": 12.00,
        "balanceBefore": 100.00,
        "balanceAfter": 88.00,
        "bizKey": "SUB:2:2002",
        "relatedType": "CHAPTER",
        "relatedId": "2002",
        "requestNo": "1764036000000-a1b2c3d4",
        "remark": "订阅《星海归途》第2章",
        "createdAt": "2026-11-25T10:05:00+08:00"
      }
    ],
    "total": 1,
    "page": 1,
    "pageSize": 10
  }
}
```

## 5. 前端页面与接口映射

| 页面 | 路由 | 使用接口 |
|---|---|---|
| 登录页 | `/login` | 4.2 登录 |
| 注册页 | `/register` | 4.1 注册 |
| 我的书架 | `/bookshelf` | 4.5、4.6、4.8、4.10 |
| 章节订阅 | `/subscription` | 4.11、4.12、4.13 |
| 作品打赏 | `/reward` | 4.15、4.16、4.17、4.18、4.19 |
| 余额与流水 | `/account` | 4.19、4.20、4.21 |

前端统一通过 `src/api/request.js` 注入 `Authorization`，按 `code` 判定业务结果；`40100` 清理本地令牌并跳转登录页。

## 6. 联调记录

| 日期 | 前端负责人 | 后端负责人 | 测试环境 | 结果 | 问题及处理 |
|---|---|---|---|---|---|
| 2026-11-25 | A | A | 本机（JDK 17 + MySQL 8.0） | 待联调 | 后端编译与启动验证通过；数据库需先执行 B 建表脚本与本模块建表脚本 |

## 7. 变更记录

| 版本 | 日期 | 修改人 | 变更内容 | 影响调用方 |
|---|---|---|---|---|
| v1.0 | 2026-11-25 | A | 完成注册登录、书架、订阅、打赏、余额共 21 个接口契约与实现 | 无（首次发布） |
