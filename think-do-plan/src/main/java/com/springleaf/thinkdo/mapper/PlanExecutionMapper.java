package com.springleaf.thinkdo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.springleaf.thinkdo.domain.entity.PlanExecutionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 每日清单Mapper
 */
@Mapper
public interface PlanExecutionMapper extends BaseMapper<PlanExecutionEntity> {
}
