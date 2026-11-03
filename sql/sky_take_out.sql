-- 苍穹外卖：截图中的 11 张业务表。
-- 字段参考课程 SQL 镜像，统一使用 utf8mb4；仅建库建表，不删除或覆盖已有数据。
-- Reference: https://github.com/Danyhug/heima_sky_take_out/blob/main/sky_take_out.sql
CREATE DATABASE IF NOT EXISTS `sky_take_out`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;
USE `sky_take_out`;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `employee` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(32) NOT NULL COMMENT '姓名',
  `username` VARCHAR(32) NOT NULL COMMENT '用户名',
  `password` VARCHAR(64) NOT NULL COMMENT '密码',
  `phone` VARCHAR(11) NOT NULL COMMENT '手机号',
  `sex` VARCHAR(2) NOT NULL COMMENT '性别',
  `id_number` VARCHAR(18) NOT NULL COMMENT '身份证号',
  `status` INT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `create_user` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_user` BIGINT DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='员工表';

CREATE TABLE IF NOT EXISTS `category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type` INT DEFAULT NULL COMMENT '类型：1菜品分类，2套餐分类',
  `name` VARCHAR(32) NOT NULL COMMENT '分类名称',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `status` INT DEFAULT NULL COMMENT '状态：0禁用，1启用',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `create_user` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_user` BIGINT DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_category_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='分类表';

CREATE TABLE IF NOT EXISTS `dish` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(32) NOT NULL COMMENT '菜品名称',
  `category_id` BIGINT NOT NULL COMMENT '菜品分类ID',
  `price` DECIMAL(10,2) DEFAULT NULL COMMENT '菜品价格',
  `image` VARCHAR(255) DEFAULT NULL COMMENT '图片',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
  `status` INT DEFAULT 1 COMMENT '状态：0停售，1起售',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `create_user` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_user` BIGINT DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_dish_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='菜品表';

CREATE TABLE IF NOT EXISTS `dish_flavor` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dish_id` BIGINT NOT NULL COMMENT '菜品ID',
  `name` VARCHAR(32) DEFAULT NULL COMMENT '口味名称',
  `value` VARCHAR(255) DEFAULT NULL COMMENT '口味选项JSON数组',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='菜品口味表';

CREATE TABLE IF NOT EXISTS `setmeal` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `category_id` BIGINT NOT NULL COMMENT '套餐分类ID',
  `name` VARCHAR(32) NOT NULL COMMENT '套餐名称',
  `price` DECIMAL(10,2) NOT NULL COMMENT '套餐价格',
  `status` INT DEFAULT 1 COMMENT '状态：0停售，1起售',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
  `image` VARCHAR(255) DEFAULT NULL COMMENT '图片',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `create_user` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_user` BIGINT DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_setmeal_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='套餐表';

CREATE TABLE IF NOT EXISTS `setmeal_dish` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `setmeal_id` BIGINT DEFAULT NULL COMMENT '套餐ID',
  `dish_id` BIGINT DEFAULT NULL COMMENT '菜品ID',
  `name` VARCHAR(32) DEFAULT NULL COMMENT '菜品名称冗余字段',
  `price` DECIMAL(10,2) DEFAULT NULL COMMENT '菜品单价冗余字段',
  `copies` INT DEFAULT NULL COMMENT '菜品份数',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='套餐菜品关系表';

CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `openid` VARCHAR(45) DEFAULT NULL COMMENT '微信用户唯一标识',
  `name` VARCHAR(32) DEFAULT NULL COMMENT '姓名',
  `phone` VARCHAR(11) DEFAULT NULL COMMENT '手机号',
  `sex` VARCHAR(2) DEFAULT NULL COMMENT '性别',
  `id_number` VARCHAR(18) DEFAULT NULL COMMENT '身份证号',
  `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `address_book` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `consignee` VARCHAR(50) DEFAULT NULL COMMENT '收货人',
  `sex` VARCHAR(2) DEFAULT NULL COMMENT '性别',
  `phone` VARCHAR(11) NOT NULL COMMENT '手机号',
  `province_code` VARCHAR(12) DEFAULT NULL COMMENT '省级区划编号',
  `province_name` VARCHAR(32) DEFAULT NULL COMMENT '省级名称',
  `city_code` VARCHAR(12) DEFAULT NULL COMMENT '市级区划编号',
  `city_name` VARCHAR(32) DEFAULT NULL COMMENT '市级名称',
  `district_code` VARCHAR(12) DEFAULT NULL COMMENT '区级区划编号',
  `district_name` VARCHAR(32) DEFAULT NULL COMMENT '区级名称',
  `detail` VARCHAR(200) DEFAULT NULL COMMENT '详细地址',
  `label` VARCHAR(100) DEFAULT NULL COMMENT '标签',
  `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认：0否，1是',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='地址表';

CREATE TABLE IF NOT EXISTS `shopping_cart` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(32) DEFAULT NULL COMMENT '商品名称',
  `image` VARCHAR(255) DEFAULT NULL COMMENT '图片',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `dish_id` BIGINT DEFAULT NULL COMMENT '菜品ID',
  `setmeal_id` BIGINT DEFAULT NULL COMMENT '套餐ID',
  `dish_flavor` VARCHAR(50) DEFAULT NULL COMMENT '菜品口味',
  `number` INT NOT NULL DEFAULT 1 COMMENT '数量',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '金额',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='购物车表';

CREATE TABLE IF NOT EXISTS `orders` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `number` VARCHAR(50) DEFAULT NULL COMMENT '订单号',
  `status` INT NOT NULL DEFAULT 1 COMMENT '订单状态：1待付款，2待接单，3已接单，4派送中，5已完成，6已取消，7退款',
  `user_id` BIGINT NOT NULL COMMENT '下单用户ID',
  `address_book_id` BIGINT NOT NULL COMMENT '地址ID',
  `order_time` DATETIME NOT NULL COMMENT '下单时间',
  `checkout_time` DATETIME DEFAULT NULL COMMENT '结账时间',
  `pay_method` INT NOT NULL DEFAULT 1 COMMENT '支付方式：1微信，2支付宝',
  `pay_status` TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态：0未支付，1已支付，2退款',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '实收金额',
  `remark` VARCHAR(100) DEFAULT NULL COMMENT '备注',
  `phone` VARCHAR(11) DEFAULT NULL COMMENT '手机号',
  `address` VARCHAR(255) DEFAULT NULL COMMENT '地址',
  `user_name` VARCHAR(32) DEFAULT NULL COMMENT '用户名称',
  `consignee` VARCHAR(32) DEFAULT NULL COMMENT '收货人',
  `cancel_reason` VARCHAR(255) DEFAULT NULL COMMENT '订单取消原因',
  `rejection_reason` VARCHAR(255) DEFAULT NULL COMMENT '订单拒绝原因',
  `cancel_time` DATETIME DEFAULT NULL COMMENT '订单取消时间',
  `estimated_delivery_time` DATETIME DEFAULT NULL COMMENT '预计送达时间',
  `delivery_status` TINYINT NOT NULL DEFAULT 1 COMMENT '配送状态：1立即送出，0选择具体时间',
  `delivery_time` DATETIME DEFAULT NULL COMMENT '送达时间',
  `pack_amount` INT DEFAULT NULL COMMENT '打包费',
  `tableware_number` INT DEFAULT NULL COMMENT '餐具数量',
  `tableware_status` TINYINT NOT NULL DEFAULT 1 COMMENT '餐具状态：1按餐量提供，0选择具体数量',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='订单表';

CREATE TABLE IF NOT EXISTS `order_detail` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(32) DEFAULT NULL COMMENT '商品名称',
  `image` VARCHAR(255) DEFAULT NULL COMMENT '图片',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `dish_id` BIGINT DEFAULT NULL COMMENT '菜品ID',
  `setmeal_id` BIGINT DEFAULT NULL COMMENT '套餐ID',
  `dish_flavor` VARCHAR(50) DEFAULT NULL COMMENT '菜品口味',
  `number` INT NOT NULL DEFAULT 1 COMMENT '数量',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '金额',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='订单明细表';

-- 执行后核对表名及字段数，已有同名表会保留原结构。
SELECT t.TABLE_NAME AS table_name, t.TABLE_COMMENT AS description,
       COUNT(c.COLUMN_NAME) AS column_count
FROM information_schema.TABLES t
JOIN information_schema.COLUMNS c
  ON c.TABLE_SCHEMA = t.TABLE_SCHEMA AND c.TABLE_NAME = t.TABLE_NAME
WHERE t.TABLE_SCHEMA = 'sky_take_out'
  AND t.TABLE_NAME IN ('employee','category','dish','dish_flavor','setmeal',
                      'setmeal_dish','user','address_book','shopping_cart','orders','order_detail')
GROUP BY t.TABLE_NAME, t.TABLE_COMMENT
ORDER BY t.TABLE_NAME;
