# B 作品与内容模块 · 接口说明 v1.0

- 版本：v1.0 / 2026-11-23
- 负责人：B（作品与内容模块）
- 依据：`docs/achieve/B_作品与内容_字段清单_v0.2_20261120.xlsx`、`sql/ddl/B_作品与内容_建表_v1.0_20261123.sql`
- 服务前缀：`/api/v1`
- 数据格式：`application/json; charset=utf-8`

## 1. 通用约定

### 1.1 响应结构

```json
{ "code": 0, "message": "ok", "data": {} }
```

| 字段 | 说明 |
|---|---|
| code | 0 成功；非 0 失败（见 1.3） |
| message | 提示信息 |
| data | 业务数据，失败时为 null |

分页数据统一为：

```json
{ "page": 1, "page_size": 20, "total": 135, "list": [] }
```

### 1.2 认证与权限

- 除「书城 / 搜索 / 详情 / 目录 / 阅读」5 个公开接口外，其余均需请求头 `Authorization: Bearer <JWT>`。
- 作者接口要求令牌中 `role` 为 `AUTHOR` 或 `ADMIN`，否则返回 `40301`。
- **作者身份一律取自令牌（`user_id`），请求体中传入的 `author_id` 一律忽略**，防止越权代发。
- 关联他人作品时校验 `novel.author_id` 与令牌用户一致，否则 `40301`。

### 1.3 错误码

| code | HTTP | 含义 |
|---|---|---|
| 0 | 200 | 成功 |
| 40001 | 400 | 参数校验失败 |
| 40101 | 401 | 未登录或令牌失效 |
| 40301 | 403 | 无权限（非作者 / 非本人作品） |
| 40401 | 404 | 资源不存在 |
| 40901 | 409 | 唯一性冲突（存储过程之外的 MySQL 1062，如并发写入标签关联） |
| 40902 | 409 | 业务规则冲突（存储过程 `SIGNAL 45000`：重复章节序号、重复标签、状态不允许等） |
| 50000 | 500 | 服务器内部错误（不向前端暴露 SQL 细节） |

### 1.4 枚举与展示规则

| 字段 | 取值 |
|---|---|
| `novel.status` | 0 连载中 / 1 已完结 / 2 已下架 |
| `novel.audit_status` | 0 待审核 / 1 通过 / 2 驳回（**只读**，由审核流程变更） |
| `chapter.status` | 0 草稿或未发布 / 1 已发布 / 2 已下架 |
| `chapter.audit_status` | NULL 未提交 / 0 待审核 / 1 通过 / 2 驳回（**只读**） |

- 小说可展示条件：`audit_status = 1 AND status IN (0,1)`。
- 章节可展示条件：`audit_status = 1 AND status = 1 AND publish_time IS NOT NULL`，且所属小说可展示。
- 列表接口**一律不返回 `chapter.content`**；正文仅在阅读接口按主键单行读取。
- `vip_price` 单位：元，`DECIMAL(10,2)`；非 VIP 恒为 `0.00`。
- 评分（`avg_score` / `rating_count`）由 C 模块 `novel_rating_stat` 提供，B 不返回、不存储。

---

## 2. 公开接口

### 2.1 书城列表 `GET /novels`

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| page | int | 否 | 默认 1 |
| page_size | int | 否 | 默认 20，最大 50 |
| status | int | 否 | 0 连载中 / 1 已完结；默认不限（不含已下架） |
| tag_id | bigint | 否 | 按标签过滤 |
| sort | string | 否 | `update`（默认，最近更新）/ `words`（总字数）/ `new`（最新创建） |

```json
{ "code": 0, "message": "ok", "data": { "page": 1, "page_size": 20, "total": 2, "list": [
  { "novel_id": 1001, "title": "斗破苍穹", "cover_url": "https://cdn/…jpg",
    "description": "…", "status": 0, "total_words": 1280000,
    "latest_chapter_id": 88001, "author_id": 10001, "author_name": "天蚕土豆",
    "tag_names": ["玄幻","热血"], "updated_at": "2026-11-23 10:00:00" }
] } }
```

### 2.2 搜索 `GET /novels/search`

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| keyword | string | 否 | 书名前缀或作者昵称匹配，长度 ≤ 50 |
| tag_id | bigint | 否 | 标签过滤 |
| page / page_size | int | 否 | 同上 |

说明：关键词走 `idx_title` 前缀匹配；`keyword` 与 `tag_id` 至少传一个，否则 `40001`。

### 2.3 作品详情 `GET /novels/{novelId}`

返回字段同 2.1 列表项，外加：

| 字段 | 说明 |
|---|---|
| description | 完整简介 |
| tags | `[{ "tag_id":1, "tag_name":"玄幻", "sort_order":0 }]` |
| chapter_count | 已发布章节数 |
| last_chapter_title | 最新章节标题，无则 null |

不存在或不可展示 → `40401`。

### 2.4 目录 `GET /novels/{novelId}/chapters`

返回已发布且审核通过的章节，按 `chapter_no` 升序。

```json
{ "code": 0, "message": "ok", "data": { "novel_id": 1001, "list": [
  { "chapter_id": 88001, "chapter_no": 1, "title": "第一章 陨落的天才",
    "is_vip": 0, "vip_price": 0.00, "word_count": 3120,
    "publish_time": "2026-11-20 12:00:00" }
] } }
```

### 2.5 章节阅读 `GET /chapters/{chapterId}`

返回：`chapter_id`、`novel_id`、`chapter_no`、`title`、`content`、`is_vip`、`vip_price`、`word_count`、`publish_time`、`prev_chapter_id`、`next_chapter_id`。

- 未发布 / 已下架 / 审核未通过 → `40401`。
- VIP 章节是否放行由 A 的订阅校验决定：未订阅时仍返回正文之外的元数据，`content` 置 null 并返回 `40301` 由前端引导订阅（接口契约见 A 模块文档）。

---

## 3. 作者接口

### 3.1 创建作品 `POST /novels`

对应存储过程：`sp_create_novel`。

| Body | 类型 | 必填 | 说明 |
|---|---|---|---|
| title | string | 是 | 书名，≤ 120，去空格后非空 |
| cover_url | string | 否 | ≤ 255 |
| description | string | 否 | ≤ 1000 |
| tag_ids | bigint[] | 否 | 标签 ID 数组，须均为启用状态 |

成功返回 `{ "novel_id": 1001 }`。

| 错误 | 场景 |
|---|---|
| 40001 | 书名为空 / 标签数量超限 |
| 40301 | 当前账号非 AUTHOR（含被锁定/禁用） |
| 40902 | 标签不存在或已停用（过程抛出 45000，整体回滚） |

### 3.2 创作台作品列表 `GET /author/novels`

| 参数 | 说明 |
|---|---|
| page / page_size | 分页 |
| status | 可选，按连载状态过滤 |

返回作者全部作品（**含待审核、已下架**），字段同 2.1，另加 `audit_status`、`chapter_count`。按 `updated_at` 倒序（走 `idx_author_update`）。

### 3.3 新建章节草稿 `POST /novels/{novelId}/chapters`

对应存储过程：`sp_save_chapter_draft`（`p_chapter_id = NULL`）。

| Body | 类型 | 必填 | 说明 |
|---|---|---|---|
| title | string | 是 | ≤ 120 |
| content | string | 是 | 正文；字数由服务端按字符数计算 |
| is_vip | int | 是 | 0 / 1 |
| vip_price | decimal | 是 | 非 VIP 必须 0.00；VIP 必须 > 0.00 |

- `chapter_no` 由服务端在同书内自动分配（`MAX(chapter_no)+1`），并发由行锁保护。
- 新建后 `status = 0`、`audit_status = NULL`（草稿未提交）。

成功返回 `{ "chapter_id": 88002, "chapter_no": 21, "word_count": 3120 }`。

### 3.4 修改章节 `PUT /chapters/{chapterId}`

参数同 3.3。对应 `sp_save_chapter_draft`（传入 `p_chapter_id`）。

规则：

- 已发布章节改稿 → 自动下线（`status 1→0`）、回减 `total_words`、`audit_status` 重新置 0，需重新审核后才能再发布。
- 已驳回章节改稿 → 重新置 0。
- 已下架章节 → `40902`「已下架章节不可编辑」。

### 3.5 发布章节 `POST /chapters/{chapterId}/publish`

对应存储过程：`sp_publish_chapter`。

- 必须 `audit_status = 1`（审核通过），否则 `40902`「章节未通过审核，不能发布」。
- 已发布 → `40902`「章节已发布，请勿重复发布」。
- 成功后 `status = 1`、写 `publish_time`、累加 `novel.total_words`、刷新 `latest_chapter_id`。

### 3.6 下架章节 `POST /chapters/{chapterId}/offline`

对应存储过程：`sp_offline_chapter`。

- 不做物理删除，仅 `status = 2`。
- 若原为已发布，回减 `total_words` 并重算 `latest_chapter_id`。
- 已下架 → `40902`「章节已处于下架状态」。

---

## 4. 接口与存储过程映射

| 接口 | 存储过程 |
|---|---|
| POST /novels | sp_create_novel |
| POST /novels/{id}/chapters、PUT /chapters/{id} | sp_save_chapter_draft |
| POST /chapters/{id}/publish | sp_publish_chapter |
| POST /chapters/{id}/offline | sp_offline_chapter |

## 5. 与其它模块的边界

| 事项 | 归属 |
|---|---|
| 订阅扣费、余额校验、VIP 章节放行 | A（读 `chapter.vip_price`） |
| 评论、评分、榜单 | C（读 `novel`，写 `novel_rating_stat`） |
| 审核操作（`audit_status` 变更、`audit_log`） | D / 审核模块；B 只读该字段 |
| 书架、阅读进度 | A（`user_bookshelf`） |

## 6. 变更记录

| 版本 | 日期 | 说明 |
|---|---|---|
| v1.0 | 2026-11-23 | 对齐字段清单 v0.2：新增审核与发布分离、下架替代删除、移除评分冗余字段 |
