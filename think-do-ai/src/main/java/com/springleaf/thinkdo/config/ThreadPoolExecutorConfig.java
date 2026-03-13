package com.springleaf.thinkdo.config;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.alibaba.ttl.threadpool.TtlExecutors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池执行器配置类
 * 为系统中不同的业务场景配置独立的线程池，提高并发处理能力
 */
@Configuration
public class ThreadPoolExecutorConfig {

    /**
     * CPU核心数，用于动态计算线程池大小
     */
    public static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    /**
     * 模型流式输出线程池
     */
    @Bean
    public Executor modelStreamExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                Math.max(2, CPU_COUNT >> 1),
                Math.max(4, CPU_COUNT),
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(200),
                ThreadFactoryBuilder.create()
                        .setNamePrefix("model_stream_executor_")
                        .build(),
                new ThreadPoolExecutor.AbortPolicy()
        );
        return TtlExecutors.getTtlExecutor(executor);
    }

}
