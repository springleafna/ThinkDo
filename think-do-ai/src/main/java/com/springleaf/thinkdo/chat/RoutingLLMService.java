package com.springleaf.thinkdo.chat;

import cn.hutool.core.collection.CollUtil;
import com.springleaf.thinkdo.chat.stream.StreamCallback;
import com.springleaf.thinkdo.chat.stream.StreamCancellationHandle;
import com.springleaf.thinkdo.enums.ModelCapability;
import com.springleaf.thinkdo.enums.ResultCodeEnum;
import com.springleaf.thinkdo.exception.BusinessException;
import com.springleaf.thinkdo.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 路由式 LLM 服务实现类
 */
@Slf4j
@Service
@Primary
public class RoutingLLMService implements LLMService {

    private static final int FIRST_PACKET_TIMEOUT_SECONDS = 60;
    private static final String STREAM_INTERRUPTED_MESSAGE = "流式请求被中断";
    private static final String STREAM_NO_PROVIDER_MESSAGE = "无可用大模型提供者";
    private static final String STREAM_START_FAILED_MESSAGE = "流式请求启动失败";
    private static final String STREAM_TIMEOUT_MESSAGE = "流式首包超时";
    private static final String STREAM_NO_CONTENT_MESSAGE = "流式请求未返回内容";
    private static final String STREAM_ALL_FAILED_MESSAGE = "大模型调用失败，请稍后再试...";

    private final ModelSelector selector;
    private final ModelHealthStore healthStore;
    private final Map<String, ChatClient> clientsByProvider;
    private final ModelRoutingExecutor executor;


    public RoutingLLMService(
            ModelSelector selector,
            ModelHealthStore healthStore,
            ModelRoutingExecutor executor,
            List<ChatClient> clients) {
        this.selector = selector;
        this.healthStore = healthStore;
        this.executor = executor;
        this.clientsByProvider = clients.stream()
                .collect(Collectors.toMap(ChatClient::provider, Function.identity()));
    }

    @Override
    public String chat(ChatRequest request) {
        return executor.executeWithFallback(
                ModelCapability.CHAT,
                selector.selectChatCandidates(request.getThinking()),
                target -> clientsByProvider.get(target.candidate().getProvider()),
                (client, target) -> client.chat(request, target)
        );
    }

    /**
     * 实现带故障转移的流式聊天功能，按优先级尝试多个模型候选，直到成功或全部失败。
     * 支持首包等待和缓冲机制，确保在模型切换时不会丢失已生成的内容。
     */
    @Override
    public StreamCancellationHandle streamChat(ChatRequest request, StreamCallback callback) {
        // 获取模型候选列表
        List<ModelTarget> targets = selector.selectChatCandidates(request.getThinking());
        if (CollUtil.isEmpty(targets)) {
            throw new BusinessException(STREAM_NO_PROVIDER_MESSAGE);
        }

        String label = ModelCapability.CHAT.getDisplayName();
        Throwable lastError = null;

        // 遍历所有候选模型，按优先级依次尝试
        for (ModelTarget target : targets) {
            ChatClient client = resolveClient(target, label);
            if (client == null) {
                continue;
            }

            FirstPacketAwaiter awaiter = new FirstPacketAwaiter();
            ProbeBufferingCallback wrapper = new ProbeBufferingCallback(callback, awaiter);

            StreamCancellationHandle handle;
            try {
                handle = client.streamChat(request, wrapper, target);
            } catch (Exception e) {
                healthStore.markFailure(target.id());
                lastError = e;
                log.warn("{} 流式请求启动失败，切换下一个模型。modelId：{}，provider：{}",
                        label, target.id(), target.candidate().getProvider(), e);
                continue;
            }
            if (handle == null) {
                healthStore.markFailure(target.id());
                lastError = new BusinessException(STREAM_START_FAILED_MESSAGE, ResultCodeEnum.REMOTE_ERROR);
                log.warn("{} 流式请求未返回取消句柄，切换下一个模型。modelId：{}，provider：{}",
                        label, target.id(), target.candidate().getProvider());
                continue;
            }

            // 使用首包等待器监控首包到达情况
            FirstPacketAwaiter.Result result = awaitFirstPacket(awaiter, handle, callback);

            // 判断首包等待结果，首包成功到达，提交缓冲并返回；否则取消当前请求并尝试下一个模型
            if (result.isSuccess()) {
                wrapper.commit();
                healthStore.markSuccess(target.id());
                return handle;
            }

            // 失败处理
            healthStore.markFailure(target.id());
            handle.cancel();

            lastError = buildLastErrorAndLog(result, target, label);
        }

        // 所有模型都失败了，通知客户端错误
        throw notifyAllFailed(callback, lastError);
    }

    private BusinessException notifyAllFailed(StreamCallback callback, Throwable lastError) {
        BusinessException finalException = new BusinessException(
                STREAM_ALL_FAILED_MESSAGE,
                lastError
        );
        callback.onError(finalException);
        return finalException;
    }

    private Throwable buildLastErrorAndLog(FirstPacketAwaiter.Result result, ModelTarget target, String label) {
        switch (result.getType()) {
            case ERROR -> {
                Throwable error = result.getError() != null
                        ? result.getError()
                        : new BusinessException("流式请求失败", ResultCodeEnum.REMOTE_ERROR);
                log.warn("{} 失败模型: modelId={}, provider={}，原因: 流式请求失败，切换下一个模型",
                        label, target.id(), target.candidate().getProvider(), error);
                return error;
            }
            case TIMEOUT -> {
                BusinessException timeout = new BusinessException(STREAM_TIMEOUT_MESSAGE, ResultCodeEnum.REMOTE_ERROR);
                log.warn("{} 失败模型: modelId={}, provider={}，原因: 流式请求超时，切换下一个模型",
                        label, target.id(), target.candidate().getProvider());
                return timeout;
            }
            case NO_CONTENT -> {
                BusinessException noContent = new BusinessException(STREAM_NO_CONTENT_MESSAGE, ResultCodeEnum.REMOTE_ERROR);
                log.warn("{} 失败模型: modelId={}, provider={}，原因: 流式请求无内容完成，切换下一个模型",
                        label, target.id(), target.candidate().getProvider());
                return noContent;
            }
            default -> {
                BusinessException unknown = new BusinessException("流式请求失败", ResultCodeEnum.REMOTE_ERROR);
                log.warn("{} 失败模型: modelId={}, provider={}，原因: 流式请求失败（未知类型），切换下一个模型",
                        label, target.id(), target.candidate().getProvider());
                return unknown;
            }
        }
    }

    /**
     * 等待首包到达
     *
     * 阻塞等待流式响应的第一个数据包在指定超时时间内到达。
     * 如果等待过程中被中断，则取消请求并抛出业务异常。
     *
     * @param awaiter 首包等待器，用于异步等待首包到达信号
     * @param handle 流式取消句柄，用于在异常情况下取消请求
     * @param callback 流式回调接口，用于在中断时通知客户端错误
     * @return 首包等待结果，包含成功或失败状态及原因
     * @throws BusinessException 当等待过程被中断时抛出此异常
     */
    private FirstPacketAwaiter.Result awaitFirstPacket(FirstPacketAwaiter awaiter,
                                                       StreamCancellationHandle handle,
                                                       StreamCallback callback) {
        try {
            return awaiter.await(FIRST_PACKET_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            handle.cancel();
            BusinessException interruptedException = new BusinessException(STREAM_INTERRUPTED_MESSAGE, e);
            callback.onError(interruptedException);
            throw interruptedException;
        }
    }

    /**
     * 解析聊天客户端
     *
     * 根据模型目标获取对应的聊天客户端实例。
     * 如果客户端不存在，记录警告日志并返回 null。
     *
     * @param target 模型目标，包含候选模型和提供商信息
     * @param label 功能标签，用于日志输出标识当前操作类型
     * @return 聊天客户端实例，如果不存在则返回 null
     */
    private ChatClient resolveClient(ModelTarget target, String label) {
        ChatClient client = clientsByProvider.get(target.candidate().getProvider());
        if (client == null) {
            log.warn("{} 提供商客户端缺失：provider：{}，modelId：{}",
                    label, target.candidate().getProvider(), target.id());
        }
        return client;
    }

    /**
     * 流式首包探测回调：
     * - 探测阶段先缓存事件，避免失败模型的内容污染下游输出
     * - 首包成功后 commit，按原始顺序回放缓存并转实时转发
     */
    private static final class ProbeBufferingCallback implements StreamCallback {

        private final StreamCallback downstream;
        private final FirstPacketAwaiter awaiter;
        private final Object lock = new Object();
        private final List<BufferedEvent> bufferedEvents = new ArrayList<>();
        private volatile boolean committed;

        private ProbeBufferingCallback(StreamCallback downstream, FirstPacketAwaiter awaiter) {
            this.downstream = downstream;
            this.awaiter = awaiter;
            this.committed = false;
        }

        @Override
        public void onContent(String content) {
            awaiter.markContent();
            bufferOrDispatch(BufferedEvent.content(content));
        }

        @Override
        public void onThinking(String content) {
            awaiter.markContent();
            bufferOrDispatch(BufferedEvent.thinking(content));
        }

        @Override
        public void onComplete() {
            awaiter.markComplete();
            bufferOrDispatch(BufferedEvent.complete());
        }

        @Override
        public void onError(Throwable t) {
            awaiter.markError(t);
            bufferOrDispatch(BufferedEvent.error(t));
        }

        /**
         * 首包探测成功后提交：
         * 1. 原子切换为 committed
         * 2. 按事件顺序回放缓存，保证时序一致
         */
        private void commit() {
            List<BufferedEvent> snapshot;
            synchronized (lock) {
                if (committed) {
                    return;
                }
                committed = true;
                if (bufferedEvents.isEmpty()) {
                    return;
                }
                snapshot = new ArrayList<>(bufferedEvents);
                bufferedEvents.clear();
            }
            for (BufferedEvent event : snapshot) {
                dispatch(event);
            }
        }

        private void bufferOrDispatch(BufferedEvent event) {
            boolean dispatchNow;
            synchronized (lock) {
                dispatchNow = committed;
                if (!dispatchNow) {
                    bufferedEvents.add(event);
                }
            }
            if (dispatchNow) {
                dispatch(event);
            }
        }

        private void dispatch(BufferedEvent event) {
            switch (event.type()) {
                case CONTENT -> downstream.onContent(event.content());
                case THINKING -> downstream.onThinking(event.content());
                case COMPLETE -> downstream.onComplete();
                case ERROR -> downstream.onError(event.error() != null
                        ? event.error()
                        : new BusinessException("流式请求失败", ResultCodeEnum.REMOTE_ERROR));
            }
        }

        private record BufferedEvent(EventType type, String content, Throwable error) {

            private static BufferedEvent content(String content) {
                return new BufferedEvent(EventType.CONTENT, content, null);
            }

            private static BufferedEvent thinking(String content) {
                return new BufferedEvent(EventType.THINKING, content, null);
            }

            private static BufferedEvent complete() {
                return new BufferedEvent(EventType.COMPLETE, null, null);
            }

            private static BufferedEvent error(Throwable error) {
                return new BufferedEvent(EventType.ERROR, null, error);
            }
        }

        private enum EventType {
            CONTENT,
            THINKING,
            COMPLETE,
            ERROR
        }
    }
}
