# 数据库脚本

知堂 my-lesson 按业务域拆分为四个 MySQL 库，本目录存放建库建表脚本与增量变更脚本。

## 目录内容

| 文件 | 说明 | 表数 |
|------|------|------|
| `ml_ums.sql` | 用户管理 User Management System | 5 |
| `ml_cms.sql` | 内容管理 Content Management System | 7 |
| `ml_oms.sql` | 订单管理 Order Management System | 3 |
| `ml_sms.sql` | 营销管理 Sales Management System | 6 |
| `20260723_order_sn_unique.sql` | 增量变更：`order.sn` 唯一索引（**已合入 `ml_oms.sql`**，仅存量库需要执行） | — |

## 初始化

四个库之间没有物理外键，执行顺序任意：

```bash
mysql -u root -p < ml_ums.sql
mysql -u root -p < ml_cms.sql
mysql -u root -p < ml_oms.sql
mysql -u root -p < ml_sms.sql
```

脚本使用 `CREATE DATABASE IF NOT EXISTS` / `CREATE TABLE IF NOT EXISTS`，**不含任何 `DROP`**，
重复执行不会破坏已有数据。反过来说，若线上表结构已经和脚本不一致，重跑脚本也**不会**把它改回来，
这类情况请写增量脚本。

要求 MySQL 8.0+：脚本使用 `utf8mb4_0900_ai_ci` 排序规则（导出源实例为 MySQL 8.4.0）。

## 脚本来源与约定

建表语句由线上实例 `SHOW CREATE TABLE` 反向导出，仅做了两处加工：

1. 去掉环境相关的 `AUTO_INCREMENT=n` 计数值；
2. 加上分节注释，并把 `DROP` 语义换成 `IF NOT EXISTS`。

字段层面的通用约定：

- **主键**：统一 `id bigint AUTO_INCREMENT`。
- **逻辑外键**：`fk_<表名>_id`，**均未建物理外键约束**，关联关系由应用层维护（跨库关联如
  `order.fk_user_id → ml_ums.user.id` 本身也无法建物理 FK）。
- **公共字段**：每张表都有 `version`（乐观锁数据版本）、`deleted`（0 未删除 / 1 已删除，逻辑删除）、
  `created` / `updated`。注意 `updated` 只有 `DEFAULT CURRENT_TIMESTAMP`，**没有
  `ON UPDATE CURRENT_TIMESTAMP`**，更新时间由 MyBatis-Flex 的字段填充负责写入。
- **树形结构**：`menu.pid`、`comment.pid` 自关联，`0` 表示根节点。
- **冗余快照**：`order_detail` / `cart` / `seckill_detail` 冗余了课程标题、封面、单价，
  `comment` / `follow` / `report` 冗余了用户昵称等，目的是跨库查询解耦 + 保留下单时点快照。
- **金额**：统一 `decimal(8,2)`，单位元。唯一的例外是 `coupons.cp_price` 的列注释写作
  「单位分」，与实际类型不符，保持与线上库一致未作改动。

索引方面，除主键外线上仅有 `ml_oms.order.uk_order_sn` 一个唯一索引，脚本与之保持一致，
未额外添加推测性索引。

## 关于初始数据

本目录**不含 `INSERT` 初始数据**。运营后台的 RBAC 需要 `menu` / `role` / `role_menu` /
`user_role` 有数据才能正常登录并渲染动态菜单，请自行准备种子数据，或从已有环境按需导出
（注意 `ml_ums.user` 含手机号、身份证号等个人信息字段，导出前请脱敏，不要提交进仓库）。

## 变更流程

修改表结构时，请在本目录新增 `YYYYMMDD_<变更说明>.sql` 增量脚本（参考
`20260723_order_sn_unique.sql` 的写法：先给出校验查询，再给出 DDL），
同时把变更同步回对应的 `ml_*.sql` 建表脚本，保证新库一次建到位。
