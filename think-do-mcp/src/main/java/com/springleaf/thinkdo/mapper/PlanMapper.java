package com.springleaf.thinkdo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.springleaf.thinkdo.entity.Plan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PlanMapper extends BaseMapper<Plan> {

    /**
     * 查询指定用户在指定时间范围内即将截止的计划（未完成）
     */
    @Select("SELECT * FROM tb_plan " +
            "WHERE user_id = #{userId} " +
            "AND status = 0 " +
            "AND deleted = 0 " +
            "AND due_time IS NOT NULL " +
            "AND due_time BETWEEN #{from} AND #{to} " +
            "ORDER BY due_time ASC")
    List<Plan> selectDueSoonPlans(@Param("userId") Long userId,
                                  @Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to);
}
