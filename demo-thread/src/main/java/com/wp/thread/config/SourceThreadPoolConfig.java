package com.wp.thread.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @description: source线程池配置
 * @author: 文攀 Mr_wenpan@163.com
 * @date: 2020-12-03 17:30
 **/
@Configuration
public class SourceThreadPoolConfig {

	@Value("${srm-source.core-pool-size:4}")
	private Integer core;
	@Value("${srm-source.max-pool-size:10}")
	private Integer maxPoolSize;
	@Value("${srm-source.keep-alive-seconds:300}")
	private Integer keepAliveSeconds;
	@Value("${srm-source.queue-capacity:200}")
	private Integer queueCapacity;
	@Value("${srm-source.thread-name-prefix:srm-source-thread-execute}")
	private String threadNamePrefix;

	@Bean
	public ThreadPoolTaskExecutor executor() {

		final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		//设置核心线程数
		executor.setCorePoolSize(core);
		//设置最大线程数
		executor.setMaxPoolSize(maxPoolSize);
		//除核心线程外的线程存活时间
		executor.setKeepAliveSeconds(keepAliveSeconds);
		//如果传入值大于0，底层队列使用的是LinkedBlockingQueue,否则默认使用SynchronousQueue
		executor.setQueueCapacity(queueCapacity);
		//线程名称前缀
		executor.setThreadNamePrefix(threadNamePrefix);
		// 设置拒绝策略。当线程池中没有线程的时候调用当前线程池的线程去执行任务
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

		return executor;
	}
}
