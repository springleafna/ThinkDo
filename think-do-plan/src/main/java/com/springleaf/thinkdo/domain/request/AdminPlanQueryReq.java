package com.springleaf.thinkdo.domain.request;

import com.springleaf.thinkdo.common.PageReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理员-计划查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdminPlanQueryReq extends PageReq {

    private Long userId;
    private String username;
    private String keyword;
    private Integer type;
    private Integer priority;
    private Integer status;
    private Integer quadrant;
}
