CREATE TABLE tb_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(255) NOT NULL UNIQUE COMMENT '用户名，唯一',
    password VARCHAR(255) NOT NULL COMMENT '加密后的密码',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE tb_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '角色ID',
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '角色名称，如 USER、ADMIN',
    description VARCHAR(255) DEFAULT NULL COMMENT '角色描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE tb_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '关联创建时间',
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

CREATE TABLE tb_notification (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id          BIGINT NOT NULL COMMENT '接收消息的用户ID',

    -- 核心分类字段
    biz_type         TINYINT NOT NULL COMMENT '业务类型：1-计划(Plan) 2-知识库(Knowledge)',
    event_type       TINYINT NOT NULL COMMENT '事件类型：1-处理完成，10-1天提醒，20-3天提醒，30-已逾期',

    -- 关联业务ID，方便前端点击跳转
    biz_id           BIGINT NOT NULL COMMENT '关联的业务表ID（如 plan_id 或 file_id）',

    -- 消息内容
    title            VARCHAR(255) NOT NULL COMMENT '消息标题',
    content          TEXT NULL COMMENT '消息内容',
    extra_data       JSON NULL COMMENT '扩展数据（存储JSON格式，如跳转路由、附加参数等）',

    -- 状态管理
    read_status      TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读 1-已读',
    read_at          DATETIME NULL COMMENT '读取时间',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息通知表';