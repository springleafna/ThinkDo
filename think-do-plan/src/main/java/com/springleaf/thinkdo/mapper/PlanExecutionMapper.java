package com.springleaf.thinkdo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.springleaf.thinkdo.domain.entity.PlanExecutionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

/**
 * 每日清单Mapper
 */
@Mapper
public interface PlanExecutionMapper extends BaseMapper<PlanExecutionEntity> {

    /**
     * 获取到deleted=0以及deleted=1的数据
     */
    @Select("SELECT * FROM tb_plan_execution WHERE plan_id = #{planId} AND execute_date = #{executeDate} LIMIT 1")
    PlanExecutionEntity selectIgnoreLogicDelete(@Param("planId") Long planId, @Param("executeDate") LocalDate executeDate);
}
