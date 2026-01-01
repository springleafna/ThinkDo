package com.springleaf.thinkdo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.springleaf.thinkdo.domain.entity.MemoEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 便签Mapper
 */
@Mapper
public interface MemoMapper extends BaseMapper<MemoEntity> {
}
