package com.springleaf.thinkdo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.springleaf.thinkdo.domain.entity.PlanStepEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 计划步骤Mapper
 */
@Mapper
public interface PlanStepMapper extends BaseMapper<PlanStepEntity> {
}
