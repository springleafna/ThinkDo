package com.springleaf.thinkdo.chat;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * OpenAI 协议风格 SSE 解析器
 * 支持从 delta/message 中提取 content，以及可选的 reasoning_content
 */
final class OpenAIStyleSseParser {

    /**
     * 使用示例：解析标准的内容块（delta 模式）
     * 输入：data: {"choices":[{"delta":{"content":"你好"},"finish_reason":null}]}
     * 输出：ParsedEvent[content="你好", reasoning=null, completed=false]
     */

    /**
     * 使用示例：解析完成标记
     * 输入：data: [DONE]
     * 输出：ParsedEvent[content=null, reasoning=null, completed=true]
     */

    /**
     * 使用示例：解析包含 finish_reason 的完成事件
     * 输入：data: {"choices":[{"delta":{},"finish_reason":"stop"}]}
     * 输出：ParsedEvent[content=null, reasoning=null, completed=true]
     */

    /**
     * 使用示例：解析 message 模式（非流式）
     * 输入：data: {"choices":[{"message":{"content":"完整回复"},"finish_reason":"stop"}]}
     * 输出：ParsedEvent[content="完整回复", reasoning=null, completed=true]
     */

    /**
     * 使用示例：解析包含推理内容的响应（reasoningEnabled=true）
     * 输入：data: {"choices":[{"delta":{"content":"答案","reasoning_content":"思考过程"},"finish_reason":null}]}
     * 输出：ParsedEvent[content="答案", reasoning="思考过程", completed=false]
     */

    /**
     * 使用示例：解析空行或无效数据
     * 输入：(空行) 或 data: {"choices":[]}
     * 输出：ParsedEvent[content=null, reasoning=null, completed=false]
     */

    /** SSE 数据行前缀标识 */
    private static final String DATA_PREFIX = "data:";

    /** SSE 流结束标记 */
    private static final String DONE_MARKER = "[DONE]";

    /**
     * 私有构造函数，防止外部实例化。
     */
    private OpenAIStyleSseParser() {
    }

    /**
     * 解析单行 SSE 数据，提取事件内容。
     * 该方法支持 OpenAI 风格的 SSE 响应格式，从 choices 数组的第一个元素中提取
     * delta 或 message 对象中的文本内容。
     *
     * @param line SSE 数据行
     * @param gson Gson 实例，用于解析 JSON 数据
     * @param reasoningEnabled 是否启用推理内容提取
     * @return 解析后的事件对象，包含内容、推理内容和完成状态
     */
    static ParsedEvent parseLine(String line, Gson gson, boolean reasoningEnabled) {
        // 空行处理
        if (line == null || line.isBlank()) {
            return ParsedEvent.empty();
        }

        // 去除 data: 前缀
        String payload = line.trim();
        if (payload.startsWith(DATA_PREFIX)) {
            payload = payload.substring(DATA_PREFIX.length()).trim();
        }

        // 检查流结束标记
        if (DONE_MARKER.equalsIgnoreCase(payload)) {
            return ParsedEvent.done();
        }

        // 解析 JSON 并提取 choices 数组
        JsonObject obj = gson.fromJson(payload, JsonObject.class);
        JsonArray choices = obj.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) {
            return ParsedEvent.empty();
        }

        // 从第一个 choice 中提取内容和推理信息
        JsonObject choice0 = choices.get(0).getAsJsonObject();
        String content = extractText(choice0, "content");
        String reasoning = reasoningEnabled ? extractText(choice0, "reasoning_content") : null;
        boolean completed = hasFinishReason(choice0);

        return new ParsedEvent(content, reasoning, completed);
    }

    /**
     * 检查是否包含完成原因标记。
     * 当 finish_reason 字段存在且不为 null 时，表示流式响应已完成。
     *
     * @param choice choice 对象
     * @return 如果包含 finish_reason 字段则返回 true，否则返回 false
     */
    private static boolean hasFinishReason(JsonObject choice) {
        if (choice == null || !choice.has("finish_reason")) {
            return false;
        }
        JsonElement finishReason = choice.get("finish_reason");
        return finishReason != null && !finishReason.isJsonNull();
    }

    /**
     * 从 choice 对象中提取指定字段的文本内容。
     * 支持从 delta（流式增量）和 message（完整消息）两种结构中提取。
     *
     * @param choice choice 对象
     * @param fieldName 要提取的字段名称（如 content、reasoning_content）
     * @return 提取的文本内容，如果不存在则返回 null
     */
    private static String extractText(JsonObject choice, String fieldName) {
        if (choice == null) {
            return null;
        }

        // 从 delta 对象中提取（流式增量模式）
        if (choice.has("delta") && choice.get("delta").isJsonObject()) {
            JsonObject delta = choice.getAsJsonObject("delta");
            if (delta.has(fieldName)) {
                JsonElement value = delta.get(fieldName);
                if (value != null && !value.isJsonNull()) {
                    return value.getAsString();
                }
            }
        }

        // 从 message 对象中提取（完整消息模式）
        if (choice.has("message") && choice.get("message").isJsonObject()) {
            JsonObject message = choice.getAsJsonObject("message");
            if (message.has(fieldName)) {
                JsonElement value = message.get(fieldName);
                if (value != null && !value.isJsonNull()) {
                    return value.getAsString();
                }
            }
        }
        return null;
    }

    /**
     * 解析后的事件记录类。
     * 封装了从 SSE 事件中提取的内容、推理内容和完成状态。
     *
     * @param content 文本内容
     * @param reasoning 推理内容（可选）
     * @param completed 是否已完成
     */
    record ParsedEvent(String content, String reasoning, boolean completed) {

        /**
         * 创建空事件实例。
         *
         * @return 空事件对象
         */
        static ParsedEvent empty() {
            return new ParsedEvent(null, null, false);
        }

        /**
         * 创建完成事件实例。
         *
         * @return 完成事件对象
         */
        static ParsedEvent done() {
            return new ParsedEvent(null, null, true);
        }

        /**
         * 检查是否包含有效内容。
         *
         * @return 如果 content 非空则返回 true，否则返回 false
         */
        boolean hasContent() {
            return content != null && !content.isEmpty();
        }

        /**
         * 检查是否包含有效推理内容。
         *
         * @return 如果 reasoning 非空则返回 true，否则返回 false
         */
        boolean hasReasoning() {
            return reasoning != null && !reasoning.isEmpty();
        }
    }
}
