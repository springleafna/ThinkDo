package com.springleaf.thinkdo.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 通用分页响应
 */
@Data
public class PageResp<T> {

    private List<T> records;
    private Long total;
    private Integer pageNum;
    private Integer pageSize;
    private Integer pages;

    /**
     * 从 MyBatis-Plus IPage 和转换函数构建 PageResp
     */
    public static <E, T> PageResp<T> of(IPage<E> page, java.util.function.Function<E, T> converter) {
        PageResp<T> resp = new PageResp<>();
        resp.setRecords(page.getRecords().stream().map(converter).collect(Collectors.toList()));
        resp.setTotal(page.getTotal());
        resp.setPageNum((int) page.getCurrent());
        resp.setPageSize((int) page.getSize());
        resp.setPages((int) page.getPages());
        return resp;
    }

    /**
     * 从记录列表和总数构建 PageResp
     */
    public static <T> PageResp<T> of(List<T> records, Long total, Integer pageNum, Integer pageSize) {
        PageResp<T> resp = new PageResp<>();
        resp.setRecords(records);
        resp.setTotal(total);
        resp.setPageNum(pageNum);
        resp.setPageSize(pageSize);
        resp.setPages((int) Math.ceil((double) total / pageSize));
        return resp;
    }
}
