package com.springleaf.thinkdo.constant;

public class ChatConstant {

    /**
     * 会话标题生成提示词模板路径
     * 通过 {@code {title_max_chars}} 与 {@code {question}} 控制标题长度与输入问题
     */
    public static final String CONVERSATION_TITLE_PROMPT_PATH = "prompt/conversation-title.st";

    /**
     * 意图识别提示词模板路径（串行模式）
     * 一次性发送所有意图节点给 LLM 进行识别
     */
    public static final String INTENT_CLASSIFIER_PROMPT_PATH = "prompt/intent-classifier.st";


}
