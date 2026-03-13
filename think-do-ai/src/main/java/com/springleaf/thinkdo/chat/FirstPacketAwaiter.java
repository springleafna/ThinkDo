package com.springleaf.thinkdo.chat;

import lombok.Getter;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 首包等待器 - 用于等待第一个数据包到达的同步工具
 * 支持超时等待，并可区分成功、错误、超时、无内容等不同状态
 */
public class FirstPacketAwaiter {

    /**
     * 倒计时锁存器，用于等待首包到达信号
     * 初始计数为 1，当首包到达时计数减为 0，释放等待线程
     */
    private final CountDownLatch latch = new CountDownLatch(1);

    /**
     * 原子布尔值，标记是否已接收到内容数据
     * true 表示已接收到有效内容，false 表示未接收到
     */
    private final AtomicBoolean hasContent = new AtomicBoolean(false);

    /**
     * 原子布尔值，标记事件是否已触发
     * 防止重复触发或重复处理
     */
    private final AtomicBoolean eventFired = new AtomicBoolean(false);

    /**
     * 原子引用，存储等待过程中发生的错误
     * 如果等待过程中出现异常，通过此引用传递给等待方
     */
    private final AtomicReference<Throwable> error = new AtomicReference<>();

    /**
     * 标记收到内容
     */
    public void markContent() {
        hasContent.set(true);
        fireEventOnce();
    }

    /**
     * 标记完成
     */
    public void markComplete() {
        fireEventOnce();
    }

    /**
     * 标记错误
     */
    public void markError(Throwable t) {
        error.set(t);
        fireEventOnce();
    }

    /**
     * 确保只触发一次事件
     */
    private void fireEventOnce() {
        if (eventFired.compareAndSet(false, true)) {
            latch.countDown();
        }
    }

    /**
     * 等待结果
     */
    public Result await(long timeout, TimeUnit unit) throws InterruptedException {
        boolean completed = latch.await(timeout, unit);

        if (error.get() != null) {
            return Result.error(error.get());
        }
        if (!completed) {
            return Result.timeout();
        }
        if (!hasContent.get()) {
            return Result.noContent();
        }
        return Result.success();
    }

    /**
     * 结果封装
     */
    @Getter
    public static class Result {

        public enum Type {SUCCESS, ERROR, TIMEOUT, NO_CONTENT}

        private final Type type;
        private final Throwable error;

        private Result(Type type, Throwable error) {
            this.type = type;
            this.error = error;
        }

        public static Result success() {
            return new Result(Type.SUCCESS, null);
        }

        public static Result error(Throwable t) {
            return new Result(Type.ERROR, t);
        }

        public static Result timeout() {
            return new Result(Type.TIMEOUT, null);
        }

        public static Result noContent() {
            return new Result(Type.NO_CONTENT, null);
        }

        public boolean isSuccess() {
            return type == Type.SUCCESS;
        }
    }
}
