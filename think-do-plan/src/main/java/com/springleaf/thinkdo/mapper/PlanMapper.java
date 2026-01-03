package com.springleaf.thinkdo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.springleaf.thinkdo.domain.entity.PlanEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 计划Mapper
 */
@Mapper
public interface PlanMapper extends BaseMapper<PlanEntity> {
}
