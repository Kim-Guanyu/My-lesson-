-- =====================================================================
-- 知堂 my-lesson · ml_cms（Content Management System · 课程/章节/课时）
--
-- 来源：从 MySQL 8.4.0 实例反向导出（SHOW CREATE TABLE），已去除
--       环境相关的 AUTO_INCREMENT 计数值。
-- 依赖：MySQL 8.0+（使用 utf8mb4_0900_ai_ci 排序规则）
-- 执行：mysql -u root -p < ml_cms.sql
--
-- 三级课程模型：course（课程）→ season（季次/章节）→ episode（集次/课时）
-- 本脚本只创建库表，不含 DROP，也不含初始数据；重复执行安全。
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `ml_cms`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `ml_cms`;

-- ---------------------------------------------------------------------
-- 课程类别表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` varchar(128) NOT NULL DEFAULT '' COMMENT '标题',
  `idx` bigint NOT NULL DEFAULT '0' COMMENT '序号',
  `info` varchar(512) NOT NULL DEFAULT '' COMMENT '描述',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '数据版本',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '0未删除，1已删除',
  `created` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课程类别表';

-- ---------------------------------------------------------------------
-- 课程表（author 字段同时用于 Elasticsearch 讲师检索）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `course` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` varchar(128) NOT NULL DEFAULT '' COMMENT '标题',
  `author` varchar(128) NOT NULL DEFAULT '' COMMENT '作者',
  `fk_category_id` bigint DEFAULT NULL COMMENT '类别ID，类别表外键',
  `summary` varchar(256) NOT NULL DEFAULT '' COMMENT '摘要图地址',
  `cover` varchar(256) NOT NULL DEFAULT '' COMMENT '封面图地址',
  `price` decimal(8,2) NOT NULL DEFAULT '0.00' COMMENT '单价，单位元',
  `idx` bigint NOT NULL DEFAULT '0' COMMENT '序号',
  `info` varchar(512) NOT NULL DEFAULT '' COMMENT '描述',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '数据版本',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '0未删除，1已删除',
  `created` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课程表';

-- ---------------------------------------------------------------------
-- 季次表（章节）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `season` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` varchar(128) NOT NULL DEFAULT '' COMMENT '标题',
  `info` varchar(512) NOT NULL DEFAULT '' COMMENT '描述',
  `fk_course_id` bigint DEFAULT NULL COMMENT '课程表ID，课程表外键',
  `idx` bigint NOT NULL DEFAULT '0' COMMENT '序号',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '数据版本',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '0未删除，1已删除',
  `created` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='季次表';

-- ---------------------------------------------------------------------
-- 集次表（课时，video 指向 MinIO 中的视频对象）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `episode` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` varchar(128) NOT NULL DEFAULT '' COMMENT '标题',
  `info` varchar(512) NOT NULL DEFAULT '' COMMENT '描述',
  `video` varchar(256) NOT NULL DEFAULT '' COMMENT '视频媒体地址',
  `cover` varchar(256) NOT NULL DEFAULT '' COMMENT '视频封面地址',
  `fk_season_id` bigint DEFAULT NULL COMMENT '季次表ID，季次表外键',
  `idx` bigint NOT NULL DEFAULT '0' COMMENT '序号',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '数据版本',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '0未删除，1已删除',
  `created` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='集次表';

-- ---------------------------------------------------------------------
-- 评论表（pid 自关联，0 为根评论；昵称/头像/省份为冗余快照）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `comment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `fk_episode_id` bigint DEFAULT NULL COMMENT '集次ID，集次表外键',
  `fk_user_id` bigint DEFAULT NULL COMMENT '评论人ID，用户表外键',
  `nickname` varchar(128) NOT NULL DEFAULT '' COMMENT '评论人昵称，冗余',
  `avatar` varchar(128) NOT NULL DEFAULT '' COMMENT '评论人头像，冗余',
  `province` varchar(128) NOT NULL DEFAULT '' COMMENT '评论人省份，冗余',
  `pid` bigint NOT NULL DEFAULT '0' COMMENT '父评论主键，0视为根节点',
  `content` varchar(512) NOT NULL DEFAULT '' COMMENT '评论内容',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '数据版本',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '0未删除，1已删除',
  `created` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评论表';

-- ---------------------------------------------------------------------
-- 收藏表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `follow` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `fk_episode_id` bigint DEFAULT NULL COMMENT '集次ID，集次表外键',
  `fk_user_id` bigint DEFAULT NULL COMMENT '收藏人ID，用户表外键',
  `nickname` varchar(128) NOT NULL DEFAULT '' COMMENT '收藏人昵称，冗余',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '数据版本',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '0未删除，1已删除',
  `created` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='收藏表';

-- ---------------------------------------------------------------------
-- 举报表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `fk_episode_id` bigint DEFAULT NULL COMMENT '集次ID，集次表外键',
  `fk_user_id` bigint DEFAULT NULL COMMENT '举报人ID，用户表外键',
  `nickname` varchar(128) NOT NULL DEFAULT '' COMMENT '举报人昵称，冗余',
  `content` varchar(512) NOT NULL DEFAULT '' COMMENT '举报内容',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '数据版本',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '0未删除，1已删除',
  `created` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='举报表';
