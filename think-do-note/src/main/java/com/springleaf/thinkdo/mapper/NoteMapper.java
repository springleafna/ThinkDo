package com.springleaf.thinkdo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.springleaf.thinkdo.domain.entity.NoteEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 笔记Mapper
 */
@Mapper
public interface NoteMapper extends BaseMapper<NoteEntity> {
}
