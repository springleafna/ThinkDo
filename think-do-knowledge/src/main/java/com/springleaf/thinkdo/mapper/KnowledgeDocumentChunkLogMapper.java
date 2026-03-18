package com.springleaf.thinkdo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.springleaf.thinkdo.domain.entity.KnowledgeDocumentChunkLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库文档分块日志 Mapper
 */
@Mapper
public interface KnowledgeDocumentChunkLogMapper extends BaseMapper<KnowledgeDocumentChunkLogEntity> {
}
