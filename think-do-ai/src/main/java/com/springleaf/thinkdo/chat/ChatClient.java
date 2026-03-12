package com.springleaf.thinkdo.chat;

import com.springleaf.thinkdo.enums.ModelProvider;
import com.springleaf.thinkdo.model.ModelTarget;

/**
 * 聊天客户端接口
 * 定义了与AI模型进行对话的核心方法
 */
public interface ChatClient {

    /**
     * 获取服务提供商名称
     *
     * @return 服务提供商标识：{@link ModelProvider}
     */
    String provider();

    /**
     * 同步聊天方法
     * 发送请求并等待完整响应返回
     *
     * @param request 聊天请求对象，包含用户消息和对话历史
     * @param target  目标模型配置，指定使用的具体模型
     * @return 模型返回的完整响应文本
     */
    String chat(ChatRequest request, ModelTarget target);
}
