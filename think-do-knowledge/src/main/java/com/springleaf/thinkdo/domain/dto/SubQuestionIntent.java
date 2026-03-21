package com.springleaf.thinkdo.domain.dto;

import com.springleaf.thinkdo.intent.NodeScore;

import java.util.List;

/**
 * 子问题与其意图候选
 *
 * @param subQuestion 子问题文本
 * @param nodeScores  子问题的意图候选
 */
public record SubQuestionIntent(String subQuestion, List<NodeScore> nodeScores) {
}
