-- 智能配送调度系统 建表语句
-- 适用于苍穹外卖 sky-take-out 项目

CREATE TABLE rider (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(32) COMMENT '姓名',
    phone VARCHAR(16) COMMENT '手机号',
    status INT DEFAULT 0 COMMENT '0离线 1空闲 2配送中',
    longitude DOUBLE COMMENT '当前经度',
    latitude DOUBLE COMMENT '当前纬度',
    create_time DATETIME,
    update_time DATETIME
);

CREATE TABLE delivery (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT COMMENT '关联订单',
    rider_id BIGINT COMMENT '骑手',
    status INT DEFAULT 0 COMMENT '0待分配 1已分配 2取餐中 3配送中 4已完成 5异常',
    assign_time DATETIME COMMENT '分配时间',
    pickup_time DATETIME COMMENT '取餐时间',
    finish_time DATETIME COMMENT '完成时间',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME,
    update_time DATETIME
);
