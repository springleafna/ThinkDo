package com.springleaf.thinkdo.domain.request;

import com.springleaf.thinkdo.common.PageReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理员-用户查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdminUserQueryReq extends PageReq {

    /**
     * 用户名（模糊搜索）
     */
    private String username;

    /**
     * 角色名称筛选（USER/ADMIN）
     */
    private String role;
}
