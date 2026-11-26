# C 互动与榜单 · 评论盖楼 / 评分 / 榜单接口说明

- 文档版本：v1.0
- 接口负责人：C（互动与榜单）
- 最近更新：2026-11-24
- 开发状态：已完成
- 关联页面或模块：`frontend/src/views/reader/NovelInteractionView.vue`、`frontend/src/views/reader/RankingBoardView.vue`、`frontend/src/views/admin/CommentManagementView.vue`、`frontend/src/views/admin/RatingAuditView.vue`、`frontend/src/views/admin/RankingConfigView.vue`

## 1. 通用约定

| 项目 | 内容 |
|---|---|
| 基础路径 | `/api` |
| 内容类型 | `application/json; charset=UTF-8`；GET 无请求体 |
| 统一响应 | `ApiResponse<T>`：`code=0` 表示成功，`message` 为提示信息，`data` 为业务数据 |
| 分页结构 | `PageResult<T>`：`list`、`total`、`page`、`pageSize`，`page` 从 1 开始，`pageSize` 上限 100 |
| BIGINT ID | JSON 中统一为字符串，避免精度丢失 |
| 时间格式 | `yyyy-MM-dd'T'HH:mm:ss+08:00` |
| 鉴权方式 | 请求头 `Authorization: Bearer <JWT>`；`sub=userId`，载荷含 `username`、`role` |
| 权限 | 普通用户可调用读者端写接口；`/api/admin/**` 要求 `role=ADMIN` |
| 错误码 | `40000` 参数错误、`40001` 业务规则不满足、`40100` 未登录或凭证失效、`40300` 无权限、`40401` 资源不存在、`40900` 唯一键冲突、`50000` 服务器异常 |

公开接口：`GET /api/interaction/comment`、`GET /api/interaction/rating/summary/{novelId}`、`GET /api/interaction/rating/review`、`GET /api/interaction/ranking/**`。其余 C 接口均需登录；`/api/admin/**` 需管理员。

写操作统一使用 C 模块冻结存储过程，业务异常由 MySQL `SIGNAL SQLSTATE '45000'` 转为 `40001`。

---

## 2. 读者端：评论盖楼

### 2.1 查询评论列表

| 项目 | 内容 |
|---|---|
| 请求地址 | `GET /api/interaction/comment` |
| 鉴权要求 | 公开；登录后返回当前用户的点赞状态 |
| 数据来源 | `v_comment_visible` + `comment` + `comment_like` |
| 幂等要求 | 是（只读） |
| 事务要求 | 无 |

Query 参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `novelId` | String | 是 | 无 | 作品 ID |
| `chapterId` | String | 否 | 无 | 传值时查询本章说；不传查询作品书评（`chapter_id IS NULL`） |
| `page` | Integer | 否 | 1 | 页码，从 1 开始 |
| `pageSize` | Integer | 否 | 10 | 每页条数，1～100 |

`data.list` 元素（一级评论，`replies` 为直接回复）：

| 字段 | 类型 | 说明 |
|---|---|---|
| `commentId` | String | 评论 ID |
| `novelId` / `chapterId` | String | 作品 ID / 章节 ID，书评的 `chapterId` 为 null |
| `parentId` | String | 一级评论为 null |
| `replyToUserId` / `replyToNickname` | String | 回复目标用户，一级评论为 null |
| `content` | String | 评论内容，最长 1000 字 |
| `likeCount` / `replyCount` | Integer | 点赞数 / 直接回复数，由触发器维护 |
| `top` | Boolean | 是否置顶 |
| `liked` | Boolean | 当前登录用户是否已点赞 |
| `createdAt` | String | 发表时间 |
| `user` | Object | `{ userId, username, nickname, avatarUrl }` |
| `replies` | Array | 直接回复列表，元素结构同上，`replies` 为空数组 |

### 2.2 发表评论或回复

| 项目 | 内容 |
|---|---|
| 请求地址 | `POST /api/interaction/comment` |
| 鉴权要求 | 登录用户 |
| 数据来源 | `sp_create_comment` |
| 幂等要求 | 否；重复提交会产生多条评论 |
| 事务要求 | 服务层事务；评论插入与计数维护由过程保证一致 |

请求体：

| 字段 | 类型 | 必填 | 校验与说明 |
|---|---|---|---|
| `novelId` | String | 是 | 作品必须存在 |
| `chapterId` | String | 否 | 本章说必填；章节必须属于该作品 |
| `parentId` | String | 否 | 回复时传一级评论 ID；只允许一级评论和直接回复，禁止三级嵌套 |
| `content` | String | 是 | 去除首尾空白后 1～1000 字 |

`replyToUserId` 由服务端根据父评论作者强制写入，客户端不得传入。

响应 `data`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `commentId` | String | 新评论 ID |
| `auditStatus` | Integer | 固定为 0（待审核） |

错误：`40001` 章节不存在、章节与作品不一致、父评论不存在、回复层级超限、回复与父评论归属不一致。

### 2.3 删除自己的评论

| 项目 | 内容 |
|---|---|
| 请求地址 | `DELETE /api/interaction/comment/{commentId}` |
| 鉴权要求 | 登录用户，仅评论作者 |
| 数据来源 | `sp_set_comment_status`（`status=3` 软删除） |
| 幂等要求 | 重复删除返回成功或业务提示，不物理删除 |
| 事务要求 | 服务层事务 |

错误：`40401` 评论不存在；`40300` 非本人评论。

### 2.4 点赞 / 取消点赞

| 项目 | 内容 |
|---|---|
| 请求地址 | `POST /api/interaction/comment/{commentId}/like`、`DELETE /api/interaction/comment/{commentId}/like` |
| 鉴权要求 | 登录用户 |
| 数据来源 | `comment_like` + 触发器维护 `comment.like_count` |
| 幂等要求 | 是；重复点赞/取消不重复计数 |
| 事务要求 | 服务层事务 |

错误：`40401` 评论不存在或不可见。

---

## 3. 读者端：评分与短评

### 3.1 查询作品评分汇总

| 项目 | 内容 |
|---|---|
| 请求地址 | `GET /api/interaction/rating/summary/{novelId}` |
| 鉴权要求 | 公开；登录后返回 `my*` 字段 |
| 数据来源 | `novel_rating_stat` 为唯一权威统计 |
| 幂等要求 | 是（只读） |
| 事务要求 | 无 |

响应 `data`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `novelId` / `novelTitle` | String | 作品信息 |
| `ratingCount` | Integer | 参与评分人数 |
| `scoreTotal` | Long | 评分总分 |
| `avgScore` | BigDecimal | 平均分，保留两位 |
| `myScore` | Integer | 当前用户评分，未登录或未评分为 null |
| `myReviewContent` | String | 当前用户短评 |
| `myReviewAuditStatus` | Integer | 短评审核状态：0 待审核、1 通过、2 驳回；无短评为 null |

错误：`40401` 作品不存在。

### 3.2 查询公开短评

| 项目 | 内容 |
|---|---|
| 请求地址 | `GET /api/interaction/rating/review` |
| 鉴权要求 | 公开 |
| 数据来源 | `novel_rating` 中短评审核通过且记录正常的评分 |
| 幂等要求 | 是（只读） |
| 事务要求 | 无 |

Query 参数：`novelId`（必填）、`page`、`pageSize`。

`data.list` 元素：`ratingId`、`userId`、`nickname`、`avatarUrl`、`novelId`、`novelTitle`、`score`、`reviewContent`、`status`、`reviewAuditStatus`、`createdAt`。

### 3.3 提交或更新评分

| 项目 | 内容 |
|---|---|
| 请求地址 | `POST /api/interaction/rating` |
| 鉴权要求 | 登录用户 |
| 数据来源 | `novel_rating`；统计由触发器写入 `novel_rating_stat` |
| 幂等要求 | 唯一键 `(user_id, novel_id)`，同一用户重复提交为更新 |
| 事务要求 | 服务层事务 |

请求体：

| 字段 | 类型 | 必填 | 校验与说明 |
|---|---|---|---|
| `novelId` | String | 是 | 作品存在且未下架 |
| `score` | Integer | 是 | 1～5 |
| `reviewContent` | String | 否 | 最长 500 字；修改后短评自动重置为待审核 |

响应与 3.1 相同。错误：`40401` 作品不存在；`40001` 作品已下架。

---

## 4. 读者端：榜单

### 4.1 查询启用榜单

| 项目 | 内容 |
|---|---|
| 请求地址 | `GET /api/interaction/ranking` |
| 鉴权要求 | 公开 |
| 数据来源 | `ranking_config` + `ranking_item` 最新期次 |
| 幂等要求 | 是（只读） |
| 事务要求 | 无 |

`data` 为数组，元素字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `rankingId` | String | 榜单 ID |
| `rankingCode` | String | 榜单编码 |
| `rankingName` | String | 榜单名称 |
| `rankingType` | String | `FAVORITE` / `RATING` / `TICKET` / `SALE` |
| `periodType` | String | `DAILY` / `WEEKLY` / `MONTHLY` / `ALL` |
| `topN` | Integer | 展示上限 |
| `weightConfig` | Object | 权重配置，如 `{ "favorite": 1 }` |
| `active` | Boolean | 是否启用 |
| `latestPeriodNo` | String | 最新期次，无则为 null |
| `latestItemCount` | Long | 最新期次条目数 |
| `updatedAt` | String | 配置更新时间 |

### 4.2 查询榜单某期次详情

| 项目 | 内容 |
|---|---|
| 请求地址 | `GET /api/interaction/ranking/{rankingCode}` |
| 鉴权要求 | 公开；榜单必须启用 |
| 数据来源 | `sp_get_ranking` |
| 幂等要求 | 是（只读） |
| 事务要求 | 无 |

Query 参数：`periodNo`（可选，不传取最新期次）、`limit`（可选，1～1000，默认 100）。

响应 `data`：`rankingCode`、`rankingName`、`rankingType`、`periodType`、`periodNo`、`items`。

`items` 元素：`rankNo`、`novelId`、`novelTitle`、`score`、`metricSnapshot`（键为小写指标 `favorite/rating/ticket/sale`）、`createdAt`。

错误：`40401` 榜单不存在或已停用；`40001` 期次格式错误。

### 4.3 查询榜单期次列表

| 项目 | 内容 |
|---|---|
| 请求地址 | `GET /api/interaction/ranking/{rankingCode}/period` |
| 鉴权要求 | 公开 |
| 数据来源 | `ranking_item` 按期次聚合 |
| 幂等要求 | 是（只读） |
| 事务要求 | 无 |

Query 参数：`limit`（可选，1～50，默认 20）。

`data` 元素：`periodNo`、`itemCount`、`createdAt`，按期次倒序。

---

## 5. 后台端：评论审核

> 以下接口均要求 `role=ADMIN`，URL 使用 `/api/admin` 前缀，业务归属 C 模块。

### 5.1 查询后台评论列表

| 项目 | 内容 |
|---|---|
| 请求地址 | `GET /api/admin/comment` |
| 鉴权要求 | 管理员 |
| 数据来源 | `comment` + `novel` + `app_user` |
| 幂等要求 | 是（只读） |
| 事务要求 | 无 |

Query 参数：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `novelId` | String | 否 | 作品 ID |
| `status` | Integer | 否 | 1 正常、2 隐藏、3 删除 |
| `auditStatus` | Integer | 否 | 0 待审核、1 通过、2 驳回 |
| `keyword` | String | 否 | 评论内容模糊匹配 |
| `page` / `pageSize` | Integer | 否 | 分页，默认 1 / 10 |

`data.list` 元素：`commentId`、`novelId`、`novelTitle`、`chapterId`、`userId`、`nickname`、`parentId`、`content`、`likeCount`、`replyCount`、`status`、`auditStatus`、`createdAt`。

### 5.2 审核评论

| 项目 | 内容 |
|---|---|
| 请求地址 | `POST /api/admin/comment/{commentId}/audit` |
| 鉴权要求 | 管理员 |
| 数据来源 | `sp_audit_comment` + `audit_log` |
| 幂等要求 | 是；重复设置同一状态不重复计数 |
| 事务要求 | 审核状态与 `audit_log` 在同一事务提交 |

请求体：`action`（1 通过、2 驳回）、`reason`（驳回时必填，最长 255 字）。

错误：`40000` 驳回原因为空；`40001` 审核状态非法；`40401` 评论不存在。

### 5.3 变更评论状态

| 项目 | 内容 |
|---|---|
| 请求地址 | `POST /api/admin/comment/{commentId}/status` |
| 鉴权要求 | 管理员 |
| 数据来源 | `sp_set_comment_status` |
| 幂等要求 | 是 |
| 事务要求 | 服务层事务；回复计数由过程同步维护 |

请求体：`status`（1 正常、2 隐藏、3 软删除）。

错误：`40001` 状态非法；`40401` 评论不存在。

---

## 6. 后台端：短评审核

### 6.1 查询短评列表

| 项目 | 内容 |
|---|---|
| 请求地址 | `GET /api/admin/rating/review` |
| 鉴权要求 | 管理员 |
| 数据来源 | `novel_rating` + `novel` + `app_user` |
| 幂等要求 | 是（只读） |
| 事务要求 | 无 |

Query 参数：`reviewAuditStatus`（0/1/2，可选）、`page`、`pageSize`。

`data.list` 元素：`ratingId`、`userId`、`nickname`、`avatarUrl`、`novelId`、`novelTitle`、`score`、`reviewContent`、`status`、`reviewAuditStatus`、`createdAt`。

### 6.2 审核短评

| 项目 | 内容 |
|---|---|
| 请求地址 | `POST /api/admin/rating/{ratingId}/audit` |
| 鉴权要求 | 管理员 |
| 数据来源 | `novel_rating.review_audit_status` + `audit_log` |
| 幂等要求 | 是；只更新短评审核状态，不改评分 |
| 事务要求 | 审核状态与 `audit_log` 在同一事务提交 |

请求体：`action`（1 通过、2 驳回）、`reason`（驳回时必填）。

错误：`40000` 驳回原因为空；`40001` 该评分没有短评；`40401` 评分记录不存在。

---

## 7. 后台端：榜单配置与生成

### 7.1 查询全部榜单配置

| 项目 | 内容 |
|---|---|
| 请求地址 | `GET /api/admin/ranking` |
| 鉴权要求 | 管理员 |
| 数据来源 | `ranking_config` + 最新期次统计 |
| 幂等要求 | 是（只读） |
| 事务要求 | 无 |

响应字段同 4.1，包含停用榜单。

### 7.2 新建榜单配置

| 项目 | 内容 |
|---|---|
| 请求地址 | `POST /api/admin/ranking` |
| 鉴权要求 | 管理员 |
| 数据来源 | `ranking_config` |
| 幂等要求 | `ranking_code` 唯一，重复返回 `40900` |
| 事务要求 | 服务层事务 |

请求体：

| 字段 | 类型 | 必填 | 校验 |
|---|---|---|---|
| `rankingCode` | String | 是 | `^[A-Z0-9_]{2,50}$`，创建后不可修改 |
| `rankingName` | String | 是 | 最长 100 字 |
| `rankingType` | String | 是 | `FAVORITE/RATING/TICKET/SALE` |
| `periodType` | String | 是 | `DAILY/WEEKLY/MONTHLY/ALL` |
| `topN` | Integer | 是 | 1～1000 |
| `weightConfig` | Object | 否 | 键为小写指标，缺省权重 1.0 |
| `isActive` | Boolean | 否 | 默认启用 |

### 7.3 修改榜单配置

| 项目 | 内容 |
|---|---|
| 请求地址 | `PUT /api/admin/ranking/{rankingId}` |
| 鉴权要求 | 管理员 |
| 数据来源 | `ranking_config` |
| 幂等要求 | 是 |
| 事务要求 | 服务层事务 |

请求体：`rankingName`、`topN`、`weightConfig`、`isActive`。编码、指标类型与周期创建后不可修改。

### 7.4 生成榜单期次

| 项目 | 内容 |
|---|---|
| 请求地址 | `POST /api/admin/ranking/{rankingId}/generate` |
| 鉴权要求 | 管理员 |
| 数据来源 | `sp_generate_ranking` |
| 幂等要求 | 当前期次与 `ALL` 可重复生成；历史期次默认拒绝覆盖 |
| 事务要求 | 服务层事务；同一期次 DELETE + INSERT 在同一事务 |

请求体：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `periodNo` | String | 是 | 日榜 `yyyymmdd`、周榜 `yyyyWww`、月榜 `yyyymm`、总榜 `ALL` |
| `allowHistoryOverwrite` | Boolean | 否 | 默认 false；仅历史修复时传 true |

响应 `data`：`rankingId`、`periodNo`、`itemCount`。

错误：`40001` 榜单已停用、期次格式错误、未来期次、历史期次禁止覆盖；`40401` 榜单不存在。

---

## 8. 典型错误响应

```json
{
  "code": 40001,
  "message": "仅允许一级评论和直接回复，不允许继续嵌套",
  "data": null
}
```

```json
{
  "code": 40100,
  "message": "请先登录",
  "data": null
}
```
