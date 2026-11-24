# A 用户与交易 · 接口说明

- 文档版本：v1.0
- 接口负责人：A
- 最近更新：2026-11-24
- 开发状态：开发中（前端构建通过；数据库联调待执行）
- 关联页面：登录注册、我的书架、章节订阅、打赏作者、余额流水
- 技术栈：Vue 3 + Element Plus / Java 17 + Spring Boot + MyBatis-Plus / MySQL 8.0.16+
- 统一响应：`ApiResponse<T>`，成功 `code=0`、`message="success"`；所有 BIGINT ID 在 JSON 中使用字符串

## 1. 通用约定

### 1.1 请求前缀

```text
/api
```

本地开发由 Vite 将 `/api` 代理到 `http://127.0.0.1:8080`。

### 1.2 鉴权

除 `POST /api/auth/register`、`POST /api/auth/login` 和现有 `/api/admin/dashboard/**` Mock 接口外，A 模块接口要求：

```http
Authorization: Bearer <JWT>
```

JWT 载荷包含 `sub`（userId）、`username`、`role`、`iat`、`exp`，默认有效期 86400 秒。  
角色：`READER` 读者、`AUTHOR` 作者、`ADMIN` 管理员。`/api/admin/**`（除 dashboard Mock）要求 `ADMIN`；`/api/user/**` 和 `/api/auth/me` 要求登录。

### 1.3 分页

```json
{
  "list": [],
  "total": 0,
  "page": 1,
  "pageSize": 10
}
```

`page` 从 1 开始；`pageSize` 允许 1–100。GET 查询使用 Query 参数，不使用 JSON 请求体。

### 1.4 错误结构

```json
{
  "code": 40901,
  "message": "账户余额不足",
  "data": null
}
```

## 2. 接口清单

| # | 方法 | 地址 | 鉴权 | 说明 |
|---:|---|---|---|---|
| 1 | POST | `/api/auth/register` | 公开 | 注册读者并自动登录 |
| 2 | POST | `/api/auth/login` | 公开 | 登录并签发 JWT |
| 3 | GET | `/api/auth/me` | 登录 | 当前用户信息 |
| 4 | GET | `/api/user/bookshelf` | 登录 | 分页查询书架 |
| 5 | POST | `/api/user/bookshelf` | 登录 | 加入书架 |
| 6 | PATCH | `/api/user/bookshelf/{novelId}` | 登录 | 修改置顶/更新提醒 |
| 7 | DELETE | `/api/user/bookshelf/{novelId}` | 登录 | 移出书架 |
| 8 | GET | `/api/user/subscriptions` | 登录 | 分页查询订阅记录 |
| 9 | POST | `/api/user/subscriptions` | 登录 | 订阅 VIP 章节（调用存储过程） |
| 10 | GET | `/api/user/tips` | 登录 | 分页查询我发出的打赏 |
| 11 | POST | `/api/user/tips` | 登录 | 打赏作者（调用存储过程） |
| 12 | GET | `/api/user/balance` | 登录 | 余额汇总 |
| 13 | GET | `/api/user/transactions` | 登录 | 分页查询账户流水 |

## 3. 认证接口

### 3.1 注册

```http
POST /api/auth/register
Content-Type: application/json
```

```json
{
  "username": "reader01",
  "password": "reader123",
  "nickname": "小读者",
  "email": "reader01@example.com",
  "phone": ""
}
```

- `username`：3–50 位字母、数字或下划线，唯一
- `password`：8–64 位，后端使用 BCrypt 哈希
- `email`、`phone`：选填；手机号需为 11 位大陆手机号
- 后端在同一事务中插入 `app_user` 和 `user_account`，账户初始余额 0.00

成功 `data`：

```json
{
  "token": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {
    "userId": "1",
    "username": "reader01",
    "nickname": "小读者",
    "avatarUrl": null,
    "role": "READER",
    "status": "ACTIVE"
  }
}
```

### 3.2 登录

```http
POST /api/auth/login
```

```json
{
  "account": "reader01",
  "password": "reader123"
}
```

`account` 支持用户名、邮箱或手机号。成功响应同注册。失败：`40101 账号或密码错误`；账号非 ACTIVE：`40301`。

### 3.3 当前用户

```http
GET /api/auth/me
Authorization: Bearer <jwt>
```

成功 `data` 为 `UserProfile`，字段同注册响应中的 `user`。

## 4. 书架接口

### 4.1 查询书架

```http
GET /api/user/bookshelf?page=1&pageSize=10
```

成功 `data`：

```json
{
  "list": [
    {
      "novelId": "10001",
      "title": "星河拾荒者",
      "coverUrl": null,
      "authorId": "2",
      "authorName": "青崖",
      "lastReadChapterId": "20001",
      "lastReadChapterTitle": "第一章：旧城来信",
      "lastReadAt": "2026-11-24T20:10:00",
      "isTop": false,
      "isNotify": true,
      "chapterCount": 12,
      "createdAt": "2026-11-24T19:30:00"
    }
  ],
  "total": 1,
  "page": 1,
  "pageSize": 10
}
```

书架数据来自 `user_bookshelf`，书名/封面/作者从 B 的 `novel`、`app_user` 读取；`chapterCount` 只统计 `status=1 AND audit_status=1` 的章节。

### 4.2 加入书架

```http
POST /api/user/bookshelf
```

```json
{
  "novelId": "10001"
}
```

- 需要 `novel_id` 存在，否则 `40410`
- 联合主键 `(user_id, novel_id)` 防重复；重复返回 `40920`

### 4.3 修改书架标记

```http
PATCH /api/user/bookshelf/10001
```

```json
{
  "isTop": true,
  "isNotify": false
}
```

`isTop`、`isNotify` 至少提供一个；只更新提供的字段。

### 4.4 移出书架

```http
DELETE /api/user/bookshelf/10001
```

不存在返回 `40411`。

## 5. 订阅接口

### 5.1 查询订阅记录

```http
GET /api/user/subscriptions?page=1&pageSize=10
```

```json
{
  "list": [
    {
      "chapterId": "20001",
      "chapterTitle": "第一章：旧城来信",
      "chapterNo": 1,
      "novelId": "10001",
      "novelTitle": "星河拾荒者",
      "unitPrice": 0.30,
      "paidAmount": 0.30,
      "status": "ACTIVE",
      "subscribedAt": "2026-11-24T20:10:00"
    }
  ],
  "total": 1,
  "page": 1,
  "pageSize": 10
}
```

### 5.2 订阅章节

```http
POST /api/user/subscriptions
```

```json
{
  "chapterId": "20001",
  "requestNo": "sub-20261124-0001"
}
```

- `requestNo` 可选；不传则由后端生成 UUID。重试时应复用同一个值
- 后端调用 `sp_subscribe_chapter(p_user_id, p_chapter_id, p_request_no, ...)`
- 价格由过程读取 `chapter.vip_price`，并校验 `is_vip=1`、`status=1`、`audit_status=1`
- 已订阅返回成功，`alreadySubscribed=true`，不重复扣费

成功 `data`：

```json
{
  "requestNo": "sub-20261124-0001",
  "txnNo": "7b0f...",
  "balanceAfter": 99.70,
  "alreadySubscribed": false,
  "message": "订阅扣费成功"
}
```

错误：余额不足 `40901`；章节不可订阅 `40903`；重复提交 `40904`。

## 6. 打赏接口

### 6.1 查询我的打赏

```http
GET /api/user/tips?page=1&pageSize=10
```

```json
{
  "list": [
    {
      "tipId": "1",
      "tipNo": "8a7c...",
      "novelId": "10001",
      "novelTitle": "星河拾荒者",
      "chapterId": "20001",
      "chapterTitle": "第一章：旧城来信",
      "toUserId": "2",
      "authorName": "青崖",
      "amount": 5.00,
      "message": "写得好",
      "status": "SUCCESS",
      "createdAt": "2026-11-24T20:20:00"
    }
  ],
  "total": 1,
  "page": 1,
  "pageSize": 10
}
```

### 6.2 打赏

```http
POST /api/user/tips
```

```json
{
  "novelId": "10001",
  "chapterId": "20001",
  "amount": 5.00,
  "message": "写得好",
  "requestNo": "tip-20261124-0001"
}
```

- `chapterId` 为空表示整本书打赏
- 后端按 `novel.author_id`（章节打赏再校验 `chapter.novel_id`）解析作者，不能指定非作者
- 调用 `sp_tip_author`，同一事务写打赏、双方余额、TIP_OUT / TIP_IN 两条流水
- 同一 `requestNo` 重复请求返回 `duplicate=true`，不重复扣款

成功 `data`：

```json
{
  "requestNo": "tip-20261124-0001",
  "tipNo": "9c2d...",
  "fromBalanceAfter": 94.70,
  "toBalanceAfter": 105.00,
  "duplicate": false,
  "message": "打赏成功"
}
```

## 7. 余额与流水

### 7.1 余额汇总

```http
GET /api/user/balance
```

```json
{
  "balance": 94.70,
  "totalIncome": 100.00,
  "totalConsumption": 5.30,
  "transactionCount": 3
}
```

`balance` 来自 `user_account.balance`；收入/支出/笔数来自 `account_transaction`。

### 7.2 账户流水

```http
GET /api/user/transactions?page=1&pageSize=10
```

```json
{
  "list": [
    {
      "txnNo": "7b0f...",
      "bizType": "SUBSCRIBE",
      "direction": "OUT",
      "amount": 0.30,
      "balanceBefore": 100.00,
      "balanceAfter": 99.70,
      "relatedType": "CHAPTER",
      "relatedId": "20001",
      "remark": "订阅章节 20001",
      "createdAt": "2026-11-24T20:10:00"
    }
  ],
  "total": 1,
  "page": 1,
  "pageSize": 10
}
```

## 8. 错误码

| HTTP | 业务码 | 触发条件 | 调用方处理 |
|---:|---:|---|---|
| 400 | 40000 | 参数缺失、格式错误、ID 非数字 | 修正输入后重试 |
| 401 | 40100 | 未携带/无效/过期 JWT | 清理本地 token，跳登录 |
| 401 | 40101 | 账号或密码错误 | 提示重新输入 |
| 403 | 40300 | 非管理员访问管理端接口 | 停止操作 |
| 403 | 40301 | 账号被锁定/禁用 | 联系管理员 |
| 404 | 40410 | 小说不存在 | 检查 novelId |
| 404 | 40411 | 书架记录不存在 | 刷新书架 |
| 404 | 40420 | 小说/章节不存在或不属于同一小说 | 检查 ID |
| 404 | 40430 | 用户账户不存在 | 重新登录/检查数据 |
| 409 | 40901 | 余额不足 | 提示充值（本期未实现充值） |
| 409 | 40902 | 章节已订阅 | 刷新订阅记录 |
| 409 | 40903 | 章节不可订阅/交易未完成 | 按 message 处理 |
| 409 | 40904 | 唯一键冲突/重复提交 | 用同一 requestNo 重查 |
| 409 | 40910/40911/40912 | 用户名/邮箱/手机号重复 | 修改注册信息 |
| 409 | 40920 | 小说已在书架 | 刷新书架 |
| 409 | 40900 | 数据库关联/完整性冲突 | 检查关联 ID |
| 500 | 50000 | 未处理异常 | 记录问题，联系后端 |

## 9. 数据库与副作用

- 注册：`app_user` + `user_account` 同事务插入。
- 书架：`user_bookshelf` 增删改；不改余额。
- 订阅：调用 `sp_subscribe_chapter`；过程内部开启/提交事务，写 `chapter_subscription`、扣 `user_account`、写 `account_transaction`。
- 打赏：调用 `sp_tip_author`；过程内部事务，写 `reward_tip`、双方 `user_account`、两条 `account_transaction`。
- 余额/流水：只读 `user_account`、`account_transaction`。
- 订阅与打赏的 `request_no` 是幂等键；前端重试必须复用。

## 10. 前端页面

| 路由 | 页面 | 说明 |
|---|---|---|
| `/login` | 登录注册 | 登录/注册，成功后写 localStorage token |
| `/shelf` | 我的书架 | 分页、加入、移出、置顶、更新提醒 |
| `/subscriptions` | 章节订阅 | 输入 chapterId 订阅、查看订阅记录 |
| `/tips` | 打赏作者 | 输入 novelId/chapterId/金额/留言，查看打赏记录 |
| `/balance` | 余额流水 | 余额、累计收入/支出、分页流水 |
| `/admin/**` | 运营后台 | 保留 D 的现有页面；前端要求 ADMIN token |

## 11. 联调记录

| 日期 | 前端 | 后端 | 环境 | 结果 | 问题 |
|---|---|---|---|---|---|
| 2026-11-24 | A | A | 本地构建 | 前端 Vite 构建通过；后端 Maven 编译通过；JWT 单元测试 2/2 通过；应用可启动，管理员 Mock 接口 200、未登录用户接口 401 | 数据库未连接；现有 Spring MockMvc 测试在 JDK 27 下 Mockito 初始化失败，建议测试环境使用 JDK 17 |

## 12. 变更记录

| 版本 | 日期 | 修改人 | 变更内容 |
|---|---|---|---|
| v1.0 | 2026-11-24 | A | 首版：注册登录、书架、订阅、打赏、余额流水接口 |
