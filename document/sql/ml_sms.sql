-- =====================================================================
-- 知堂 my-lesson · ml_sms（Sales Management System · 营销/秒杀/内容位）
--
-- 来源：从 MySQL 8.4.0 实例反向导出（SHOW CREATE TABLE），已去除
--       环境相关的 AUTO_INCREMENT 计数值。
-- 依赖：MySQL 8.0+（使用 utf8mb4_0900_ai_ci 排序规则）
-- 执行：mysql -u root -p < ml_sms.sql
--
-- 本脚本只创建库表，不含 DROP，也不含初始数据；重复执行安全。
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `ml_sms`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `ml_sms`;

-- ---------------------------------------------------------------------
-- 横幅表（首页轮播图）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `banner` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `url` varchar(256) NOT NULL DEFAULT '' COMMENT '图片地址',
  `info` varchar(512) NOT NULL DEFAULT '' COMMENT '描述',
  `idx` bigint NOT NULL DEFAULT '0' COMMENT '序号',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '数据版本',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '0未删除，1已删除',
  `created` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='横幅表';

-- ---------------------------------------------------------------------
-- 通知表（站内公告）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `notice` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `content` varchar(512) NOT NULL DEFAULT '' COMMENT '内容',
  `idx` bigint NOT NULL DEFAULT '0' COMMENT '序号',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '数据版本',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '0未删除，1已删除',
  `created` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知表';

-- ---------------------------------------------------------------------
-- 新闻表（资讯文章）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `article` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` varchar(128) NOT NULL DEFAULT '' COMMENT '标题',
  `content` varchar(512) NOT NULL DEFAULT '' COMMENT '内容',
  `idx` bigint NOT NULL DEFAULT '0' COMMENT '序号',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '数据版本',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '0未删除，1已删除',
  `created` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='新闻表';

-- ---------------------------------------------------------------------
-- 优惠卷表
-- 注意：cp_price 的列注释写的是「单位分」，但列类型为 decimal(8,2)，
--       与 course.price / order.total_amount 一致，实际按「元」存储。
--       此处保持与线上库完全一致，未改动注释。
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `coupons` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` varchar(128) NOT NULL DEFAULT '' COMMENT '兑换码',
  `title` varchar(128) NOT NULL DEFAULT '' COMMENT '标题',
  `cp_price` decimal(8,2) NOT NULL DEFAULT '0.00' COMMENT '优惠金额，单位分',
  `info` varchar(512) NOT NULL DEFAULT '' COMMENT '描述',
  `start_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '优惠卷生效时间',
  `end_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '优惠卷失效时间',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '数据版本',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '0未删除，1已删除',
  `created` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='优惠卷表';

-- ---------------------------------------------------------------------
-- 秒杀表（活动主表，status 由 XXL-JOB 定时开关驱动）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `seckill` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` varchar(128) NOT NULL DEFAULT '' COMMENT '标题',
  `info` varchar(512) NOT NULL DEFAULT '' COMMENT '描述',
  `start_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '活动开始时间',
  `end_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '活动结束时间',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0未开始，1已开始，2已结束',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '数据版本',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '0未删除，1已删除',
  `created` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='秒杀表';

-- ---------------------------------------------------------------------
-- 秒杀明细表（sk_count 为活动库存基数，活动预热时写入 Redis 由 Lua 扣减）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `seckill_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `fk_seckill_id` bigint DEFAULT NULL COMMENT '秒杀ID，秒杀表外键',
  `fk_course_id` bigint DEFAULT NULL COMMENT '课程ID，课程表外键',
  `course_title` varchar(128) NOT NULL DEFAULT '' COMMENT '课程标题（冗余）',
  `course_cover` varchar(256) NOT NULL DEFAULT '' COMMENT '课程封面图（冗余）',
  `course_price` decimal(8,2) NOT NULL DEFAULT '0.00' COMMENT '课程单价，单位元（冗余）',
  `sk_price` decimal(8,2) NOT NULL DEFAULT '0.00' COMMENT '秒杀价格，单位元',
  `sk_count` int NOT NULL DEFAULT '0' COMMENT '秒杀数量',
  `info` varchar(512) NOT NULL DEFAULT '' COMMENT '描述',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '数据版本',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '0未删除，1已删除',
  `created` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='秒杀明细表';
