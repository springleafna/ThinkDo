package com.springleaf.thinkdo.retrieve.prompt;

import com.springleaf.thinkdo.domain.dto.RetrievedChunk;
import com.springleaf.thinkdo.intent.NodeScore;

import java.util.List;
import java.util.Map;

public interface ContextFormatter {

    String formatKbContext(List<NodeScore> kbIntents, Map<String, List<RetrievedChunk>> rerankedByIntent, int topK);

    String formatMcpContext(List<NodeScore> mcpIntents);

}
