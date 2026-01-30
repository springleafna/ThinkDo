package com.springleaf.thinkdo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.springleaf.thinkdo.domain.entity.NotificationEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息通知Mapper
 */
@Mapper
public interface NotificationMapper extends BaseMapper<NotificationEntity> {
}
