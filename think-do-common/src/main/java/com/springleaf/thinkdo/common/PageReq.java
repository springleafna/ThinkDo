package com.springleaf.thinkdo.common;

import lombok.Data;

/**
 * 通用分页请求基类
 */
@Data
public class PageReq {

    /**
     * 当前页码，默认第1页
     */
    private Integer pageNum = 1;

    /**
     * 每页条数，默认10条
     */
    private Integer pageSize = 10;
}
