-- =====================================================================
-- 知堂 my-lesson · ml_ums（User Management System · 用户/角色/菜单）
--
-- 来源：从 MySQL 8.4.0 实例反向导出（SHOW CREATE TABLE），已去除
--       环境相关的 AUTO_INCREMENT 计数值。
-- 依赖：MySQL 8.0+（使用 utf8mb4_0900_ai_ci 排序规则）
-- 执行：mysql -u root -p < ml_ums.sql
--
-- 本脚本只创建库表，不含 DROP，也不含初始数据；
-- 重复执行安全（IF NOT EXISTS）。
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `ml_ums`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `ml_ums`;

-- ---------------------------------------------------------------------
-- 用户表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(128) NOT NULL DEFAULT '' COMMENT '账号',
  `password` varchar(128) NOT NULL DEFAULT '' COMMENT '密码',
  `nickname` varchar(128) NOT NULL DEFAULT '' COMMENT '昵称',
  `email` varchar(128) NOT NULL DEFAULT '' COMMENT '邮箱',
  `province` varchar(128) NOT NULL DEFAULT '' COMMENT '省份',
  `realname` varchar(128) NOT NULL DEFAULT '' COMMENT '姓名',
  `avatar` varchar(256) NOT NULL DEFAULT '' COMMENT '头像',
  `zodiac` char(3) NOT NULL DEFAULT '' COMMENT '星座',
  `phone` char(11) NOT NULL DEFAULT '' COMMENT '手机',
  `idcard` char(18) NOT NULL DEFAULT '' COMMENT '身份证号',
  `gender` tinyint NOT NULL DEFAULT '0' COMMENT '性别，0女，1男，2保密',
  `age` tinyint NOT NULL DEFAULT '0' COMMENT '年龄',
  `info` varchar(512) NOT NULL DEFAULT '' COMMENT '描述',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '数据版本',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '0未删除，1已删除',
  `created` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

-- ---------------------------------------------------------------------
-- 角色表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` varchar(128) NOT NULL DEFAULT '' COMMENT '标题',
  `idx` bigint NOT NULL DEFAULT '0' COMMENT '序号',
  `info` varchar(512) NOT NULL DEFAULT '' COMMENT '描述',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '数据版本',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '0未删除，1已删除',
  `created` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';

-- ---------------------------------------------------------------------
-- 菜单表（pid 自关联，0 为根节点；按钮级权限也落在此表）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` varchar(128) NOT NULL DEFAULT '' COMMENT '标题',
  `url` varchar(256) NOT NULL DEFAULT '' COMMENT '跳转地址',
  `icon` varchar(256) NOT NULL DEFAULT '' COMMENT '图标名称',
  `pid` bigint NOT NULL DEFAULT '0' COMMENT '父菜单主键，0视为根节点',
  `idx` bigint NOT NULL DEFAULT '0' COMMENT '序号',
  `info` varchar(512) NOT NULL DEFAULT '' COMMENT '描述',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '数据版本',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '0未删除，1已删除',
  `created` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜单表';

-- ---------------------------------------------------------------------
-- 用户角色关系表（逻辑外键，未建物理 FK）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `fk_user_id` bigint DEFAULT NULL COMMENT '用户ID，用户表外键',
  `fk_role_id` bigint DEFAULT NULL COMMENT '角色ID，角色表外键',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '数据版本',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '0未删除，1已删除',
  `created` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关系表';

-- ---------------------------------------------------------------------
-- 角色菜单关系表（逻辑外键，未建物理 FK）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `role_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `fk_role_id` bigint DEFAULT NULL COMMENT '角色ID，角色表外键',
  `fk_menu_id` bigint DEFAULT NULL COMMENT '菜单ID，菜单表外键',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '数据版本',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '0未删除，1已删除',
  `created` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色菜单关系表';
