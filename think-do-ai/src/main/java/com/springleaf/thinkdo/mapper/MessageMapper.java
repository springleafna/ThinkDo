package com.springleaf.thinkdo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.springleaf.thinkdo.domain.entity.MessageEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话消息记录 Mapper 接口
 */
@Mapper
public interface MessageMapper extends BaseMapper<MessageEntity> {
}
