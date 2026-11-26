# 网络文学创作与阅读平台 · 全局 SQL 部署测试报告

- 测试日期：2026-11-23
- 测试阶段：第一阶段——全局 SQL 部署测试
- 原始脚本：`sql/D_全局建表_schema_all_v1.0_20261123.sql`
- 测试副本：`test/D_全局部署测试_schema_all_v1.0_20261123.sql`
- 结构校验：`test/D_全局部署测试_结构校验_v1.0_20261123.sql`
- MySQL 版本：26.7.0 Community Server（满足 MySQL 8.0.16+）
- 连接地址：`127.0.0.1:33307`
- 执行账号：`novel_test`@`127.0.0.1`
- 目标数据库：`novel_platform_it_20261123_01`
- 独立数据目录：`C:\Users\Wu\AppData\Local\Temp\novel-platform-mysql-33307\data`
- 认证方式：使用此前受 ACL 保护的本地认证文件，未读取或输出明文密码

## 1. 安全前置检查

| 编号 | 前置条件与测试操作 | 预期结果 | 实际结果 | 状态 |
|---|---|---|---|---|
| S01 | 使用 `novel_test` 的本地安全认证文件进行只读连接 | 连接成功，不使用 root | `CURRENT_USER()` 为 `novel_test@127.0.0.1` | 通过 |
| S02 | 查询 `@@port`、`@@datadir`、`DATABASE()` | 端口为 33307，数据目录为独立测试目录，当前库为空 | 端口 33307；目录指向 `novel-platform-mysql-33307\data`；`DATABASE()` 为 `NULL` | 通过 |
| S03 | 执行前查询目标库是否存在 | 目标库不存在；若存在则立即停止 | 查询结果为 0，未删除或覆盖任何数据库 | 通过 |
| S04 | 比较原全局 SQL 与测试副本 | 仅数据库名发生变化 | 原脚本中的 7 处 `novel_platform` 均替换为测试库名；将测试库名反向替换后，全文与原脚本逐字一致 | 通过 |

本阶段未连接或操作现有 3306 实例，未操作正式数据库 `novel_platform`，未修改 A/B/C/D 模块 SQL、原始全局 SQL或数据库设计文件。

## 2. 部署执行记录

### 2.1 客户端调用问题（均未执行全局 SQL）

| 次序 | 问题 | 数据库状态与处理 |
|---|---|---|
| 第 1 次 | MySQL 26.7 客户端返回 `unknown option '--abort-source-on-error'`，退出码 2 | 未进入 SQL 执行；确认目标库仍不存在后移除该客户端选项 |
| 第 2 次 | 通过 `--execute` 传入中文 `source` 路径时发生编码错误，客户端报告 `ERROR 1064 (42000)` | 全局脚本未开始执行；确认目标库仍不存在，改为 UTF-8 标准输入读取测试副本 |

以上为测试执行器的客户端调用问题，不是全局 SQL 中的语句错误。

### 2.2 正式部署执行

| 编号 | 前置条件与测试操作 | 预期结果 | 实际结果 | 状态 |
|---|---|---|---|---|
| D01 | 目标库不存在；通过 UTF-8 标准输入执行完整测试副本，不使用 `--force` | 脚本全部执行成功，退出码 0，无 SQL 执行错误 | 退出码 0；全局 SQL 部署成功；SQL 执行错误 0 | 通过 |

部署输出包含首次执行 `DROP ... IF EXISTS` 时对象不存在的提示，以及 MySQL 26.7 的弃用警告，详见第 5 节。这些输出未导致语句失败。

## 3. 自动结构与对象检查

以下检查均在部署成功后执行。对象数量断言之外，还对实际对象名称与预期清单进行了自动比对。

| 编号 | 前置条件与测试操作 | 预期结果 | 实际结果 | 状态 |
|---|---|---|---|---|
| O01 | 查询 `information_schema.TABLES` | 19 张 BASE TABLE | 19 张，名称全部匹配 | 通过 |
| O02 | 查询 `information_schema.VIEWS` | 5 个视图 | 5 个，名称全部匹配 | 通过 |
| O03 | 查询 `information_schema.ROUTINES` | 15 个 PROCEDURE，0 个 FUNCTION | 15 个存储过程，名称全部匹配；函数 0 个 | 通过 |
| O04 | 查询 `information_schema.TRIGGERS` | 8 个触发器 | 8 个，名称全部匹配 | 通过 |
| O05 | 查询 `REFERENTIAL_CONSTRAINTS` | 30 个外键 | 30 个 | 通过 |
| O06 | 查询 `STATISTICS` 并按表名、索引名去重 | 75 个索引，包括 MySQL 为外键自动补建的索引 | 75 个 | 通过 |
| O07 | 检查所有外键列是否存在首列索引 | 无缺失 | 缺失数量 0 | 通过 |
| O08 | 检查外键删除规则 | `fk_shelf_last_chapter` 为 `SET NULL`，其余为 `RESTRICT` | 规则异常数量 0 | 通过 |
| O09 | 检查外键更新规则 | D 模块管理员外键为 `RESTRICT`，其余为 `CASCADE` | 规则异常数量 0 | 通过 |
| O10 | 检查基表存储引擎 | 全部 InnoDB | 非 InnoDB 基表 0 张 | 通过 |
| O11 | 检查数据库默认字符集和排序规则 | `utf8mb4` / `utf8mb4_0900_ai_ci` | 完全匹配，异常数量 0 | 通过 |
| O12 | 检查所有基表排序规则 | 全部 `utf8mb4_0900_ai_ci` | 异常基表 0 张 | 通过 |
| O13 | 检查 CHAR、VARCHAR、TEXT、ENUM 等文本列排序规则 | 全部 `utf8mb4_0900_ai_ci` | 异常文本列 0 个 | 通过 |

### 3.1 实际对象清单

- **19 张表**：`app_user`、`user_account`、`novel`、`chapter`、`tag`、`novel_tag`、`user_bookshelf`、`chapter_subscription`、`reward_tip`、`account_transaction`、`comment`、`comment_like`、`novel_rating`、`novel_rating_stat`、`ranking_config`、`ranking_item`、`audit_log`、`admin_operation`、`sys_dict`。
- **5 个视图**：`v_user_balance_reconciliation`、`v_comment_visible`、`v_novel_rating_summary`、`v_ranking_item_detail`、`v_ranking_latest`。
- **15 个存储过程**：`sp_subscribe_chapter`、`sp_tip_author`、`sp_create_novel`、`sp_save_chapter_draft`、`sp_publish_chapter`、`sp_offline_chapter`、`sp_apply_comment_reply_delta`、`sp_create_comment`、`sp_edit_comment_content`、`sp_audit_comment`、`sp_set_comment_status`、`sp_sync_comment_reply_count`、`sp_refresh_rating_stat`、`sp_generate_ranking`、`sp_get_ranking`。

- **8 个触发器**：`trg_novel_rating_ai`、`trg_novel_rating_au`、`trg_novel_rating_ad`、`trg_novel_rating_bi`、`trg_novel_rating_bu`、`trg_comment_like_ai`、`trg_comment_like_ad`、`trg_comment_bu`。

### 3.2 索引预期修正记录

首次结构校验将索引总数预估为 72，实际查询为 75，因此自动断言按设计失败并停止。逐表只读核对后确认：

- MySQL 为 `comment.reply_to_user_id` 外键自动创建了 `fk_comment_reply_user` 索引；
- 其余索引均与全局 SQL 定义相符；
- 加上自动外键索引后，正确预期总数为 75。

随后仅将测试校验脚本中的预期值由 72 修正为 75，未修改任何业务 SQL。重新执行全部断言后通过。

## 4. 视图查询验证

在空数据部署环境下，分别对 5 个视图执行 `SELECT COUNT(*)`。返回 0 行记录属于预期，检查重点是视图能否正常解析和执行。

| 编号 | 前置条件与测试操作 | 预期结果 | 实际结果 | 状态 |
|---|---|---|---|---|
| V01 | 查询 `v_user_balance_reconciliation` | 查询成功 | 查询成功，记录数 0 | 通过 |
| V02 | 查询 `v_comment_visible` | 查询成功 | 查询成功，记录数 0 | 通过 |
| V03 | 查询 `v_novel_rating_summary` | 查询成功 | 查询成功，记录数 0 | 通过 |
| V04 | 查询 `v_ranking_item_detail` | 查询成功 | 查询成功，记录数 0 | 通过 |
| V05 | 查询 `v_ranking_latest` | 查询成功 | 查询成功，记录数 0 | 通过 |

## 5. 警告与提示记录

| 类型 | MySQL 代码 | 含义 | 本次处理与结论 |
|---|---|---|---|
| Warning | 1681 | 整数显示宽度在 MySQL 26.7 中已弃用 | 不影响建表及当前版本执行；记录为未来版本兼容性提示，本阶段不修改业务 SQL |
| Warning | 1287 | `BINARY expr` 在 MySQL 26.7 中已弃用，建议未来使用 `CAST` | 不影响本次创建；本阶段不修改业务 SQL |
| Note | 1305 | 首次执行时待删除存储过程不存在 | 全新数据库下的正常提示 |
| Note | 1360 | 首次执行时待删除触发器不存在 | 全新数据库下的正常提示 |

**实际全局 SQL 执行错误：0。**

## 6. 本阶段范围限制与结论

- 已检查 15 个存储过程和 8 个触发器均成功创建，名称及数量正确。
- 按要求，本阶段未调用业务存储过程，未验证触发器的业务触发效果，未写入业务测试数据。
- 未开展用户注册、订阅、打赏、评论、评分、审核或榜单等跨模块业务链路测试，留待第二阶段。
- 最终自动断言结果：`ALL_METADATA_ASSERTIONS=PASS`，5 个视图查询全部成功。

**第一阶段全局 SQL 部署测试结论：通过。**
