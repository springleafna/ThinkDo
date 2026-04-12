package com.springleaf.thinkdo.domain.request;

import com.springleaf.thinkdo.common.PageReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理员-便签查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdminMemoQueryReq extends PageReq {

    private Long userId;
    private String username;
    private String keyword;
    private String tag;
    private Integer pinned;
}
