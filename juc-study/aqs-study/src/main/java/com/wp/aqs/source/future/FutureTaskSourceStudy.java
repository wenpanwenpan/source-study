package com.wp.aqs.source.future;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.testcontainers.shaded.com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.*;

/**
 * @description: FutureTask源码学习测试
 * @author: panfeng.wen@hand-china
 * @create: 2021/04/17 21:34
 */
public class FutureTaskSourceStudy {

    public static void pool(){
        // 创建线程池方式一
        //org.apache.commons.lang3.concurrent.BasicThreadFactory
        ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1,
                new BasicThreadFactory.Builder().namingPattern("example-schedule-pool-%d").daemon(true).build());


        // 创建线程池方式二
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("demo-pool-%d").build();

        //Common Thread Pool
        ExecutorService pool = new ThreadPoolExecutor(5, 200,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(1024), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy());
        pool.execute(()-> System.out.println(Thread.currentThread().getName()));

        final FutureTask<Integer> futureTask = new FutureTask<>(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return null;
            }
        });
        // pool.execute(futureTask);

        pool.submit(futureTask);

        //gracefully shutdown
        pool.shutdown();
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException, TimeoutException {


        final ExecutorService executorService = Executors.newFixedThreadPool(3);
        executorService.submit(new Runnable() {
            @Override
            public void run() {

            }
        });
        executorService.execute(new Runnable() {
            @Override
            public void run() {

            }
        });
        final FutureTask<Integer> futureTask = new FutureTask<>(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return null;
            }
        });

        // 提交到线程池
        Future<?> future = executorService.submit(futureTask);
        // 获取任务执行结果
        Object result = future.get();
        // 带超时时间的获取任务执行结果方法。
        future.get(100,TimeUnit.MICROSECONDS);
        // 如果任务还没有被执行，可以调用 cancel 方法取消该任务
        future.cancel(true);
        // 判断该任务是否已经被取消了
        future.isCancelled();
    }

}
