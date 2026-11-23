# C 互动与榜单 · SQL 说明

- 模块：C（互动与榜单）
- 版本：v1.0
- 日期：2026-11-23
- 负责人：C
- 依据：`docs/current/D_数据字典_v1.0_20261120.xlsx`（冻结基线）、`docs/current/er/全局_ER图_v1.0_20261120.drawio`
- 数据库：MySQL 8.0.16+，InnoDB，utf8mb4 / utf8mb4_0900_ai_ci

---

## 1. 交付清单

| 文件 | 目录 | 作用 |
|---|---|---|
| `C_互动与榜单_建表_v1.0_20261121.sql` | `sql/ddl/` | 6 张表、20 个索引、12 个外键及 CHECK 约束 |
| `C_互动与榜单_触发器_v1.0_20261121.sql` | `sql/objects/` | 8 个触发器：点赞计数、评分统计、审核重审 |
| `C_互动与榜单_视图与存储过程_v1.0_20261121.sql` | `sql/objects/` | 4 个视图 + 9 个存储过程 |
| `C_互动与榜单_评论接口说明_v1.0_20261123.md` | `api/` | 评论统一写入、调用方事务及 C/D 审核集成约束 |
| `C_互动与榜单_测试前置_v1.0_20261121.sql` | `test/` | 本地验证用的 A/B 最小前置表（不属交付基线） |
| `C_互动与榜单_测试数据_v1.0_20261121.sql` | `test/` | 少量 INSERT + 触发器/视图/存储过程验证 |
| `C_互动与榜单_执行结果_v1.0_20261121.md` | `test/` | 实际执行结果留档 |

---

## 2. 执行顺序与环境

```text
1) A_用户交易_建表_*.sql        -- 提供 app_user
2) B_作品内容_建表_*.sql        -- 提供 novel / chapter
3) sql/ddl/C_互动与榜单_建表_v1.0_20261121.sql
4) sql/objects/C_互动与榜单_触发器_v1.0_20261121.sql
5) sql/objects/C_互动与榜单_视图与存储过程_v1.0_20261121.sql
```

- 建表脚本用 `CREATE TABLE IF NOT EXISTS`，可重复执行；对象脚本先 `DROP ... IF EXISTS` 再创建。
- 字符集统一 utf8mb4，避免书名、评论中的生僻字与表情符乱码。
- 时间口径遵循数据字典：C 模块使用 `DATETIME`（A 模块为 `DATETIME(6)`）。

---

## 3. 表清单（6 表 / 50 字段 / 20 索引）

| 表名 | 中文名 | 字段数 | 主键 | 二级索引 | 说明 |
|---|---|---|---|---|---|
| `comment` | 评论表 | 14 | `comment_id` | 5 | 整本书评、本章说；仅一级评论和直接回复，`parent_id` 自关联 |
| `comment_like` | 评论点赞表 | 4 | `like_id` | 2（含 1 唯一） | `uk(comment_id,user_id)` 防重复点赞 |
| `novel_rating` | 小说评分表 | 9 | `rating_id` | 3（含 1 唯一） | `uk(user_id,novel_id)` 一人一书一条 |
| `novel_rating_stat` | 评分统计表 | 5 | `novel_id` | 0 | 与 novel 一对一，唯一权威评分统计 |
| `ranking_config` | 榜单配置表 | 10 | `ranking_id` | 1（唯一） | `ranking_code` 唯一 |
| `ranking_item` | 榜单条目表 | 8 | `item_id` | 4（含 2 唯一） | 同期同书唯一、同期名次唯一 |

索引小计：数据字典《索引清单》共 20 个显式索引（含 6 个主键、5 个唯一键）。另外，`comment.reply_to_user_id` 在索引清单中未定义索引，MySQL 会为该外键自动建立隐式索引 `fk_comment_reply_user`，因此实际索引对象为 21 个，属数据库强制行为，不算偏离字典。

---

## 4. 外键依赖（12 个物理外键）

| 约束名 | 子表.字段 | 父表.字段 | 删除策略 |
|---|---|---|---|
| `fk_comment_novel` | comment.novel_id | novel.novel_id | RESTRICT / CASCADE(update) |
| `fk_comment_chapter` | comment.chapter_id | chapter.chapter_id | RESTRICT |
| `fk_comment_user` | comment.user_id | app_user.user_id | RESTRICT |
| `fk_comment_parent` | comment.parent_id | comment.comment_id（自关联） | RESTRICT |
| `fk_comment_reply_user` | comment.reply_to_user_id | app_user.user_id | RESTRICT |
| `fk_comment_like_comment` | comment_like.comment_id | comment.comment_id | RESTRICT |
| `fk_comment_like_user` | comment_like.user_id | app_user.user_id | RESTRICT |
| `fk_novel_rating_user` | novel_rating.user_id | app_user.user_id | RESTRICT |
| `fk_novel_rating_novel` | novel_rating.novel_id | novel.novel_id | RESTRICT |
| `fk_rating_stat_novel` | novel_rating_stat.novel_id | novel.novel_id | RESTRICT |
| `fk_ranking_item_config` | ranking_item.ranking_id | ranking_config.ranking_id | RESTRICT |
| `fk_ranking_item_novel` | ranking_item.novel_id | novel.novel_id | RESTRICT |

说明：小说与章节采用下架/软删除，全部使用 `ON DELETE RESTRICT`，禁止级联物理删除；`ON UPDATE` 统一 `CASCADE`。

---

## 5. 特殊约束与业务口径

1. **评论结构与可见性**：仅允许一级评论和直接回复，不允许对回复继续嵌套。评论自身必须 `audit_status = 1 AND status = 1`；直接回复还要求其父评论是一级评论且满足相同条件。历史第三级及更深层评论即使自身和直接父评论正常，也不在展示视图中出现；一级评论不可见时其回复均不展示，但保留数据库记录。`status = 3` 为软删除，不物理删除。一级评论的章节须属于所评小说；回复与父评论的 `novel_id`、`chapter_id` 必须严格一致，章节 NULL 与非 NULL 不允许混用。
2. **审核重审**：评论内容修改后 `audit_status` 重新置 0；评分短评新增/修改后 `review_audit_status` 重新置 0，`''` 统一存 `NULL`。由 BEFORE 触发器实现。
3. **评分口径**：`novel_rating_stat` 为唯一权威统计，仅统计 `status = 1` 的评分，不按短评审核状态过滤；短评审核不影响数值评分。
4. **计数口径**：`comment.like_count` 由点赞触发器维护；`reply_count` 表示回复自身当前可展示的直接回复数量，仅统计 `parent_id` 指向该评论且 `status = 1 AND audit_status = 1` 的回复。父评论是否可见不计入该公式，父评论隐藏/恢复只改变视图展示，不级联修改子回复计数。
5. **榜单口径**：同一榜单同一期次重复生成时先清空该期次再写入，保证幂等；只收录 `novel.status <> 2`（未下架）的作品；`period_no` 在同一榜单内必须保持统一格式（如 `202611`、`2026W47`、`20261127`），字典序即时间序。
6. **收藏口径**：收藏/书架唯一数据源为 A 模块 `user_bookshelf`，C 模块只读取统计，不重复建收藏表。

---

## 6. 触发器说明（8 个）

| 触发器 | 时机/事件 | 作用 |
|---|---|---|
| `trg_comment_like_ai` | AFTER INSERT ON comment_like | `comment.like_count + 1` |
| `trg_comment_like_ad` | AFTER DELETE ON comment_like | `comment.like_count - 1`（下限 0，取消点赞为物理删除） |
| `trg_comment_bu` | BEFORE UPDATE ON comment | 评论内容变更时 `audit_status` 置 0 |
| `trg_novel_rating_bi` | BEFORE INSERT ON novel_rating | `''` 短评转 `NULL`；有短评时待审核 0 |
| `trg_novel_rating_bu` | BEFORE UPDATE ON novel_rating | 短评规范化；内容变更后重新置 0 / 无短评置 NULL |
| `trg_novel_rating_ai` | AFTER INSERT ON novel_rating | 新增有效评分，增量累加统计 |
| `trg_novel_rating_au` | AFTER UPDATE ON novel_rating | 先扣旧贡献再累加新贡献（改分/隐藏） |
| `trg_novel_rating_ad` | AFTER DELETE ON novel_rating | 删除有效评分，扣减统计 |

**为什么 `reply_count` 不用自表触发器**：MySQL 禁止触发器更新触发该语句的同一张表（ERROR 1442），因此不能用回复行的触发器直接更新父评论。评论新增、内容修改、审核和状态变更统一通过数据库写入过程执行，根据 OLD/NEW 自身可见性精确 `+1/-1`；评论操作与计数更新由调用方放在同一事务中。`reply_count` 属于冻结数据字典字段，本次继续物理存储；`sp_sync_comment_reply_count` 仅保留为后台对账/修复工具，正常业务不得显式调用。

---

## 7. 视图与存储过程

### 视图

| 视图 | 作用 |
|---|---|
| `v_comment_visible` | 仅展示一级评论和二级回复；回复的一级父评论须审核通过、状态正常 |
| `v_novel_rating_summary` | 小说 + 评分统计联表，评分页/榜单直接取用 |
| `v_ranking_item_detail` | 榜单条目 + 榜单元信息 + 书名 |
| `v_ranking_latest` | 每榜最新期次明细 |

### 存储过程

| 过程 | 入参 / 出参 | 作用 |
|---|---|---|
| `sp_sync_comment_reply_count` | IN `p_comment_id` | 全量重算直接回复数，仅用于后台对账/修复 |
| `sp_apply_comment_reply_delta` | IN 父评论及 OLD/NEW 状态、审核状态 | 私有增量辅助过程，仅处理同一父评论下的实际可见性迁移 |
| `sp_create_comment` | IN 小说、章节、用户、父评论、回复对象、内容；OUT `p_comment_id` | 新增一级评论/直接回复，统一初始待审核；校验章节归属、两级限制及回复归属 |
| `sp_edit_comment_content` | IN `p_comment_id`, `p_content` | 修改内容，配合触发器重新送审并维护计数 |
| `sp_audit_comment` | IN `p_comment_id`, `p_audit_status` | 待审/通过/驳回，重复设置不重复计数 |
| `sp_set_comment_status` | IN `p_comment_id`, `p_status` | 正常/隐藏/恢复/软删除并维护计数 |
| `sp_refresh_rating_stat` | IN `p_novel_id` | 按明细重算评分统计，用于对账与历史回填 |
| `sp_generate_ranking` | IN `p_ranking_id`, `p_period_no`, `p_allow_history_overwrite`；OUT `p_item_count` | 生成某榜单某期次名次快照 |
| `sp_get_ranking` | IN `p_ranking_code`, `p_period_no`（空取最新）, `p_limit` | 查询榜单明细 |

评论写入过程及 `sp_generate_ranking` 均不自行开启、提交或回滚事务，统一由调用方管理事务边界。评论更新入口先以 `FOR UPDATE` 锁定目标评论，UPDATE 后重读实际状态；调用方应将评论操作、计数更新及必要的前置写入放在同一事务中，异常时整体回滚。

应用层不得直接 INSERT/UPDATE/DELETE `comment`，也不得绕开过程直接修改 `parent_id`、`status`、`audit_status` 或 `reply_count`；评论新增、编辑、审核和状态变更必须调用统一写入过程。**这是应用层约束，不是数据库强制限制**，数据库不会对具备表权限的账号自动阻止直接 DML。

C 模块不再维护独立数据库账号、角色或过程权限体系，管理员鉴权由应用层负责。`sp_audit_comment` 保留审核状态变更；应用层执行管理员审核时，应在同一事务中向 D 模块 `audit_log` 追加 `target_type = 'COMMENT'` 的审核日志。评论内容修改仍由触发器重新送审，D 模块审核日志表及约束不变。

`sp_generate_ranking` 指标口径：

| ranking_type | 数据源 | 指标值 |
|---|---|---|
| `FAVORITE` | `user_bookshelf` | 收藏人数（COUNT） |
| `RATING` | `novel_rating_stat` | 平均分（0.00~5.00） |
| `TICKET` | `reward_tip` | 成功打赏金额合计 |
| `SALE` | `chapter_subscription` | 有效订阅实付金额合计 |

`score = 指标值 × weight_config 中对应权重`（缺省 1.0），名次按 `score DESC, novel_id ASC` 排列，取 `top_n` 条。

---

## 8. 与数据字典的口径说明及待确认项

1. **`ranking_type` / `period_type` 索引**：字段清单标记为“是”，但《索引清单》未列出这两个字段的索引定义；本实现以索引清单为准（仅建 `uk_ranking_code`）。若 D 需要按类型筛选榜单，建议评审后补 `idx_ranking_config_type(ranking_type, period_type, is_active)`。
2. **榜单类型范围**：当前仅保留已实现的 `FAVORITE`、`RATING`、`TICKET`、`SALE`；阅读榜暂不纳入字段备注、CHECK 或生成过程，后续须先确定阅读统计数据源再恢复。
3. **期次口径**：`period_no` 目前只作为期次标签，指标为生成时刻的累计值；如需“自然月/自然周”窗口切片，需要评审确定 `period_no` 与时间区间的映射规则后再扩展。
4. **榜单权重**：`weight_config` 只对当前 `ranking_type` 对应键取值；多指标加权榜单需要新增类型并在评审中明确口径。

---

## 9. 验证结论

已在隔离的 MySQL 测试实例（26.7.0，端口 33307，库 `c_module_test`）执行原有 5 个 C 模块脚本及既有 D 模块建表脚本，共 6 个 SQL 文件，退出码均为 0；6 张 C 模块表、20 个显式索引、12 个外键、8 个触发器、4 个视图、9 个正式存储过程全部创建成功。评论回复数状态迁移、重复更新、异常回滚和全量对账断言均通过；历史第三级、第四级评论均不展示；一级评论隐藏时其直接回复不可见，恢复后按自身状态重新展示；第三级回复、一级评论跨小说章节归属及回复归属不一致均被拒绝且数据不变。C/D 审核状态与日志同事务提交、日志 CHECK 失败整体回滚均已实测通过。调用方外层回滚可撤销评论、榜单及前置写入，显式提交可正常持久化。评分关键统计值、榜单名次和得分已加入自动断言，点赞计数、评分短评及其他原有验证均符合预期。详细输出见 `test/C_互动与榜单_执行结果_v1.0_20261121.md`。
