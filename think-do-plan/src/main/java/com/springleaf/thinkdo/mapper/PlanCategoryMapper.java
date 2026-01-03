package com.springleaf.thinkdo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.springleaf.thinkdo.domain.entity.PlanCategoryEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 计划分类Mapper
 */
@Mapper
public interface PlanCategoryMapper extends BaseMapper<PlanCategoryEntity> {
}
