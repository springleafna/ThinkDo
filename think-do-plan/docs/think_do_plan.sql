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
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) DEFAULT 0 COMMENT '删除标记(0:正常 1:删除)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='便签表';