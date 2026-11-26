# A 模块 · 用户与交易接口说明

- 文档版本：v1.0
- 最近更新：2026-11-25
- 开发状态：已完成，待真实 MySQL 环境联调
- 关联页面：`/login`、`/register`、`/account`

## 1. 通用约定

- 基础路径：`/api/user`。
- 成功响应统一为 `{"code":0,"message":"success","data":...}`；失败时 `data=null`。
- 除注册、登录外，所有接口均需请求头 `Authorization: Bearer <token>`。
- 数据库 BIGINT ID 在 JSON 中均使用字符串；金额为十进制数字，最多两位小数。
- 时间字段为 ISO 8601 字符串，东八区示例：`2026-11-25T12:00:00+08:00`。
- 分页查询均支持 `page`（默认 1）和 `pageSize`（默认 10，范围 1～100），返回：
  `{"list":[],"total":0,"page":1,"pageSize":10}`。
- 锁定或停用账号不能登录，也不能调用本模块业务接口。账号状态启停由 D 模块后台接口负责。
- 本模块不提供充值接口；订阅价格由服务端读取章节价格，打赏收款人由服务端根据作品作者确定。

## 2. 接口清单

| 功能 | 方法 | 地址 | 鉴权 | 写入与幂等 |
|---|---|---|---|---|
| 注册并自动登录 | POST | `/api/user/auth/register` | 公开 | 同一事务创建用户和零余额账户；用户名、邮箱、手机号唯一 |
| 登录 | POST | `/api/user/auth/login` | 公开 | 更新最后登录时间 |
| 退出登录 | POST | `/api/user/auth/logout` | 登录 | 无服务端写入，客户端删除本地令牌 |
| 查询资料 | GET | `/api/user/profile` | 登录 | 只读 |
| 修改资料 | PUT | `/api/user/profile` | 登录 | 昵称、头像 URL、邮箱、手机号；邮箱和手机号唯一 |
| 查询余额 | GET | `/api/user/account` | 登录 | 只读 |
| 查询账户流水 | GET | `/api/user/account/transaction` | 登录 | 只读、分页，仅当前用户流水 |
| 查询书架 | GET | `/api/user/bookshelf` | 登录 | 只读、分页 |
| 加入书架 | POST | `/api/user/bookshelf` | 登录 | 可重复调用，不生成重复记录 |
| 移出书架 | DELETE | `/api/user/bookshelf/{novelId}` | 登录 | 可重复调用 |
| 订阅章节 | POST | `/api/user/subscription` | 登录 | 调用冻结存储过程；`requestNo` 幂等，重复订阅不重复扣费 |
| 查询订阅记录 | GET | `/api/user/subscription` | 登录 | 只读、分页 |
| 取消订阅并退款 | DELETE | `/api/user/subscription/{chapterId}` | 登录 | 事务内退款；重复调用不重复退款 |
| 打赏作品或章节 | POST | `/api/user/tip` | 登录 | 调用冻结存储过程；`requestNo` 幂等，双方余额及流水同时成功或回滚 |
| 查询打赏记录 | GET | `/api/user/tip` | 登录 | 只读、分页；支持支出/收入方向 |

## 3. 认证与资料

### 3.1 注册 `POST /api/user/auth/register`

请求体：

| 字段 | 类型 | 必填 | 校验 |
|---|---|---|---|
| `username` | String | 是 | 3～50 位字母、数字或下划线 |
| `password` | String | 是 | 8～72 位，且 UTF-8 不超过 72 字节；服务端 BCrypt 加密 |
| `nickname` | String | 是 | 非空，最长 50 字 |
| `email` | String | 否 | 合法邮箱，最长 254 字；空串按未填写处理 |
| `phone` | String | 否 | 5～20 位数字及 `+ - 空格`；空串按未填写处理 |

```json
{
  "username": "reader_101",
  "password": "password123",
  "nickname": "读者101",
  "email": "reader@example.com",
  "phone": "13800138000"
}
```

成功直接返回登录结果，新用户角色为 `READER`、状态为 `ACTIVE`、余额为 `0.00`。

### 3.2 登录 `POST /api/user/auth/login`

```json
{"username":"reader_101","password":"password123"}
```

注册和登录的 `data` 结构相同：

| 字段 | 类型 | 说明 |
|---|---|---|
| `token` | String | JWT |
| `tokenType` | String | 固定 `Bearer` |
| `expiresIn` | Number | 有效期秒数 |
| `userId` | String | 用户 ID |
| `username` / `nickname` | String | 登录名 / 昵称 |
| `avatarUrl` | String/null | 头像 URL |
| `role` | String | 当前角色，如 `READER`、`AUTHOR`、`ADMIN` |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "eyJ...",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "userId": "101",
    "username": "reader_101",
    "nickname": "读者101",
    "avatarUrl": null,
    "role": "READER"
  }
}
```

### 3.3 退出 `POST /api/user/auth/logout`

无请求体，成功时 `data=null`。JWT 采用无状态退出，客户端必须删除本地 token 和用户信息。

### 3.4 查询资料 `GET /api/user/profile`

`data` 字段：`userId`、`username`、`nickname`、`avatarUrl`、`email`、`phone`、`role`、`status`、`lastLoginAt`、`createdAt`、`updatedAt`。

### 3.5 修改资料 `PUT /api/user/profile`

请求体完整提交以下资料字段；不允许通过本接口修改用户名、角色或账号状态。

```json
{
  "nickname": "新昵称",
  "avatarUrl": "https://example.com/avatar.png",
  "email": "new@example.com",
  "phone": "13900139000"
}
```

`nickname` 必填且最长 50 字，`avatarUrl` 最长 500 字，其余校验同注册。响应为更新后的完整资料。头像通过 URL 管理，本版本不含文件上传接口。

## 4. 账户

### 4.1 余额 `GET /api/user/account`

```json
{
  "code": 0,
  "message": "success",
  "data": {"userId":"101","balance":88.50,"updatedAt":"2026-11-25T12:00:00+08:00"}
}
```

### 4.2 流水 `GET /api/user/account/transaction?page=1&pageSize=10`

`data.list[]` 字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `txnId` / `txnNo` | String | 流水 ID / 全局流水号 |
| `bizType` | String | 业务类型，如 `SUBSCRIBE`、`TIP`、`REFUND` |
| `direction` | String | `IN` 收入或 `OUT` 支出 |
| `amount` | Number | 本次变动金额，非负 |
| `balanceBefore` / `balanceAfter` | Number | 变动前 / 后余额 |
| `relatedType` / `relatedId` | String/null | 关联对象类型 / ID |
| `requestNo` | String/null | 客户端或系统生成的幂等请求号 |
| `remark` | String/null | 流水摘要 |
| `createdAt` | String | 创建时间 |

## 5. 书架

### 5.1 加入 `POST /api/user/bookshelf`

```json
{"novelId":"301"}
```

作品不存在或当前不可见时返回 404。首次添加响应 `added=true`；重复添加返回已有记录且 `added=false`。`removed` 固定为 `false`。

### 5.2 移除 `DELETE /api/user/bookshelf/301`

存在并删除时 `removed=true`；原本不存在时 `removed=false`，均视为成功。此时 `added=false`。

### 5.3 列表 `GET /api/user/bookshelf?page=1&pageSize=10`

`data.list[]` 字段：`novelId`、`title`、`coverUrl`、`authorId`、`authorName`、`novelStatus`、`auditStatus`、`lastReadChapterId`、`lastReadChapterTitle`、`lastReadAt`、`isTop`、`isNotify`、`createdAt`、`updatedAt`、`added`、`removed`。列表项的操作标记 `added`、`removed` 均为 `false`。

## 6. 章节订阅

### 6.1 订阅 `POST /api/user/subscription`

```json
{"chapterId":"501","requestNo":"SUB-8f0d3b1f4f624a8b"}
```

- `chapterId` 必须为正整数 ID；`requestNo` 必填，最长 64 字，仅允许字母、数字、点、下划线、冒号和短横线。
- 客户端应在一次用户操作中生成并复用同一 `requestNo`，不得让每次自动重试生成新号码。
- 仅已发布、审核通过且价格大于零的 VIP 章节可订阅；价格取数据库 `chapter.vip_price`，客户端不能指定金额。
- 调用 `sp_subscribe_chapter`，由过程完成账户行锁、余额扣减、订阅记录和账户流水写入。
- 重复章节订阅或重复请求返回原流水及当前余额，不再次扣款；相同请求号对应不同章节时返回冲突。

成功 `data`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `chapterId` | String | 章节 ID |
| `resultCode` | String | `SUCCESS` 或 `ALREADY_SUBSCRIBED` |
| `txnNo` | String | 本次或原订阅流水号 |
| `paidAmount` | Number | 实际订阅价格 |
| `balanceAfter` | Number | 操作后的账户余额 |
| `duplicate` | Boolean | 是否命中已有订阅 |

### 6.2 订阅记录 `GET /api/user/subscription?page=1&pageSize=10`

`data.list[]` 字段：`chapterId`、`novelId`、`novelTitle`、`chapterNo`、`chapterTitle`、`unitPrice`、`paidAmount`、`txnNo`、`requestNo`、`status`、`subscribedAt`、`refundedAt`、`balanceAfter`、`refunded`。普通列表项后两个操作字段为 `null`。

### 6.3 取消订阅 `DELETE /api/user/subscription/501`

- 仅本人已有订阅可取消；无记录返回 404。
- `ACTIVE` 记录按实际支付金额全额退回，状态改为 `REFUNDED`，写入一条 `REFUND/IN` 流水。
- 应用事务内依次锁定账户和订阅，余额、订阅状态及流水同时成功或回滚。
- 已退款记录重复取消直接返回，不再次退款。
- 受冻结过程现有唯一业务规则限制，已退款章节当前不能再次订阅。

响应结构同订阅记录项，`balanceAfter` 为退款后余额，`refunded=true`。

## 7. 打赏

### 7.1 打赏 `POST /api/user/tip`

```json
{
  "novelId":"301",
  "chapterId":"501",
  "amount":19.00,
  "message":"写得很好",
  "requestNo":"TIP-1f72bc4d0e714f20"
}
```

- `novelId`、`amount`、`requestNo` 必填；`chapterId` 可省略或传 `null`，省略表示打赏作品。
- 金额最低 `0.01`，最多 10 位整数和 2 位小数；留言最长 200 字。
- 章节必须属于该作品；不能给自己打赏；作者账号必须可用。
- 服务端从作品记录解析作者，忽略客户端指定收款人，调用 `sp_tip_author` 同时完成打赏者扣款、作者入账、打赏记录和双方流水。
- 同一 `requestNo` 重试返回原打赏单，不重复变动余额；请求号相同但作品、章节或金额不同则返回冲突。

成功 `data`：`tipId`、`tipNo`、`resultCode`（`SUCCESS` 或 `DUPLICATE_REQUEST`）、`amount`、`balanceAfter`（打赏者余额）、`duplicate`。

### 7.2 打赏记录 `GET /api/user/tip?direction=OUT&page=1&pageSize=10`

- `direction=OUT`：我发出的打赏，默认值；`direction=IN`：我作为作者收到的打赏。
- `data.list[]` 字段：`tipId`、`tipNo`、`requestNo`、`fromUserId`、`toUserId`、`counterpartyName`、`novelId`、`novelTitle`、`chapterId`、`chapterTitle`、`amount`、`message`、`status`、`outTxnNo`、`inTxnNo`、`createdAt`、`refundedAt`、`direction`。

## 8. 错误码

| HTTP | 业务码 | 触发条件 |
|---|---:|---|
| 400 | 40000 | 参数缺失、格式错误、分页越界或 ID 非法 |
| 400 | 41005 | 给自己打赏或收款人不是作品作者 |
| 400 | 41000 | 其他存储过程业务校验失败 |
| 401 | 40100 | 未登录或 token 失效 |
| 401 | 40101 | 用户名或密码错误 |
| 403 | 40301 | 账号或交易相关用户状态不可用 |
| 404 | 41001 | 用户不存在 |
| 404 | 41004 | 作品、章节不存在，或章节不属于作品 |
| 404 | 41008 | 用户账户不存在 |
| 404 | 41009 | 作品不存在或不可加入书架 |
| 404 | 41010 | 订阅记录不存在 |
| 409 | 41002 | 用户名、邮箱或手机号已使用 |
| 409 | 41003 | 账户余额不足 |
| 409 | 41006 | 同一请求号对应的交易参数不一致 |
| 409 | 41007 | 章节当前不可订阅 |
| 500 | 50000 | 未处理服务器异常 |

## 9. 数据库与事务边界

- 用户：`app_user`；账户：`user_account`；流水：`account_transaction`。
- 书架：`user_bookshelf`，只读关联 `novel`、章节及阅读进度数据。
- 订阅：`chapter_subscription`，调用 `sp_subscribe_chapter`，只读关联 `chapter`、`novel`。
- 打赏：`reward_tip`，调用 `sp_tip_author`，只读关联 `novel`、`chapter`。
- 注册和取消订阅由 Spring 事务管理；订阅和打赏过程内部自行管理事务，其 Service 调用方法不叠加 `@Transactional`。
- 未修改任何冻结建表 SQL、存储过程 SQL 或其他模块业务表结构。
