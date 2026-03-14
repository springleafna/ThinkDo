package com.springleaf.thinkdo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.springleaf.thinkdo.domain.entity.ConversationEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话列表 Mapper 接口
 */
@Mapper
public interface ConversationMapper extends BaseMapper<ConversationEntity> {
}
