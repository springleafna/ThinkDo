package com.springleaf.thinkdo.chat.stream.handler;

import cn.hutool.core.util.StrUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.springleaf.thinkdo.chat.stream.StreamCancellationHandle;
import com.springleaf.thinkdo.domain.dto.CompletionPayload;
import com.springleaf.thinkdo.enums.SSEEventType;
import com.springleaf.thinkdo.http.SseEmitterSender;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * 流式任务管理器：
 * 负责管理所有流式任务的生命周期，包括任务注册、取消、清理等功能。
 * 采用本地缓存 + Redis 分布式锁的机制，支持跨节点的流式任务取消。
 */
@Slf4j
@Component
public class StreamTaskManager {

    /**
     * Redis 发布订阅主题名称，用于在分布式环境下广播任务取消事件
     */
    private static final String CANCEL_TOPIC = "thinkdo:stream:cancel";

    /**
     * Redis 取消标记键前缀，用于存储任务的取消状态
     */
    private static final String CANCEL_KEY_PREFIX = "thinkdo:stream:cancel:";

    /**
     * 取消标记的过期时间，防止永久占用 Redis 存储空间
     */
    private static final Duration CANCEL_TTL = Duration.ofMinutes(30);

    /**
     * 本地任务缓存，使用 Guava Cache 存储活动任务信息
     * 配置自动过期时间和最大容量限制以控制内存使用
     */
    private final Cache<String, StreamTaskInfo> tasks = CacheBuilder.newBuilder()
            .expireAfterWrite(CANCEL_TTL)
            .maximumSize(10000)
            .build();

    /**
     * Redis 监听器 ID，用于在销毁时取消订阅
     */
    private int listenerId = -1;

    private final RedissonClient redissonClient;

    public StreamTaskManager(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 初始化订阅 Redis 取消主题，监听其他节点发送的任务取消事件。
     * 当收到取消消息时，同步取消本地对应的任务。
     */
    @PostConstruct
    public void subscribe() {
        RTopic topic = redissonClient.getTopic(CANCEL_TOPIC);
        listenerId = topic.addListener(String.class, (channel, taskId) -> {
            if (StrUtil.isBlank(taskId)) {
                return;
            }
            cancelLocal(taskId);
        });
    }

    /**
     * 销毁前取消订阅 Redis 主题，释放资源。
     */
    @PreDestroy
    public void unsubscribe() {
        if (listenerId == -1) {
            return;
        }
        redissonClient.getTopic(CANCEL_TOPIC).removeListener(listenerId);
    }

    /**
     * 注册流式任务到管理器。
     * 将任务的发送器和取消回调保存到本地缓存，并检查是否已在 Redis 中被标记为取消。
     *
     * @param taskId 任务唯一标识符
     * @param sender SSE 发送器，用于向客户端推送事件
     * @param onCancelSupplier 取消时的回调函数，用于获取已累积的内容作为取消事件的载荷
     */
    public void register(String taskId, SseEmitterSender sender, Supplier<CompletionPayload> onCancelSupplier) {
        StreamTaskInfo taskInfo = getOrCreate(taskId);
        taskInfo.sender = sender;
        taskInfo.onCancelSupplier = onCancelSupplier;
        if (isTaskCancelledInRedis(taskId, taskInfo)) {
            CompletionPayload payload = taskInfo.onCancelSupplier.get();
            sendCancelAndDone(sender, payload);
            sender.complete();
        }
    }

    /**
     * 绑定流式取消句柄到指定任务。
     * 如果任务已被标记为取消，则立即执行取消操作。
     *
     * @param taskId 任务唯一标识符
     * @param handle 流式取消句柄，用于实际取消正在执行的流式请求
     */
    public void bindHandle(String taskId, StreamCancellationHandle handle) {
        StreamTaskInfo taskInfo = getOrCreate(taskId);
        taskInfo.handle = handle;
        if (taskInfo.cancelled.get() && handle != null) {
            handle.cancel();
        }
    }

    /**
     * 检查任务是否已被取消。
     *
     * @param taskId 任务唯一标识符
     * @return 如果任务已取消返回 true，否则返回 false
     */
    public boolean isCancelled(String taskId) {
        StreamTaskInfo info = tasks.getIfPresent(taskId);
        return info != null && info.cancelled.get();
    }

    /**
     * 取消指定任务。
     * 先在 Redis 中设置取消标记（带 TTL），然后发布取消消息到 Redis 主题，通知所有节点取消该任务。
     *
     * @param taskId 任务唯一标识符
     */
    public void cancel(String taskId) {
        // 先设置 Redis 标记，再发布消息
        RBucket<Boolean> bucket = redissonClient.getBucket(cancelKey(taskId));
        bucket.set(Boolean.TRUE, CANCEL_TTL);

        // 发布消息通知所有节点（包括本地）
        // 本地节点也通过监听器统一处理，避免重复调用 cancelLocal
        redissonClient.getTopic(CANCEL_TOPIC).publish(taskId);
    }

    /**
     * 检查任务是否在 Redis 中被标记为已取消。
     * 如果是，会同步状态到本地缓存。
     *
     * @param taskId 任务唯一标识符
     * @param taskInfo 本地缓存中的任务信息对象
     * @return 如果任务在 Redis 中已取消返回 true，否则返回 false
     */
    private boolean isTaskCancelledInRedis(String taskId, StreamTaskInfo taskInfo) {
        if (taskInfo.cancelled.get()) {
            return true;
        }

        RBucket<Boolean> bucket = redissonClient.getBucket(cancelKey(taskId));
        Boolean cancelled = bucket.get();
        if (Boolean.TRUE.equals(cancelled)) {
            taskInfo.cancelled.set(true);
            return true;
        }
        return false;
    }

    /**
     * 本地取消任务。
     * 使用 CAS 原子操作确保只执行一次取消逻辑，包括取消远程调用、发送取消事件和完成事件。
     *
     * @param taskId 任务唯一标识符
     */
    private void cancelLocal(String taskId) {
        StreamTaskInfo taskInfo = tasks.getIfPresent(taskId);
        if (taskInfo == null) {
            return;
        }

        // 使用 CAS 确保只执行一次
        if (!taskInfo.cancelled.compareAndSet(false, true)) {
            return;
        }

        if (taskInfo.handle != null) {
            taskInfo.handle.cancel();
        }

        // 在取消时执行回调，保存已累积的内容
        if (taskInfo.sender != null) {
            CompletionPayload payload = taskInfo.onCancelSupplier.get();
            sendCancelAndDone(taskInfo.sender, payload);
            taskInfo.sender.complete();
        }
    }

    /**
     * 注销指定任务，清理相关资源。
     * 同时清理本地缓存和 Redis 中的取消标记。
     *
     * @param taskId 任务唯一标识符
     */
    public void unregister(String taskId) {
        // 清理本地缓存
        tasks.invalidate(taskId);

        // 清理 Redis
        redissonClient.getBucket(cancelKey(taskId)).deleteAsync();
    }

    /**
     * 生成 Redis 取消键。
     *
     * @param taskId 任务唯一标识符
     * @return Redis 取消键字符串
     */
    private String cancelKey(String taskId) {
        return CANCEL_KEY_PREFIX + taskId;
    }

    /**
     * 发送取消和完成事件给客户端。
     *
     * @param sender SSE 发送器
     * @param payload 完成载荷，如果为 null 则使用默认空载荷
     */
    private void sendCancelAndDone(SseEmitterSender sender, CompletionPayload payload) {
        CompletionPayload actualPayload = payload == null ? new CompletionPayload(null, null) : payload;
        sender.sendEvent(SSEEventType.CANCEL.value(), actualPayload);
        sender.sendEvent(SSEEventType.DONE.value(), "[DONE]");
    }

    /**
     * 获取或创建任务信息对象。
     *
     * @param taskId 任务唯一标识符
     * @return 任务信息对象
     */
    @SneakyThrows
    private StreamTaskInfo getOrCreate(String taskId) {
        return tasks.get(taskId, StreamTaskInfo::new);
    }

    /**
     * 流式任务信息内部类。
     * 存储任务的取消状态、取消句柄、发送器和取消回调等信息。
     */
    private static final class StreamTaskInfo {
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private volatile StreamCancellationHandle handle;
        private volatile SseEmitterSender sender;
        private volatile Supplier<CompletionPayload> onCancelSupplier;
    }
}

