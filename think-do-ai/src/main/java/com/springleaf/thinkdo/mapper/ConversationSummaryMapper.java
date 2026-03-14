package com.springleaf.thinkdo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.springleaf.thinkdo.domain.entity.ConversationSummaryEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话摘要 Mapper 接口
 */
@Mapper
public interface ConversationSummaryMapper extends BaseMapper<ConversationSummaryEntity> {
}
