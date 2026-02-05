package com.springleaf.thinkdo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.springleaf.thinkdo.domain.entity.NoteCategoryEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 笔记分类Mapper
 */
@Mapper
public interface NoteCategoryMapper extends BaseMapper<NoteCategoryEntity> {
}
