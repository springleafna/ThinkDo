package com.springleaf.thinkdo.chat.stream;

import okhttp3.Call;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 流式取消操作句柄工具类，提供创建和管理流式请求取消句柄的静态方法。
 * 该类为不可实例化的工具类，所有方法均为静态方法。
 */
public final class StreamCancellationHandles {

    /**
     * 私有构造函数，防止外部实例化。
     */
    private StreamCancellationHandles() {}

    /**
     * 返回一个空实现的流式取消句柄实例。
     * 该句柄的 cancel() 方法不执行任何操作，适用于不需要实际取消操作的场景。
     *
     * @return 空实现的流式取消句柄实例
     */
    public static StreamCancellationHandle noop() {
        return NoopCancellationHandle.INSTANCE;
    }

    /**
     * 基于 OkHttp Call 对象创建流式取消句柄。
     * 该方法创建的句柄可用于取消正在执行的 OkHttp 请求。
     *
     * @param call OkHttp Call 对象，用于执行和取消 HTTP 请求
     * @param cancelled 取消状态标记，用于记录取消操作是否已执行
     * @return OkHttp 实现的流式取消句柄实例
     */
    public static StreamCancellationHandle fromOkHttp(Call call, AtomicBoolean cancelled) {
        return new OkHttpCancellationHandle(call, cancelled);
    }

    /**
     * 空实现的取消操作句柄内部类。
     * 该类实现了单例模式，提供无操作的取消实现。
     */
    private static final class NoopCancellationHandle implements StreamCancellationHandle {

        /** 单例实例 */
        static final NoopCancellationHandle INSTANCE = new NoopCancellationHandle();

        /**
         * 空实现的取消方法，不执行任何操作。
         */
        @Override
        public void cancel() {

        }
    }

    /**
     * OkHttp 取消操作句柄实现类，用于管理 HTTP 请求的取消操作。
     * 该类实现了线程安全的取消机制，确保取消操作只执行一次。
     */
    private static final class OkHttpCancellationHandle implements StreamCancellationHandle {

        /** OkHttp Call 对象，用于执行和取消 HTTP 请求 */
        private final Call call;

        /** 取消状态标记，用于记录取消操作是否已执行 */
        private final AtomicBoolean cancelled;

        /** 一次性标志，确保取消操作只执行一次 */
        private final AtomicBoolean once = new AtomicBoolean(false);

        /**
         * 构造 OkHttp 取消操作句柄。
         *
         * @param call OkHttp Call 对象，用于执行和取消 HTTP 请求
         * @param cancelled 取消状态标记，记录取消操作是否已执行
         */
        OkHttpCancellationHandle(Call call, AtomicBoolean cancelled) {
            this.call = call;
            this.cancelled = cancelled;
        }

        /**
         * 执行取消操作，取消关联的 HTTP 请求。
         * 该方法保证幂等性，多次调用只会执行一次实际的取消操作。
         * 取消流程：
         * 1. 使用 CAS 操作确保只执行一次
         * 2. 设置取消状态标记
         * 3. 取消 OkHttp Call 对象
         */
        @Override
        public void cancel() {
            // 使用 CAS 操作确保取消逻辑只执行一次
            if (!once.compareAndSet(false, true)) {
                return;
            }

            // 设置取消状态标记
            if (cancelled != null) {
                cancelled.set(true);
            }

            // 取消 OkHttp Call 对象
            if (call != null) {
                call.cancel();
            }
        }
    }
}
