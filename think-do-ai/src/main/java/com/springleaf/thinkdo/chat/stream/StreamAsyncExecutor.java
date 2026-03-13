package com.springleaf.thinkdo.chat.stream;

import okhttp3.Call;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 流式异步执行器工具类，用于在异步线程池中执行流式任务并提供取消支持。
 * 该类管理异步任务的生命周期，包括资源清理和错误处理。
 */
public final class StreamAsyncExecutor {

    /**
     * 提交流式异步任务到执行器。
     * 该方法将任务异步提交到指定的线程池执行，同时提供资源管理和取消支持。
     * 当线程池满时，会主动释放资源并回调错误信息。
     *
     * @param executor 线程池，用于执行异步任务
     * @param call 需要释放的资源（如 OkHttp Call）
     * @param callback 回调接口，用于通知任务执行结果和错误
     * @param task 实际的流式任务，接收取消状态标记作为参数
     * @return 流式取消句柄，用于取消正在执行的任务
     * @throws IllegalStateException 当线程池已满时的异常
     */
    public static StreamCancellationHandle submit(
            Executor executor,           // 线程池
            Call call,                   // 需要释放的资源（如 OkHttp Call）
            StreamCallback callback,     // 回调接口
            Consumer<AtomicBoolean> task // 实际的流式任务
    ) {
        // 创建取消状态标记
        AtomicBoolean cancelled = new AtomicBoolean(false);

        try {
            // 异步执行任务
            CompletableFuture.runAsync(() -> task.accept(cancelled), executor);
        } catch (RejectedExecutionException e) {
            // 线程池满：主动释放资源并回调错误
            call.cancel();
            callback.onError(new IllegalStateException("线程池已满，请求被拒绝", e));
            return StreamCancellationHandles.noop();
        }

        // 返回可取消的句柄
        return StreamCancellationHandles.fromOkHttp(call, cancelled);
    }
}

