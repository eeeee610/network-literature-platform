# D 运营后台接口说明 v1.0

日期：2026-11-25  
适用模块：D 运营后台  
统一前缀：`/api/admin`

## 1. 通用约定

- 全部接口需要请求头 `Authorization: Bearer <JWT>`，且 JWT 用户角色必须为 `ADMIN`。
- 响应统一为 `{ "code": 0, "message": "success", "data": ... }`；失败时 HTTP 状态码与业务错误码同时返回。
- 分页请求使用 `page`（默认 1）和 `pageSize`（默认 10，最大 100），响应为 `list/total/page/pageSize`。
- 所有 BIGINT ID 在 JSON 中均使用字符串；金额使用十进制数值，单位为元。
- 日期时间输出为带 `+08:00` 的 ISO 8601 格式；日志查询的 `startTime/endTime` 使用 ISO 本地日期时间，如 `2026-11-25T00:00:00`。
- 本模块只读取或更新冻结表，不新增或修改数据库结构。

## 2. 数据统计与后台首页

### 2.1 获取运营统计

- 请求：`GET /api/admin/dashboard/summary`
- 请求参数：无。
- 统计口径：用户、作品、评论为全表数量；订阅仅统计 `chapter_subscription.status=ACTIVE`，金额为 `paid_amount` 合计；打赏仅统计 `reward_tip.status=SUCCESS`，金额为 `amount` 合计。

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "dataSource": "DB",
    "userCount": 120,
    "novelCount": 30,
    "commentCount": 260,
    "subscriptionCount": 15,
    "subscriptionAmount": 128.00,
    "tipCount": 4,
    "tipAmount": 36.00,
    "pendingCounts": { "novel": 2, "chapter": 5, "comment": 3, "rating": 1 },
    "recentPendingAudits": [
      {
        "id": "1001",
        "type": "NOVEL",
        "title": "示例作品",
        "author": "作者甲",
        "submittedAt": "2026-11-25T09:30:00+08:00"
      }
    ]
  }
}
```

`type` 可取 `NOVEL/CHAPTER/COMMENT/RATING`，最近摘要最多 8 条；章节摘要不查询正文。

## 3. 小说与章节审核

审核请求体统一为：

```json
{ "action": 1, "reason": "可选审核说明" }
```

`action=1` 表示通过，`action=2` 表示驳回；驳回时 `reason` 必填，最长 255 字。小说和章节状态更新与 `audit_log` 插入在同一事务中执行。审核通过只更新 `audit_status`，不自动发布、下架或改变连载状态。

### 3.1 小说审核列表

- 请求：`GET /api/admin/audit/novel`
- 参数：`auditStatus`（0/1/2，可选）、`keyword`（作品名）、`author`（作者昵称或用户名）、`page`、`pageSize`。
- 响应 `data.list[]`：`novelId/authorId/title/authorName/coverUrl/description/status/auditStatus/totalWords/createdAt/updatedAt`。

### 3.2 审核小说

- 请求：`POST /api/admin/audit/novel/{novelId}`
- 请求体：统一审核请求体。
- 成功响应：`data=null`。

### 3.3 章节审核列表

- 请求：`GET /api/admin/audit/chapter`
- 参数：`novelId`（可选字符串 ID）、`auditStatus`（0/1/2，可选）、`keyword`（章节标题）、`page`、`pageSize`。
- 响应 `data.list[]`：`chapterId/novelId/novelTitle/authorName/chapterNo/title/wordCount/isVip/status/auditStatus/createdAt/updatedAt`。
- 列表 SQL 不读取 `chapter.content`；草稿的 `auditStatus` 可为 `null`。

### 3.4 章节审核详情

- 请求：`GET /api/admin/audit/chapter/{chapterId}`
- 请求参数：无。

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "chapter": {
      "chapterId": "2001",
      "novelId": "1001",
      "novelTitle": "示例作品",
      "authorName": "作者甲",
      "chapterNo": 1,
      "title": "第一章",
      "wordCount": 2200,
      "isVip": 0,
      "status": 0,
      "auditStatus": 0,
      "createdAt": "2026-11-25T09:00:00+08:00",
      "updatedAt": "2026-11-25T09:30:00+08:00"
    },
    "content": "章节正文"
  }
}
```

### 3.5 审核章节

- 请求：`POST /api/admin/audit/chapter/{chapterId}`
- 请求体：统一审核请求体。
- 成功响应：`data=null`。

### 3.6 审核记录查询

- 请求：`GET /api/admin/audit/log`
- 参数：`targetType`（NOVEL/CHAPTER/COMMENT/RATING）、`targetId`、`adminId`、`startTime`、`endTime`、`page`、`pageSize`，均可选。
- 响应 `data.list[]`：`auditId/adminId/adminName/targetType/targetId/auditAction/auditReason/createdAt`。
- 审核日志只追加，不提供修改和删除接口。

## 4. 用户管理

### 4.1 用户分页查询

- 请求：`GET /api/admin/user`
- 参数：`keyword`（用户名或昵称）、`role`（READER/AUTHOR/ADMIN）、`status`（ACTIVE/LOCKED/DISABLED）、`page`、`pageSize`。
- 响应 `data.list[]`：`userId/username/nickname/avatarUrl/email/phone/role/status/lastLoginAt/createdAt`。
- 查询字段不包含 `password_hash`。

### 4.2 修改用户状态

- 请求：`PATCH /api/admin/user/{userId}/status`
- 请求体：

```json
{ "status": "LOCKED" }
```

`status` 可取 `ACTIVE/LOCKED/DISABLED`。管理员不能锁定或停用当前登录的本人账号。本接口不修改角色、余额或交易数据。

## 5. 标签管理

标签请求体：

```json
{ "tagName": "科幻", "isActive": 1 }
```

`tagName` 去除首尾空格后必填，最长 32 字；`isActive` 为 0（停用）或 1（启用）。

### 5.1 标签分页查询

- 请求：`GET /api/admin/tag`
- 参数：`tagName`（名称模糊查询）、`isActive`（0/1）、`page`、`pageSize`。
- 响应 `data.list[]`：`tagId/tagName/isActive/novelCount/createdAt`；`novelCount` 来自 `novel_tag` 关联数量。

### 5.2 新增标签

- 请求：`POST /api/admin/tag`
- 请求体：标签请求体。
- 响应 `data`：新增标签对象。

### 5.3 修改标签

- 请求：`PUT /api/admin/tag/{tagId}`
- 请求体：标签请求体。
- 响应 `data`：修改后的标签对象。

### 5.4 删除标签

- 请求：`DELETE /api/admin/tag/{tagId}`
- 请求参数：无。
- 成功响应：`data=null`。
- 已被 `novel_tag` 引用的标签不物理删除，返回 40902，调用方应改为停用。

## 6. 错误说明

| HTTP | 业务码 | 场景 |
|---|---:|---|
| 400 | 40000 | 参数格式错误、驳回原因缺失、时间范围错误 |
| 400 | 40001 | 尝试锁定或停用当前管理员本人 |
| 401 | 40100 | 未登录或登录失效 |
| 403 | 40300 | 非管理员访问后台接口 |
| 404 | 40401 | 作品、章节、用户或标签不存在 |
| 409 | 40901 | 标签名称重复 |
| 409 | 40902 | 标签已被作品引用，不能删除 |
| 500 | 50000 | 数据库或其他服务端异常；不会自动降级为模拟数据 |

错误响应示例：

```json
{ "code": 40902, "message": "标签已被作品引用，请改为停用", "data": null }
```

## 7. 复用的 C 模块接口

D 页面直接跳转并复用以下既有实现，不在 D 中重写业务逻辑：

- 评论列表：`GET /api/admin/comment`
- 评论审核：`POST /api/admin/comment/{commentId}/audit`
- 评论状态：`POST /api/admin/comment/{commentId}/status`
- 评分短评列表：`GET /api/admin/rating/review`
- 评分短评审核：`POST /api/admin/rating/{ratingId}/audit`
- 榜单配置与生成：`/api/admin/ranking` 下既有接口

评论审核继续使用 `sp_audit_comment`，榜单生成继续使用 `sp_generate_ranking`。
