-- =====================================================================
-- 知堂 my-lesson · ml_oms（Order Management System · 购物车/订单/支付）
--
-- 来源：从 MySQL 8.4.0 实例反向导出（SHOW CREATE TABLE），已去除
--       环境相关的 AUTO_INCREMENT 计数值。
-- 依赖：MySQL 8.0+（使用 utf8mb4_0900_ai_ci 排序规则）
-- 执行：mysql -u root -p < ml_oms.sql
--
-- 注意：`order` 上的 uk_order_sn 唯一索引是秒杀链路 MQ 重投的最终幂等
--       保障，已直接包含在下面的建表语句中（对应增量脚本
--       20260723_order_sn_unique.sql，新库无需再单独执行）。
-- 本脚本只创建库表，不含 DROP，也不含初始数据；重复执行安全。
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `ml_oms`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `ml_oms`;

-- ---------------------------------------------------------------------
-- 订单表（order 是 MySQL 关键字，读写时必须用反引号）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `sn` varchar(128) NOT NULL DEFAULT '' COMMENT '编号',
  `total_amount` decimal(8,2) NOT NULL DEFAULT '0.00' COMMENT '总金额',
  `pay_amount` decimal(8,2) NOT NULL DEFAULT '0.00' COMMENT '实际支付总金额',
  `pay_type` tinyint NOT NULL DEFAULT '0' COMMENT '支付方式，0未支付，1微信，2支付宝，3其他',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '订单状态，0未付款，1已付款，2已取消，3其他',
  `fk_user_id` bigint DEFAULT NULL COMMENT '用户ID，用户表外键',
  `username` varchar(128) NOT NULL DEFAULT '' COMMENT '用户账号（冗余）',
  `fk_coupons_id` bigint DEFAULT NULL COMMENT '优惠卷ID，优惠卷表外键',
  `info` varchar(512) NOT NULL DEFAULT '' COMMENT '描述',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '数据版本',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '0未删除，1已删除',
  `created` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_sn` (`sn`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单表';

-- ---------------------------------------------------------------------
-- 订单明细表（下单时快照课程标题/封面/单价，course 后续改价不影响历史订单）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `order_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `fk_order_id` bigint DEFAULT NULL COMMENT '订单ID，订单表外键',
  `fk_course_id` bigint DEFAULT NULL COMMENT '课程ID，课程表外键',
  `course_title` varchar(128) NOT NULL DEFAULT '' COMMENT '课程标题（冗余）',
  `course_cover` varchar(256) NOT NULL DEFAULT '' COMMENT '课程封面图（冗余）',
  `course_price` decimal(8,2) NOT NULL DEFAULT '0.00' COMMENT '课程单价，单位元（冗余）',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '数据版本',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '0未删除，1已删除',
  `created` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单明细表';

-- ---------------------------------------------------------------------
-- 购物车表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `cart` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `fk_user_id` bigint DEFAULT NULL COMMENT '用户ID，用户表外键',
  `username` varchar(128) NOT NULL DEFAULT '' COMMENT '用户账号（冗余）',
  `fk_course_id` bigint DEFAULT NULL COMMENT '课程ID，课程表外键',
  `course_title` varchar(128) NOT NULL DEFAULT '' COMMENT '课程标题（冗余）',
  `course_cover` varchar(256) NOT NULL DEFAULT '' COMMENT '课程封面图（冗余）',
  `course_price` decimal(8,2) NOT NULL DEFAULT '0.00' COMMENT '课程单价，单位元（冗余）',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '数据版本',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '0未删除，1已删除',
  `created` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='购物车表';
