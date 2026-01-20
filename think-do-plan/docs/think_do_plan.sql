-- 便签表
CREATE TABLE tb_memo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '便签ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    title VARCHAR(100) COMMENT '便签标题',
    content TEXT COMMENT '便签内容',
    tag VARCHAR(20) COMMENT '便签标签',
    background_color ENUM(
        '#fef9e3',  -- 黄色
        '#f4f8fe',  -- 蓝色
        '#f9f3ff',  -- 紫色
        '#fff1f2',  -- 粉色
        '#e8fcf2'   -- 绿色
        ) DEFAULT '#fef9e3' COMMENT '背景颜色',
    pinned TINYINT(1) DEFAULT 0 COMMENT '是否置顶(0:否 1:是)',
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) DEFAULT 0 COMMENT '删除标记(0:正常 1:删除)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='便签表';

# 暂时不设置完成模式的设置
# completion_mode  TINYINT NOT NULL DEFAULT 1 COMMENT '完成模式：1-手动 2-子计划驱动 3-执行驱动',
# 暂时不考虑排序问题
# sort_order   INT NOT NULL DEFAULT 0 COMMENT '排序值（越小越靠前，用于拖拽排序）',

CREATE TABLE tb_plan (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id          BIGINT NOT NULL COMMENT '所属用户',
    category_id      BIGINT NULL COMMENT '分类ID',
    title            VARCHAR(255) NOT NULL COMMENT '计划标题',
    type             TINYINT NOT NULL DEFAULT 0 COMMENT '计划类型：0.普通计划，1.四象限计划，2.每日计划',
    description      TEXT NULL COMMENT '计划描述',
    priority         TINYINT NOT NULL DEFAULT 2 COMMENT '计划优先级：1-低 2-中 3-高',
    quadrant         TINYINT NOT NULL DEFAULT 0 COMMENT '四象限状态：0-无, 1-重要且紧急, 2-重要不紧急, 3-紧急不重要, 4-不重要不紧急',
    tags             VARCHAR(255) NULL COMMENT '计划标签（逗号分隔）',
    start_time       DATETIME NULL COMMENT '开始时间',
    due_time         DATETIME NULL COMMENT '截止时间',
    repeat_type      TINYINT NOT NULL DEFAULT 0 COMMENT '重复类型：0-不重复, 1-每天, 2-每周, 3-每月, 4-每年, 5-工作日',
    repeat_conf      VARCHAR(255) NULL COMMENT '重复配置细节(JSON格式)',
    repeat_until     DATE NULL COMMENT '重复截止日期(空代表无限重复)',
    status           TINYINT NOT NULL DEFAULT 0 COMMENT '计划状态：0-未完成 1-已完成',
    completed_at     DATETIME NULL COMMENT '计划完成时间',
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT(1) DEFAULT 0 COMMENT '删除标记(0:正常 1:删除)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计划表';

CREATE TABLE tb_plan_category (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id      BIGINT NOT NULL COMMENT '所属用户',
    name         VARCHAR(50) NOT NULL COMMENT '分类名称',
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT(1) DEFAULT 0 COMMENT '删除标记(0:正常 1:删除)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计划分类表（长期计划）';

CREATE TABLE tb_plan_step (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id      BIGINT NOT NULL COMMENT '关联的父计划ID',
    title        VARCHAR(255) NOT NULL COMMENT '步骤标题',
    status       TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-未完成 1-已完成',
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT(1) DEFAULT 0 COMMENT '删除标记(0:正常 1:删除)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计划步骤表';

CREATE TABLE tb_plan_execution (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id         BIGINT NOT NULL COMMENT '计划ID',
    execute_date    DATE NOT NULL COMMENT '执行日期',
    status          TINYINT NOT NULL DEFAULT 0 COMMENT '执行状态：0-未完成 1-已完成',
    completed_at    DATETIME NULL COMMENT '当天完成时间',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT(1) DEFAULT 0 COMMENT '删除标记(0:正常 1:删除)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日清单表';

