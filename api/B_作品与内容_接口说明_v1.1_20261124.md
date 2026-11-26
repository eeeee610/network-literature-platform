# B 作品与内容 · 接口说明 v1.1

- 文档版本：v1.1
- 接口负责人：B（作品与内容模块）
- 最近更新：2026-11-24
- 开发状态：开发中（接口契约已确定，待联调）
- 关联页面或模块：书城首页、搜索页、作品详情页、章节目录页、阅读页、作者创作台
- 依据文件：`sql/ddl/B_作品与内容_建表_v1.0_20261123.sql`、`sql/objects/B_作品与内容_存储过程_v1.0_20261123.sql`、`sql/ddl/A_用户交易_建表_v1.1_20261123.sql`、`docs/D_技术开发规范_v1.0_20261123.md`
- 服务前缀：`/api/novel`
- 数据格式：`application/json; charset=utf-8`

## 1. 通用约定

### 1.1 鉴权与作者身份

- 公开接口无需令牌：`GET /api/novel/list`、`/search`、`/{novelId}`、`/{novelId}/chapters`、`/chapter/{chapterId}`、`/tag/list`。
- 作者接口必须携带请求头 `Authorization: Bearer <JWT>`；未携带、格式错误或令牌过期统一返回 HTTP 401、业务码 `40100`。
- 作者接口要求令牌中 `role` 为 `AUTHOR` 或 `ADMIN`，否则返回 HTTP 403、业务码 `40300`。
- **作者身份只从令牌中解析，请求体与 Query 中不接收 `authorId`；即使传入也一律忽略**，防止越权代发。
- 涉及既有作品、章节的写操作，必须校验 `novel.author_id` 等于令牌用户（`ADMIN` 除外），不一致返回业务码 `40902`（HTTP 403）。
- 存储过程 `sp_create_novel` 内部还会再次校验 `app_user.role='AUTHOR'` 且 `status='ACTIVE'`，校验失败返回业务码 `40300`。

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
| `page` | Integer | 1 | 从 1 开始；小于 1 按 1 处理或返回 `40000` |
| `pageSize` | Integer | 10 | 取值 1～100，超过 100 返回 `40000` |

非分页接口（标签列表、作品详情、章节阅读）`data` 直接返回对象或数组，不套分页结构。

### 1.4 时间与 ID

- 所有数据库 `BIGINT` ID 在 JSON 中一律用字符串返回，如 `"1001"`，避免 JavaScript 精度丢失。
- 日期时间一律 ISO 8601 带时区，后端时区 `Asia/Shanghai`，如 `2026-11-24T20:15:30+08:00`。
- 金额 `vipPrice` 以数值返回，单位元，保留两位小数，如 `12.00`。

### 1.5 枚举含义（以 DDL 为准）

| 字段 | 取值与含义 |
|---|---|
| `novel.status` | `0` 连载中；`1` 已完结；`2` 已下架（不物理删除） |
| `novel.auditStatus` | `0` 待审核；`1` 审核通过；`2` 审核驳回；书名或简介修改后由后端重新置 `0` |
| `chapter.status` | `0` 草稿/未发布；`1` 已发布；`2` 已下架（不物理删除） |
| `chapter.auditStatus` | `null` 未提交（草稿）；`0` 待审核；`1` 审核通过；`2` 审核驳回；正文修改后重新置 `0` |
| `chapter.isVip` | `0` 免费；`1` VIP |
| `tag.isActive` | `1` 启用；`0` 停用，仅启用中的标签可被选择 |

`chapter.vipPrice` 约束：`isVip=0` 时必须为 `0.00`；`isVip=1` 时必须大于 `0.00`。

### 1.6 可见性规则

| 对象 | 公开可见条件 |
|---|---|
| 作品 | `status IN (0,1)` 且 `audit_status = 1` |
| 章节目录 | 所属作品公开可见，且章节 `status = 1`、`audit_status = 1`、`publish_time IS NOT NULL` |
| 章节正文 | 与目录一致；正文仅在阅读接口按主键单行读取 |

- 列表、目录、详情接口**一律不返回 `chapter.content`**，避免大字段拖慢查询。
- 新建作品默认 `audit_status = 0`（待审核），未通过审核前不出现在书城与搜索中。
- 评分（`avgScore`/`ratingCount`）由 C 模块 `novel_rating_stat` 维护，本模块不返回、不存储。

### 1.7 排序约定

| `sort` 取值 | 排序规则 |
|---|---|
| `update`（默认） | `novel.updated_at DESC` |
| `words` | `novel.total_words DESC`，相同则 `updated_at DESC` |
| `new` | `novel.created_at DESC` |

## 2. 接口清单

| 序号 | 方法 | 路径 | 鉴权 | 说明 | 数据来源 |
|---:|---|---|---|---|---|
| 1 | GET | `/api/novel/list` | 公开 | 书城列表 | 真实数据库 |
| 2 | GET | `/api/novel/search` | 公开 | 关键词搜索 | 真实数据库 |
| 3 | GET | `/api/novel/{novelId}` | 公开 | 作品详情 | 真实数据库 |
| 4 | GET | `/api/novel/{novelId}/chapters` | 公开 | 公开章节目录 | 真实数据库 |
| 5 | GET | `/api/novel/chapter/{chapterId}` | 公开 | 章节正文阅读 | 真实数据库 |
| 6 | GET | `/api/novel/tag/list` | 公开 | 启用标签列表 | 真实数据库 |
| 7 | POST | `/api/novel` | 作者/管理员 | 创建作品 | `sp_create_novel` |
| 8 | PUT | `/api/novel/{novelId}` | 作者/管理员 | 编辑作品并重新待审 | 真实数据库（后端事务） |
| 9 | GET | `/api/novel/author/list` | 作者/管理员 | 我的作品列表 | 真实数据库 |
| 10 | GET | `/api/novel/author/{novelId}/chapters` | 作者/管理员 | 作者视角章节列表 | 真实数据库 |
| 11 | POST | `/api/novel/chapter` | 作者/管理员 | 新建章节草稿 | `sp_save_chapter_draft` |
| 12 | PUT | `/api/novel/chapter/{chapterId}` | 作者/管理员 | 修改章节 | `sp_save_chapter_draft` |
| 13 | POST | `/api/novel/chapter/{chapterId}/publish` | 作者/管理员 | 发布章节 | `sp_publish_chapter` |
| 14 | POST | `/api/novel/chapter/{chapterId}/offline` | 作者/管理员 | 下架章节 | `sp_offline_chapter` |
| 15 | GET | `/api/novel/author/chapter/{chapterId}` | 作者/管理员 | 作者章节详情（含正文，供改稿回显） | 真实数据库 |

## 3. 错误码

### 3.1 公共错误码

| HTTP 状态 | 业务码 | 触发条件 | message 示例 | 调用方处理 |
|---|---:|---|---|---|
| 400 | 40000 | 缺少必填参数、长度/取值范围/格式校验失败 | `pageSize: 不能大于 100` | 修正输入后重试 |
| 401 | 40100 | 未携带令牌、令牌格式错误或已失效 | `未登录或登录已失效` | 清理本地令牌并跳转登录 |
| 403 | 40300 | 已登录但非 `AUTHOR`/`ADMIN`，或账号被锁定/禁用 | `无权执行该操作` | 停止重试，提示权限不足 |
| 404 | 40400 | 请求路径不存在 | `接口不存在` | 检查请求地址 |
| 500 | 50000 | 未处理服务器异常 | `服务器内部错误` | 记录问题并联系后端，不展示堆栈 |

### 3.2 B 模块业务码

| HTTP 状态 | 业务码 | 触发条件 | message 示例 | 调用方处理 |
|---|---:|---|---|---|
| 403 | 40902 | 已登录但不是该作品作者（且非管理员） | `无权操作该作品` | 停止重试；只允许操作本人作品 |
| 404 | 40901 | `novelId` 对应作品不存在 | `作品不存在` | 刷新列表或返回书城 |
| 404 | 40903 | `chapterId` 对应章节不存在 | `章节不存在` | 刷新目录 |
| 409 | 40904 | 当前状态不允许该操作（未过审却发布、重复发布、重复下架、编辑已下架章节） | `章节未通过审核，不能发布` | 刷新状态后再决定操作 |
| 400 | 40905 | 提交的标签不存在或已停用 | `标签不存在或已停用` | 重新拉取标签列表后重选 |
| 409 | 40906 | 并发新建章节导致章节序号唯一键冲突 | `章节序号冲突，请重试` | 可重试一次；仍失败则联系后端 |
| 409 | 40900 | 存储过程抛出的其他业务校验失败 | `VIP 价格与 VIP 标记不一致` | 按 message 修正后重试 |

说明：业务码 `409xx` 是 B 模块的编号段，**HTTP 状态码按语义单独设置**——资源不存在用 404，权限不足用 403，状态冲突用 409，输入类语义错误用 400。因此 `40902`、`40905` 的 HTTP 状态不是 409，调用方应以 HTTP 状态 + `code` 共同判断。

## 4. 接口详情

### 4.1 书城列表

#### 基本信息

| 项目 | 内容 |
|---|---|
| 接口名称 | 书城作品列表 |
| 请求地址 | `/api/novel/list` |
| 请求方式 | GET |
| 接口说明 | 书城首页与分类页展示公开作品，支持排序与标签筛选 |
| 鉴权要求 | 公开 |
| 内容类型 | 无请求体 |
| 数据来源 | 真实数据库（`novel`、`novel_tag`、`tag`、`app_user`） |
| 幂等要求 | 幂等，可重复调用 |
| 事务要求 | 只读查询，无事务 |

#### 请求参数（Query）

| 参数 | 类型 | 必填 | 默认值 | 示例 | 校验与说明 |
|---|---|---|---|---|---|
| `page` | Integer | 否 | 1 | `1` | 从 1 开始 |
| `pageSize` | Integer | 否 | 10 | `10` | 1～100 |
| `sort` | String | 否 | `update` | `update` | 枚举 `update`/`words`/`new`，其他值返回 `40000` |
| `status` | Integer | 否 | 无 | `0` | 仅允许 `0` 连载中、`1` 已完结；不传表示不限；`2` 已下架不对外 |
| `tagId` | String | 否 | 无 | `"3"` | 标签 ID 字符串，按标签过滤 |

固定过滤条件：`novel.audit_status = 1 AND novel.status IN (0,1)`。

请求示例：

```http
GET /api/novel/list?page=1&pageSize=10&sort=update&status=0&tagId=3 HTTP/1.1
```

#### 成功响应

| 字段 | 类型 | 必返 | 说明 |
|---|---|---|---|
| `data.list[].novelId` | String | 是 | 作品 ID |
| `data.list[].title` | String | 是 | 书名 |
| `data.list[].coverUrl` | String | 否 | 封面地址，无封面为 `null` |
| `data.list[].description` | String | 否 | 作品简介，可为 `null` |
| `data.list[].status` | Integer | 是 | 0 连载中 / 1 已完结 |
| `data.list[].totalWords` | Integer | 是 | 已发布章节总字数 |
| `data.list[].latestChapterId` | String | 否 | 最新已发布章节 ID，无则 `null` |
| `data.list[].latestChapterTitle` | String | 否 | 最新已发布章节标题，无则 `null` |
| `data.list[].authorId` | String | 是 | 作者用户 ID |
| `data.list[].authorName` | String | 是 | 作者昵称（`app_user.nickname`） |
| `data.list[].tags` | Array | 是 | 标签数组，元素为 `{ tagId, tagName }`，无标签返回 `[]` |
| `data.list[].updatedAt` | String | 是 | 更新时间，ISO 8601 带时区 |
| `data.total` | Integer | 是 | 满足条件的总记录数 |
| `data.page` | Integer | 是 | 当前页码 |
| `data.pageSize` | Integer | 是 | 每页条数 |

```http
HTTP/1.1 200 OK
Content-Type: application/json
```

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "novelId": "1001",
        "title": "星海归途",
        "coverUrl": "https://cdn.example.com/cover/1001.jpg",
        "description": "少年在星际废墟中捡到一枚旧式引擎……",
        "status": 0,
        "totalWords": 128000,
        "latestChapterId": "88021",
        "latestChapterTitle": "第二十一章 折跃点",
        "authorId": "10001",
        "authorName": "夜航",
        "tags": [
          { "tagId": "3", "tagName": "科幻" },
          { "tagId": "7", "tagName": "热血" }
        ],
        "updatedAt": "2026-11-24T20:15:30+08:00"
      }
    ],
    "total": 1,
    "page": 1,
    "pageSize": 10
  }
}
```

#### 错误场景

| HTTP 状态 | 业务码 | 场景 |
|---|---:|---|
| 400 | 40000 | `sort` 非枚举值、`status` 非 0/1、`pageSize` 超出 1～100、`tagId` 非数字 |
| 500 | 50000 | 数据库查询异常 |

### 4.2 搜索作品

#### 基本信息

| 项目 | 内容 |
|---|---|
| 接口名称 | 作品搜索 |
| 请求地址 | `/api/novel/search` |
| 请求方式 | GET |
| 接口说明 | 按书名关键词搜索公开作品，可叠加标签筛选 |
| 鉴权要求 | 公开 |
| 内容类型 | 无请求体 |
| 数据来源 | 真实数据库（`novel`、`novel_tag`、`tag`、`app_user`） |
| 幂等要求 | 幂等，可重复调用 |
| 事务要求 | 只读查询，无事务 |

#### 请求参数（Query）

| 参数 | 类型 | 必填 | 默认值 | 示例 | 校验与说明 |
|---|---|---|---|---|---|
| `keyword` | String | 是 | 无 | `星海` | 去首尾空格后长度 1～50；为空返回 `40000` |
| `tagId` | String | 否 | 无 | `"3"` | 标签 ID，与关键词为“且”关系 |
| `page` | Integer | 否 | 1 | `1` | 从 1 开始 |
| `pageSize` | Integer | 否 | 10 | `10` | 1～100 |

匹配规则：对 `novel.title` 做前缀匹配（`title LIKE CONCAT(keyword,'%')`，命中 `idx_title`）；不检索正文与简介，也没有全文索引。固定过滤 `audit_status = 1 AND status IN (0,1)`。

请求示例：

```http
GET /api/novel/search?keyword=星海&tagId=3&page=1&pageSize=10 HTTP/1.1
```

#### 成功响应

响应字段与书城列表完全一致（`data.list[]` 字段见 4.1），仅筛选条件不同。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "novelId": "1001",
        "title": "星海归途",
        "coverUrl": "https://cdn.example.com/cover/1001.jpg",
        "description": "少年在星际废墟中捡到一枚旧式引擎……",
        "status": 0,
        "totalWords": 128000,
        "latestChapterId": "88021",
        "latestChapterTitle": "第二十一章 折跃点",
        "authorId": "10001",
        "authorName": "夜航",
        "tags": [{ "tagId": "3", "tagName": "科幻" }],
        "updatedAt": "2026-11-24T20:15:30+08:00"
      }
    ],
    "total": 1,
    "page": 1,
    "pageSize": 10
  }
}
```

#### 错误场景

| HTTP 状态 | 业务码 | 场景 |
|---|---:|---|
| 400 | 40000 | `keyword` 缺失、为空或超过 50 字符；`pageSize` 越界 |
| 500 | 50000 | 数据库查询异常 |

### 4.3 作品详情

#### 基本信息

| 项目 | 内容 |
|---|---|
| 接口名称 | 作品详情 |
| 请求地址 | `/api/novel/{novelId}` |
| 请求方式 | GET |
| 接口说明 | 作品详情页展示简介、标签、已发布章节数与最新章节 |
| 鉴权要求 | 公开 |
| 内容类型 | 无请求体 |
| 数据来源 | 真实数据库（`novel`、`novel_tag`、`tag`、`chapter`、`app_user`） |
| 幂等要求 | 幂等，可重复调用 |
| 事务要求 | 只读查询，无事务 |

#### 请求参数

路径参数：

| 参数 | 类型 | 必填 | 示例 | 说明 |
|---|---|---|---|---|
| `novelId` | String | 是 | `"1001"` | 作品 ID，BIGINT 以字符串传递 |

Query 参数：无。

请求示例：

```http
GET /api/novel/1001 HTTP/1.1
```

#### 成功响应

| 字段 | 类型 | 必返 | 说明 |
|---|---|---|---|
| `data.novelId` | String | 是 | 作品 ID |
| `data.title` | String | 是 | 书名 |
| `data.coverUrl` | String | 否 | 封面地址，可为 `null` |
| `data.description` | String | 否 | 完整简介，可为 `null` |
| `data.status` | Integer | 是 | 0 连载中 / 1 已完结 |
| `data.totalWords` | Integer | 是 | 已发布章节总字数 |
| `data.chapterCount` | Integer | 是 | 已发布且审核通过的章节数 |
| `data.latestChapter` | Object | 否 | 最新章节 `{ chapterId, chapterNo, title, publishTime }`，无已发布章节时为 `null` |
| `data.authorId` | String | 是 | 作者用户 ID |
| `data.authorName` | String | 是 | 作者昵称 |
| `data.authorAvatarUrl` | String | 否 | 作者头像，可为 `null` |
| `data.tags` | Array | 是 | `[{ tagId, tagName, sortOrder }]`，按 `sortOrder` 升序，无标签为 `[]` |
| `data.createdAt` | String | 是 | 创建时间 |
| `data.updatedAt` | String | 是 | 更新时间 |

作品不存在或不满足公开可见条件（`audit_status<>1`、`status=2`）时返回 `40901`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "novelId": "1001",
    "title": "星海归途",
    "coverUrl": "https://cdn.example.com/cover/1001.jpg",
    "description": "少年在星际废墟中捡到一枚旧式引擎，从此踏上归途。",
    "status": 0,
    "totalWords": 128000,
    "chapterCount": 21,
    "latestChapter": {
      "chapterId": "88021",
      "chapterNo": 21,
      "title": "第二十一章 折跃点",
      "publishTime": "2026-11-24T20:15:30+08:00"
    },
    "authorId": "10001",
    "authorName": "夜航",
    "authorAvatarUrl": "https://cdn.example.com/avatar/10001.png",
    "tags": [
      { "tagId": "3", "tagName": "科幻", "sortOrder": 0 },
      { "tagId": "7", "tagName": "热血", "sortOrder": 1 }
    ],
    "createdAt": "2026-10-01T09:00:00+08:00",
    "updatedAt": "2026-11-24T20:15:30+08:00"
  }
}
```

#### 错误场景

| HTTP 状态 | 业务码 | 场景 |
|---|---:|---|
| 400 | 40000 | `novelId` 不是合法数字 |
| 404 | 40901 | 作品不存在、已下架或未通过审核 |

### 4.4 公开章节目录

#### 基本信息

| 项目 | 内容 |
|---|---|
| 接口名称 | 公开章节目录 |
| 请求地址 | `/api/novel/{novelId}/chapters` |
| 请求方式 | GET |
| 接口说明 | 作品详情页与阅读页展示已发布章节，按章节序号升序 |
| 鉴权要求 | 公开 |
| 内容类型 | 无请求体 |
| 数据来源 | 真实数据库（`chapter`、`novel`） |
| 幂等要求 | 幂等，可重复调用 |
| 事务要求 | 只读查询，无事务 |

#### 请求参数

路径参数：

| 参数 | 类型 | 必填 | 示例 | 说明 |
|---|---|---|---|---|
| `novelId` | String | 是 | `"1001"` | 作品 ID |

Query 参数：无。目录不分页，一次性返回该书全部已发布章节。

筛选条件：所属作品公开可见，且章节 `status=1 AND audit_status=1 AND publish_time IS NOT NULL`；按 `chapter_no ASC` 排序；**不返回 `content`**。

请求示例：

```http
GET /api/novel/1001/chapters HTTP/1.1
```

#### 成功响应

| 字段 | 类型 | 必返 | 说明 |
|---|---|---|---|
| `data.novelId` | String | 是 | 作品 ID |
| `data.total` | Integer | 是 | 已发布章节数 |
| `data.list[].chapterId` | String | 是 | 章节 ID |
| `data.list[].chapterNo` | Integer | 是 | 章节序号，同书内从 1 递增 |
| `data.list[].title` | String | 是 | 章节标题 |
| `data.list[].isVip` | Integer | 是 | 0 免费 / 1 VIP |
| `data.list[].vipPrice` | Number | 是 | VIP 价格（元），免费章节为 `0.00` |
| `data.list[].wordCount` | Integer | 是 | 本章字数 |
| `data.list[].publishTime` | String | 是 | 发布时间，ISO 8601 带时区 |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "novelId": "1001",
    "total": 2,
    "list": [
      {
        "chapterId": "88001",
        "chapterNo": 1,
        "title": "第一章 废墟信号",
        "isVip": 0,
        "vipPrice": 0.00,
        "wordCount": 3120,
        "publishTime": "2026-10-02T12:00:00+08:00"
      },
      {
        "chapterId": "88021",
        "chapterNo": 21,
        "title": "第二十一章 折跃点",
        "isVip": 1,
        "vipPrice": 12.00,
        "wordCount": 4210,
        "publishTime": "2026-11-24T20:15:30+08:00"
      }
    ]
  }
}
```

#### 错误场景

| HTTP 状态 | 业务码 | 场景 |
|---|---:|---|
| 400 | 40000 | `novelId` 不是合法数字 |
| 404 | 40901 | 作品不存在或不满足公开可见条件 |

### 4.5 章节正文阅读

#### 基本信息

| 项目 | 内容 |
|---|---|
| 接口名称 | 章节正文阅读 |
| 请求地址 | `/api/novel/chapter/{chapterId}` |
| 请求方式 | GET |
| 接口说明 | 阅读页按章节 ID 读取正文，并返回上一章/下一章定位信息 |
| 鉴权要求 | 公开 |
| 内容类型 | 无请求体 |
| 数据来源 | 真实数据库（`chapter`、`novel`） |
| 幂等要求 | 幂等，可重复调用 |
| 事务要求 | 只读查询，无事务 |

#### 请求参数

路径参数：

| 参数 | 类型 | 必填 | 示例 | 说明 |
|---|---|---|---|---|
| `chapterId` | String | 是 | `"88021"` | 章节 ID |

Query 参数：无。

规则：

- 仅返回 `status=1 AND audit_status=1 AND publish_time IS NOT NULL` 且所属作品公开可见的章节。
- `prevChapterId`/`nextChapterId` 在**同书已发布章节**中按 `chapter_no` 相邻定位，跳过草稿、待审与已下架章节；无则返回 `null`。
- VIP 章节是否放行由 A 模块的订阅校验负责（订阅、余额、扣费均属 A），本接口不做付费拦截，也不返回订阅状态。

请求示例：

```http
GET /api/novel/chapter/88021 HTTP/1.1
```

#### 成功响应

| 字段 | 类型 | 必返 | 说明 |
|---|---|---|---|
| `data.chapterId` | String | 是 | 章节 ID |
| `data.novelId` | String | 是 | 所属作品 ID |
| `data.novelTitle` | String | 是 | 作品书名 |
| `data.chapterNo` | Integer | 是 | 章节序号 |
| `data.title` | String | 是 | 章节标题 |
| `data.content` | String | 是 | 章节正文 |
| `data.isVip` | Integer | 是 | 0 免费 / 1 VIP |
| `data.vipPrice` | Number | 是 | VIP 价格（元） |
| `data.wordCount` | Integer | 是 | 本章字数 |
| `data.publishTime` | String | 是 | 发布时间 |
| `data.prevChapterId` | String | 否 | 上一已发布章节 ID，无则 `null` |
| `data.prevChapterTitle` | String | 否 | 上一章标题，无则 `null` |
| `data.nextChapterId` | String | 否 | 下一已发布章节 ID，无则 `null` |
| `data.nextChapterTitle` | String | 否 | 下一章标题，无则 `null` |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "chapterId": "88021",
    "novelId": "1001",
    "novelTitle": "星海归途",
    "chapterNo": 21,
    "title": "第二十一章 折跃点",
    "content": "引擎的低鸣在舱壁间回荡，他把坐标推入主控台……",
    "isVip": 1,
    "vipPrice": 12.00,
    "wordCount": 4210,
    "publishTime": "2026-11-24T20:15:30+08:00",
    "prevChapterId": "88020",
    "prevChapterTitle": "第二十章 旧航线",
    "nextChapterId": null,
    "nextChapterTitle": null
  }
}
```

#### 错误场景

| HTTP 状态 | 业务码 | 场景 |
|---|---:|---|
| 400 | 40000 | `chapterId` 不是合法数字 |
| 404 | 40903 | 章节不存在、未发布、未过审、已下架，或所属作品不可见 |

### 4.6 标签列表

#### 基本信息

| 项目 | 内容 |
|---|---|
| 接口名称 | 启用标签列表 |
| 请求地址 | `/api/novel/tag/list` |
| 请求方式 | GET |
| 接口说明 | 书城筛选与作者发布页选择标签，仅返回启用中的标签 |
| 鉴权要求 | 公开 |
| 内容类型 | 无请求体 |
| 数据来源 | 真实数据库（`tag`，`is_active=1`） |
| 幂等要求 | 幂等，可重复调用 |
| 事务要求 | 只读查询，无事务 |

#### 请求参数

无路径参数、无 Query 参数、无请求体。标签数量有限，不分页。

请求示例：

```http
GET /api/novel/tag/list HTTP/1.1
```

#### 成功响应

`data` 为数组（不分页）：

| 字段 | 类型 | 必返 | 说明 |
|---|---|---|---|
| `data[].tagId` | String | 是 | 标签 ID |
| `data[].tagName` | String | 是 | 标签名称 |

```json
{
  "code": 0,
  "message": "success",
  "data": [
    { "tagId": "3", "tagName": "科幻" },
    { "tagId": "7", "tagName": "热血" },
    { "tagId": "9", "tagName": "悬疑" }
  ]
}
```

#### 错误场景

| HTTP 状态 | 业务码 | 场景 |
|---|---:|---|
| 500 | 50000 | 数据库查询异常 |

### 4.7 创建作品

#### 基本信息

| 项目 | 内容 |
|---|---|
| 接口名称 | 创建作品 |
| 请求地址 | `/api/novel` |
| 请求方式 | POST |
| 接口说明 | 作者在创作台新建作品，写入 `novel` 与 `novel_tag` |
| 鉴权要求 | 登录用户，令牌 `role` 为 `AUTHOR` 或 `ADMIN` |
| 内容类型 | `application/json` |
| 数据来源 | 存储过程 `sp_create_novel` |
| 幂等要求 | 非幂等；无请求号，重复提交会创建多本作品，前端需在提交期间禁用按钮 |
| 事务要求 | 存储过程内部自行 `START TRANSACTION`/`COMMIT`，后端不得再包 `@Transactional`，避免双重事务 |

#### 请求参数（Body）

| 字段 | 类型 | 必填 | 示例 | 校验与说明 |
|---|---|---|---|---|
| `title` | String | 是 | `"星海归途"` | 去首尾空格后非空，长度 ≤ 120 |
| `coverUrl` | String | 否 | `"https://cdn.example.com/cover/1001.jpg"` | 长度 ≤ 255；不存二进制 |
| `description` | String | 否 | `"少年在星际废墟中……"` | 长度 ≤ 1000 |
| `tagIds` | Array\<String\> | 否 | `["3","7"]` | 标签 ID 字符串数组；元素须为启用标签，数组内不可重复，空数组表示不打标签 |

作者身份取自令牌，**请求体不接收 `authorId`**。作品创建后 `status=0`（连载中）、`audit_status=0`（待审核）、`total_words=0`。

请求示例：

```http
POST /api/novel HTTP/1.1
Authorization: Bearer <JWT>
Content-Type: application/json
```

```json
{
  "title": "星海归途",
  "coverUrl": "https://cdn.example.com/cover/1001.jpg",
  "description": "少年在星际废墟中捡到一枚旧式引擎，从此踏上归途。",
  "tagIds": ["3", "7"]
}
```

#### 成功响应

| 字段 | 类型 | 必返 | 说明 |
|---|---|---|---|
| `data.novelId` | String | 是 | 新作品 ID |
| `data.status` | Integer | 是 | 固定为 0（连载中） |
| `data.auditStatus` | Integer | 是 | 固定为 0（待审核） |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "novelId": "1002",
    "status": 0,
    "auditStatus": 0
  }
}
```

#### 错误场景

| HTTP 状态 | 业务码 | 场景 |
|---|---:|---|
| 400 | 40000 | `title` 缺失/为空/超 120 字符；`coverUrl` 超 255；`description` 超 1000；`tagIds` 元素非数字或数组内重复 |
| 401 | 40100 | 未携带令牌或令牌失效 |
| 403 | 40300 | 令牌角色为 `READER`；账号非 `ACTIVE`（锁定/禁用）；`sp_create_novel` 校验作者身份失败 |
| 400 | 40905 | `tagIds` 中存在不存在或已停用的标签（过程 `SIGNAL 45000`，整笔回滚） |
| 500 | 50000 | 未处理异常（不返回 SQL 细节） |

说明：`ADMIN` 令牌可通过鉴权，但 `sp_create_novel` 要求 `role='AUTHOR' AND status='ACTIVE'`，因此管理员无法代建作品，实际调用会返回 `40300`；需要管理员代建时须另立规则并改过程，不在本版本范围。

### 4.8 编辑作品

#### 基本信息

| 项目 | 内容 |
|---|---|
| 接口名称 | 编辑作品 |
| 请求地址 | `/api/novel/{novelId}` |
| 请求方式 | PUT |
| 接口说明 | 作者修改书名、封面、简介与标签，修改后作品重新进入待审核 |
| 鉴权要求 | 登录用户，令牌 `role` 为 `AUTHOR` 或 `ADMIN`；且为作品作者（`ADMIN` 除外） |
| 内容类型 | `application/json` |
| 数据来源 | 真实数据库（`novel`、`novel_tag`、`tag`）；无对应存储过程 |
| 幂等要求 | 结果幂等：相同 Payload 重复提交后数据一致，但每次都会把 `audit_status` 置 0 并刷新 `updated_at` |
| 事务要求 | 后端 `@Transactional` 单一事务：更新 `novel` → 删除旧 `novel_tag` → 写入新 `novel_tag`，任一步失败整体回滚 |

#### 请求参数

路径参数：

| 参数 | 类型 | 必填 | 示例 | 说明 |
|---|---|---|---|---|
| `novelId` | String | 是 | `"1001"` | 作品 ID |

请求体（PUT 为整体覆盖，四个字段均需提交；`null` 表示清空）：

| 字段 | 类型 | 必填 | 示例 | 校验与说明 |
|---|---|---|---|---|
| `title` | String | 是 | `"星海归途（修订版）"` | 去首尾空格后非空，≤ 120 |
| `coverUrl` | String | 是 | `"https://cdn.example.com/cover/1001-v2.jpg"` | 可为 `null` 表示清除封面，≤ 255 |
| `description` | String | 是 | `"修订后的简介……"` | 可为 `null`，≤ 1000 |
| `tagIds` | Array\<String\> | 是 | `["3","9"]` | 传 `[]` 表示清空标签；元素须为启用标签，数组内不可重复 |

本接口不改 `status`、`total_words`、`latest_chapter_id`，也不接收 `authorId`。

请求示例：

```http
PUT /api/novel/1001 HTTP/1.1
Authorization: Bearer <JWT>
Content-Type: application/json
```

```json
{
  "title": "星海归途（修订版）",
  "coverUrl": "https://cdn.example.com/cover/1001-v2.jpg",
  "description": "修订后的简介：归途从一枚旧引擎开始。",
  "tagIds": ["3", "9"]
}
```

#### 成功响应

| 字段 | 类型 | 必返 | 说明 |
|---|---|---|---|
| `data.novelId` | String | 是 | 作品 ID |
| `data.auditStatus` | Integer | 是 | 固定为 0（重新待审核） |
| `data.updatedAt` | String | 是 | 本次更新时间 |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "novelId": "1001",
    "auditStatus": 0,
    "updatedAt": "2026-11-24T20:30:00+08:00"
  }
}
```

#### 错误场景

| HTTP 状态 | 业务码 | 场景 |
|---|---:|---|
| 400 | 40000 | `title` 为空或超长；`tagIds` 元素非数字/重复；缺字段 |
| 401 | 40100 | 未携带令牌或令牌失效 |
| 403 | 40300 | 令牌角色非 `AUTHOR`/`ADMIN` |
| 403 | 40902 | 当前用户不是该作品作者 |
| 404 | 40901 | 作品不存在 |
| 400 | 40905 | 标签不存在或已停用（整笔回滚，旧标签不受影响） |
| 500 | 50000 | 未处理异常 |

### 4.9 我的作品列表

#### 基本信息

| 项目 | 内容 |
|---|---|
| 接口名称 | 我的作品 |
| 请求地址 | `/api/novel/author/list` |
| 请求方式 | GET |
| 接口说明 | 作者创作台列出本人全部作品，含未过审、驳回与已下架 |
| 鉴权要求 | 登录用户，令牌 `role` 为 `AUTHOR` 或 `ADMIN`；作者身份取自令牌 |
| 内容类型 | 无请求体 |
| 数据来源 | 真实数据库（`novel`、`chapter`、`novel_tag`、`tag`） |
| 幂等要求 | 幂等，可重复调用 |
| 事务要求 | 只读查询，无事务 |

#### 请求参数（Query）

| 参数 | 类型 | 必填 | 默认值 | 示例 | 校验与说明 |
|---|---|---|---|---|---|
| `page` | Integer | 否 | 1 | `1` | 从 1 开始 |
| `pageSize` | Integer | 否 | 10 | `10` | 1～100 |

排序：`novel.updated_at DESC`（命中 `idx_author_update`）。`ADMIN` 调用时按其令牌用户 ID 查询本人作品，不提供代查他人作品参数。

请求示例：

```http
GET /api/novel/author/list?page=1&pageSize=10 HTTP/1.1
Authorization: Bearer <JWT>
```

#### 成功响应

| 字段 | 类型 | 必返 | 说明 |
|---|---|---|---|
| `data.list[].novelId` | String | 是 | 作品 ID |
| `data.list[].title` | String | 是 | 书名 |
| `data.list[].coverUrl` | String | 否 | 封面，可为 `null` |
| `data.list[].status` | Integer | 是 | 0 连载中 / 1 已完结 / 2 已下架 |
| `data.list[].auditStatus` | Integer | 是 | 0 待审核 / 1 通过 / 2 驳回 |
| `data.list[].totalWords` | Integer | 是 | 已发布章节总字数 |
| `data.list[].chapterCount` | Integer | 是 | 已发布章节数（仅统计 `status=1`） |
| `data.list[].latestChapterId` | String | 否 | 最新已发布章节 ID，可为 `null` |
| `data.list[].tags` | Array | 是 | `[{ tagId, tagName }]`，无标签为 `[]` |
| `data.list[].createdAt` | String | 是 | 创建时间 |
| `data.list[].updatedAt` | String | 是 | 更新时间 |
| `data.total` / `data.page` / `data.pageSize` | Integer | 是 | 分页信息 |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "novelId": "1001",
        "title": "星海归途",
        "coverUrl": "https://cdn.example.com/cover/1001.jpg",
        "status": 0,
        "auditStatus": 1,
        "totalWords": 128000,
        "chapterCount": 21,
        "latestChapterId": "88021",
        "tags": [{ "tagId": "3", "tagName": "科幻" }],
        "createdAt": "2026-10-01T09:00:00+08:00",
        "updatedAt": "2026-11-24T20:15:30+08:00"
      }
    ],
    "total": 1,
    "page": 1,
    "pageSize": 10
  }
}
```

#### 错误场景

| HTTP 状态 | 业务码 | 场景 |
|---|---:|---|
| 400 | 40000 | `pageSize` 超出 1～100 |
| 401 | 40100 | 未携带令牌或令牌失效 |
| 403 | 40300 | 令牌角色非 `AUTHOR`/`ADMIN` |
| 500 | 50000 | 数据库查询异常 |

### 4.10 作者视角章节列表

#### 基本信息

| 项目 | 内容 |
|---|---|
| 接口名称 | 我的章节列表 |
| 请求地址 | `/api/novel/author/{novelId}/chapters` |
| 请求方式 | GET |
| 接口说明 | 创作台管理某本作品的章节，含草稿、待审、驳回、已发布与已下架 |
| 鉴权要求 | 登录用户，令牌 `role` 为 `AUTHOR` 或 `ADMIN`；且为作品作者（`ADMIN` 除外） |
| 内容类型 | 无请求体 |
| 数据来源 | 真实数据库（`chapter`、`novel`） |
| 幂等要求 | 幂等，可重复调用 |
| 事务要求 | 只读查询，无事务 |

#### 请求参数

路径参数：

| 参数 | 类型 | 必填 | 示例 | 说明 |
|---|---|---|---|---|
| `novelId` | String | 是 | `"1001"` | 作品 ID |

Query 参数：

| 参数 | 类型 | 必填 | 默认值 | 示例 | 校验与说明 |
|---|---|---|---|---|---|
| `page` | Integer | 否 | 1 | `1` | 从 1 开始 |
| `pageSize` | Integer | 否 | 10 | `10` | 1～100 |

排序：`chapter_no ASC`。**不返回 `content`**，只返回元数据；正文在编辑页单独按章节读取（本版本由 `GET /api/novel/chapter/{chapterId}` 获取已发布章节正文，草稿正文请在保存后回显本地内容）。

请求示例：

```http
GET /api/novel/author/1001/chapters?page=1&pageSize=10 HTTP/1.1
Authorization: Bearer <JWT>
```

#### 成功响应

| 字段 | 类型 | 必返 | 说明 |
|---|---|---|---|
| `data.list[].chapterId` | String | 是 | 章节 ID |
| `data.list[].chapterNo` | Integer | 是 | 章节序号 |
| `data.list[].title` | String | 是 | 章节标题 |
| `data.list[].isVip` | Integer | 是 | 0 免费 / 1 VIP |
| `data.list[].vipPrice` | Number | 是 | VIP 价格（元） |
| `data.list[].wordCount` | Integer | 是 | 章节字数 |
| `data.list[].status` | Integer | 是 | 0 草稿/未发布 / 1 已发布 / 2 已下架 |
| `data.list[].auditStatus` | Integer | 否 | `null` 未提交 / 0 待审核 / 1 通过 / 2 驳回 |
| `data.list[].publishTime` | String | 否 | 发布时间，未发布为 `null` |
| `data.list[].createdAt` | String | 是 | 创建时间 |
| `data.list[].updatedAt` | String | 是 | 更新时间 |
| `data.total` / `data.page` / `data.pageSize` | Integer | 是 | 分页信息 |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "chapterId": "88001",
        "chapterNo": 1,
        "title": "第一章 废墟信号",
        "isVip": 0,
        "vipPrice": 0.00,
        "wordCount": 3120,
        "status": 1,
        "auditStatus": 1,
        "publishTime": "2026-10-02T12:00:00+08:00",
        "createdAt": "2026-10-01T10:00:00+08:00",
        "updatedAt": "2026-10-02T12:00:00+08:00"
      },
      {
        "chapterId": "88022",
        "chapterNo": 22,
        "title": "第二十二章 归途",
        "isVip": 0,
        "vipPrice": 0.00,
        "wordCount": 2980,
        "status": 0,
        "auditStatus": null,
        "publishTime": null,
        "createdAt": "2026-11-24T20:40:00+08:00",
        "updatedAt": "2026-11-24T20:40:00+08:00"
      }
    ],
    "total": 2,
    "page": 1,
    "pageSize": 10
  }
}
```

#### 错误场景

| HTTP 状态 | 业务码 | 场景 |
|---|---:|---|
| 400 | 40000 | `novelId` 非法、`pageSize` 越界 |
| 401 | 40100 | 未携带令牌或令牌失效 |
| 403 | 40300 | 令牌角色非 `AUTHOR`/`ADMIN` |
| 403 | 40902 | 当前用户不是该作品作者 |
| 404 | 40901 | 作品不存在 |

### 4.11 新建章节草稿

#### 基本信息

| 项目 | 内容 |
|---|---|
| 接口名称 | 新建章节草稿 |
| 请求地址 | `/api/novel/chapter` |
| 请求方式 | POST |
| 接口说明 | 作者在作品下新建章节，保存为草稿并自动分配章节序号 |
| 鉴权要求 | 登录用户，令牌 `role` 为 `AUTHOR` 或 `ADMIN`；且为作品作者（`ADMIN` 除外） |
| 内容类型 | `application/json` |
| 数据来源 | 存储过程 `sp_save_chapter_draft`（`p_chapter_id = NULL`） |
| 幂等要求 | 非幂等；重复提交会创建多个草稿并占用章节序号，前端需防重复点击 |
| 事务要求 | 存储过程内部自管事务（先 `novel` 行 `FOR UPDATE` 再写 `chapter`），后端不得再包 `@Transactional` |

#### 请求参数（Body）

| 字段 | 类型 | 必填 | 示例 | 校验与说明 |
|---|---|---|---|---|
| `novelId` | String | 是 | `"1001"` | 目标作品 ID；后端先校验作品存在且属于当前作者 |
| `title` | String | 是 | `"第二十二章 归途"` | 去首尾空格后非空，≤ 120 |
| `content` | String | 是 | `"引擎熄火后，他看见了熟悉的星图……"` | 正文，非空；`wordCount` 由过程按 `CHAR_LENGTH(content)` 计算 |
| `isVip` | Integer | 是 | `0` | 只能为 0 或 1 |
| `vipPrice` | Number | 是 | `0.00` | `isVip=0` 时必须为 `0.00`；`isVip=1` 时必须大于 `0.00`，两位小数 |

`chapter_no` 取值 `MAX(chapter_no)+1`，由过程在锁住 `novel` 行后分配。新建后 `status=0`（草稿）、`audit_status=NULL`（未提交）、`publish_time=NULL`。

请求示例：

```http
POST /api/novel/chapter HTTP/1.1
Authorization: Bearer <JWT>
Content-Type: application/json
```

```json
{
  "novelId": "1001",
  "title": "第二十二章 归途",
  "content": "引擎熄火后，他看见了熟悉的星图……",
  "isVip": 0,
  "vipPrice": 0.00
}
```

#### 成功响应

| 字段 | 类型 | 必返 | 说明 |
|---|---|---|---|
| `data.chapterId` | String | 是 | 新章节 ID |
| `data.novelId` | String | 是 | 所属作品 ID |
| `data.chapterNo` | Integer | 是 | 自动分配的章节序号 |
| `data.wordCount` | Integer | 是 | 过程计算的字数 |
| `data.status` | Integer | 是 | 固定为 0（草稿） |
| `data.auditStatus` | Integer | 是 | 固定为 `null`（未提交） |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "chapterId": "88022",
    "novelId": "1001",
    "chapterNo": 22,
    "wordCount": 2980,
    "status": 0,
    "auditStatus": null
  }
}
```

#### 错误场景

| HTTP 状态 | 业务码 | 场景 |
|---|---:|---|
| 400 | 40000 | `title` 为空/超长；`content` 缺失；`isVip` 非 0/1；`vipPrice` 与 `isVip` 不一致（过程 `SIGNAL 45000`） |
| 401 | 40100 | 未携带令牌或令牌失效 |
| 403 | 40300 | 令牌角色非 `AUTHOR`/`ADMIN` |
| 403 | 40902 | 当前用户不是 `novelId` 对应作品的作者 |
| 404 | 40901 | `novelId` 对应作品不存在 |
| 409 | 40906 | 并发新建导致 `uk_novel_chapter` 唯一键冲突（过程转换为该提示，可重试一次） |
| 500 | 50000 | 未处理异常 |

### 4.12 修改章节

#### 基本信息

| 项目 | 内容 |
|---|---|
| 接口名称 | 修改章节 |
| 请求地址 | `/api/novel/chapter/{chapterId}` |
| 请求方式 | PUT |
| 接口说明 | 作者修改章节标题、正文与 VIP 设置；已发布章节改稿会自动下线并重新待审 |
| 鉴权要求 | 登录用户，令牌 `role` 为 `AUTHOR` 或 `ADMIN`；且为所属作品作者（`ADMIN` 除外） |
| 内容类型 | `application/json` |
| 数据来源 | 存储过程 `sp_save_chapter_draft`（传入 `p_chapter_id`） |
| 幂等要求 | 结果幂等：相同内容重复提交数据一致；但每次都可能触发下线与重新待审，重复调用不会重复回减字数 |
| 事务要求 | 存储过程内部自管事务（锁顺序 `novel` → `chapter`），后端不得再包 `@Transactional` |

#### 请求参数

路径参数：

| 参数 | 类型 | 必填 | 示例 | 说明 |
|---|---|---|---|---|
| `chapterId` | String | 是 | `"88022"` | 章节 ID |

请求体（章节所属作品由服务端按 `chapterId` 反查，**不接收 `novelId`**）：

| 字段 | 类型 | 必填 | 示例 | 校验与说明 |
|---|---|---|---|---|
| `title` | String | 是 | `"第二十二章 归途（修）"` | 去首尾空格后非空，≤ 120 |
| `content` | String | 是 | `"引擎熄火后，他看见了熟悉的星图与旧航线……"` | 正文，非空；重新计算字数 |
| `isVip` | Integer | 是 | `1` | 只能为 0 或 1 |
| `vipPrice` | Number | 是 | `10.00` | 与 `isVip` 必须匹配：非 VIP 为 `0.00`，VIP 大于 `0.00` |

处理规则（依据 `sp_save_chapter_draft`）：

- 已发布章节（`status=1`）改稿：先将 `status` 置 0，按旧 `word_count` 回减 `novel.total_words`，并重算 `latest_chapter_id`，再写入新内容。
- 审核状态：原 `audit_status` 为 `NULL`（草稿）时保持 `NULL`；原为 0/1/2 时统一重置为 `0`（重新待审）。
- 已下架章节（`status=2`）禁止编辑，返回 `40904`。

请求示例：

```http
PUT /api/novel/chapter/88022 HTTP/1.1
Authorization: Bearer <JWT>
Content-Type: application/json
```

```json
{
  "title": "第二十二章 归途（修）",
  "content": "引擎熄火后，他看见了熟悉的星图与旧航线……",
  "isVip": 1,
  "vipPrice": 10.00
}
```

#### 成功响应

| 字段 | 类型 | 必返 | 说明 |
|---|---|---|---|
| `data.chapterId` | String | 是 | 章节 ID |
| `data.chapterNo` | Integer | 是 | 章节序号（不变） |
| `data.wordCount` | Integer | 是 | 修改后的字数 |
| `data.status` | Integer | 是 | 修改后的状态（已发布改稿后为 0） |
| `data.auditStatus` | Integer | 否 | 修改后的审核状态（草稿为 `null`，其余为 0） |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "chapterId": "88022",
    "chapterNo": 22,
    "wordCount": 3050,
    "status": 0,
    "auditStatus": null
  }
}
```

#### 错误场景

| HTTP 状态 | 业务码 | 场景 |
|---|---:|---|
| 400 | 40000 | `title` 为空/超长；`content` 缺失；`isVip` 非 0/1；`vipPrice` 与 `isVip` 不一致 |
| 401 | 40100 | 未携带令牌或令牌失效 |
| 403 | 40300 | 令牌角色非 `AUTHOR`/`ADMIN` |
| 403 | 40902 | 当前用户不是该章节所属作品的作者 |
| 404 | 40903 | 章节不存在 |
| 409 | 40904 | 章节已下架（`status=2`），不可编辑（提示“已下架章节不可编辑，请新建章节”） |
| 500 | 50000 | 未处理异常 |

### 4.13 发布章节

#### 基本信息

| 项目 | 内容 |
|---|---|
| 接口名称 | 发布章节 |
| 请求地址 | `/api/novel/chapter/{chapterId}/publish` |
| 请求方式 | POST |
| 接口说明 | 将审核通过且未发布的章节发布；成功后按章节字数累加作品总字数并更新最新章节 |
| 鉴权要求 | 登录用户，令牌 `role` 为 `AUTHOR` 或 `ADMIN`；且为所属作品作者（`ADMIN` 除外） |
| 内容类型 | 无请求体 |
| 数据来源 | 存储过程 `sp_publish_chapter` |
| 幂等要求 | 非幂等；重复发布会返回 `40904`（请勿重复发布） |
| 事务要求 | 存储过程内部自管事务（锁顺序 `novel` → `chapter`），后端不得再包 `@Transactional` |

#### 请求参数

路径参数：

| 参数 | 类型 | 必填 | 示例 | 说明 |
|---|---|---|---|---|
| `chapterId` | String | 是 | `"88022"` | 章节 ID |

请求示例：

```http
POST /api/novel/chapter/88022/publish HTTP/1.1
Authorization: Bearer <JWT>
```

#### 成功响应

| 字段 | 类型 | 必返 | 说明 |
|---|---|---|---|
| `data.chapterId` | String | 是 | 章节 ID |
| `data.novelId` | String | 是 | 所属作品 ID |
| `data.chapterNo` | Integer | 是 | 章节序号 |
| `data.wordCount` | Integer | 是 | 本章字数 |
| `data.status` | Integer | 是 | 发布后状态，固定为 `1` |
| `data.publishTime` | String | 是 | 发布时间，ISO 8601（如 `2026-11-24T20:15:30+08:00`） |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "chapterId": "88022",
    "novelId": "77001",
    "chapterNo": 22,
    "wordCount": 3120,
    "status": 1,
    "publishTime": "2026-11-24T20:15:30+08:00"
  }
}
```

#### 错误响应

| HTTP 状态 | 业务码 | 场景 |
|---|---:|---|
| 401 | 40100 | 未携带令牌或令牌失效 |
| 403 | 40300 | 令牌角色非 `AUTHOR`/`ADMIN` |
| 403 | 40902 | 当前用户不是该章节所属作品的作者 |
| 404 | 40903 | 章节不存在 |
| 409 | 40904 | 章节未通过审核、已发布或已下架 |
| 500 | 50000 | 未处理异常 |

### 4.14 下架章节

#### 基本信息

| 项目 | 内容 |
|---|---|
| 接口名称 | 下架章节 |
| 请求地址 | `/api/novel/chapter/{chapterId}/offline` |
| 请求方式 | POST |
| 接口说明 | 将已发布章节下架（`status=2`），回减作品总字数并重算最新章节；章节数据保留，不做物理删除 |
| 鉴权要求 | 登录用户，令牌 `role` 为 `AUTHOR` 或 `ADMIN`；且为所属作品作者（`ADMIN` 除外） |
| 内容类型 | 无请求体 |
| 数据来源 | 存储过程 `sp_offline_chapter` |
| 幂等要求 | 非幂等；重复下架返回 `40904`（章节未发布或已下架） |
| 事务要求 | 存储过程内部自管事务，后端不得再包 `@Transactional` |

#### 请求参数

路径参数：

| 参数 | 类型 | 必填 | 示例 | 说明 |
|---|---|---|---|---|
| `chapterId` | String | 是 | `"88022"` | 章节 ID |

请求示例：

```http
POST /api/novel/chapter/88022/offline HTTP/1.1
Authorization: Bearer <JWT>
```

#### 成功响应

| 字段 | 类型 | 必返 | 说明 |
|---|---|---|---|
| `data.chapterId` | String | 是 | 章节 ID |
| `data.novelId` | String | 是 | 所属作品 ID |
| `data.status` | Integer | 是 | 下架后状态，固定为 `2` |
| `data.totalWords` | Integer | 是 | 下架后作品总字数 |
| `data.latestChapterId` | String | 否 | 下架后最新章节 ID，无已发布章节时为 `null` |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "chapterId": "88022",
    "novelId": "77001",
    "status": 2,
    "totalWords": 28600,
    "latestChapterId": "88021"
  }
}
```

#### 错误响应

| HTTP 状态 | 业务码 | 场景 |
|---|---:|---|
| 401 | 40100 | 未携带令牌或令牌失效 |
| 403 | 40300 | 令牌角色非 `AUTHOR`/`ADMIN` |
| 403 | 40902 | 当前用户不是该章节所属作品的作者 |
| 404 | 40903 | 章节不存在 |
| 409 | 40904 | 章节未发布或已下架 |
| 500 | 50000 | 未处理异常 |

### 4.15 作者章节详情（含正文）

#### 基本信息

| 项目 | 内容 |
|---|---|
| 接口名称 | 作者章节详情 |
| 请求地址 | `/api/novel/author/chapter/{chapterId}` |
| 请求方式 | GET |
| 接口说明 | 供创作台改稿回显：返回本人作品的章节完整信息（含正文），不受章节发布状态限制 |
| 鉴权要求 | 登录用户，令牌 `role` 为 `AUTHOR` 或 `ADMIN`；且为所属作品作者（`ADMIN` 除外） |
| 内容类型 | 无 |
| 数据来源 | 真实数据库（单行查询） |
| 幂等要求 | 天然幂等 |
| 事务要求 | 只读，无需事务 |

#### 请求参数

路径参数：

| 参数 | 类型 | 必填 | 示例 | 说明 |
|---|---|---|---|---|
| `chapterId` | String | 是 | `"88022"` | 章节 ID |

请求示例：

```http
GET /api/novel/author/chapter/88022 HTTP/1.1
Authorization: Bearer <JWT>
```

#### 成功响应

| 字段 | 类型 | 必返 | 说明 |
|---|---|---|---|
| `data.chapterId` | String | 是 | 章节 ID |
| `data.novelId` | String | 是 | 所属作品 ID |
| `data.chapterNo` | Integer | 是 | 章节序号 |
| `data.title` | String | 是 | 章节标题 |
| `data.content` | String | 是 | 章节正文 |
| `data.isVip` | Integer | 是 | 是否 VIP：0 否，1 是 |
| `data.vipPrice` | Number | 是 | VIP 单价（元），非 VIP 为 `0.00` |
| `data.wordCount` | Integer | 是 | 字数 |
| `data.status` | Integer | 是 | 0 草稿 / 1 已发布 / 2 已下架 |
| `data.auditStatus` | Integer | 否 | `null` 未提交、0 待审、1 通过、2 驳回 |
| `data.publishTime` | String | 否 | 发布时间，ISO 8601 |
| `data.updatedAt` | String | 是 | 最后更新时间，ISO 8601 |

#### 错误响应

| HTTP 状态 | 业务码 | 场景 |
|---|---:|---|
| 401 | 40100 | 未携带令牌或令牌失效 |
| 403 | 40300 | 令牌角色非 `AUTHOR`/`ADMIN` |
| 403 | 40902 | 当前用户不是该章节所属作品的作者 |
| 404 | 40903 | 章节不存在 |
| 500 | 50000 | 未处理异常 |

