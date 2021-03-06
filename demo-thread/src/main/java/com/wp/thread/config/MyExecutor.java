package com.wp.thread.config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class MyExecutor {

    /**
     * 获取系统可用数量
     */
    private static int maximumPoolSize = Runtime.getRuntime().availableProcessors();

    public ExecutorService getThreadPool() {
        ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("myThread-handle-%d ").build();

        int corePoolSize = 4;
        return new ThreadPoolExecutor(corePoolSize, maximumPoolSize, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(500),factory);
    }
}