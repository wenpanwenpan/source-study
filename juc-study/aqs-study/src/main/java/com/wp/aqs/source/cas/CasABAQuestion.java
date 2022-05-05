package com.wp.aqs.source.cas;

import java.sql.Time;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *   CAS的ABA问题
 * @author wenpan
 *  @create  2021/04/18 17:06
 */
public class CasABAQuestion {

    /**
     * 使用 AtomicInteger 解决多线程操作变量的安全性问题（保证自增的原子性，但不能解决ABA问题）
     */
    static AtomicInteger atomicInteger = new AtomicInteger(1);
    static CountDownLatch countDownLatch = new CountDownLatch(2);

    public static void main(String[] args) throws InterruptedException {

        Thread thread1 = new Thread(() -> {
            try {
                // 期望值
                int expectNum = atomicInteger.get();
                // 新值
                int newNum = expectNum + 1;
                System.out.println("thread-2 获取到期望值 : " + expectNum);
                // 线程1线睡1s，让出CPU
                TimeUnit.SECONDS.sleep(1);
                // 使用cas，如果 atomicInteger 的值等于期望值，则更新 atomicInteger 的值为新值
                boolean flag = atomicInteger.compareAndSet(expectNum, newNum);
                // int increment = atomicInteger.incrementAndGet();
                System.out.println("thread-1 通过 cas 更新值是否成功： " + flag);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }finally {
                countDownLatch.countDown();
            }
        },"thread-1");
        Thread thread2 = new Thread(() -> {
            try {
                // 线程2线睡 50 ms
                TimeUnit.MICROSECONDS.sleep(50);
                int expectNum = atomicInteger.get();
                int newNum = expectNum + 1;
                System.out.println("thread-2 获取到期望值 ： " + expectNum + "， 新值是：" + newNum);
                // 使用cas，如果 atomicInteger 的值等于期望值，则更新 atomicInteger 的值为新值
                atomicInteger.compareAndSet(expectNum,newNum);
                System.out.println("thread-2 更新完毕，atomicInteger 的值是： " + atomicInteger.get());
                // 更新完毕后将 atomicInteger 值减一
                atomicInteger.decrementAndGet();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }finally {
                countDownLatch.countDown();
            }
        },"thread-2");

        thread1.start();
        thread2.start();

        // 主线程在这里阻塞等待 thread-1 和 thread-2 执行完毕！
        countDownLatch.await();
    }
}
