package com.wp.aqs.source.cas;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @description: cas原理
 * 模拟100个人每个人访问一个网站10次，使用一个变量 Count 来记录网站被访问的总次数，每访问一次就将访问次数加一
 * @author: panfeng.wen@hand-china
 * @create: 2021/04/18 15:46
 */
public class CasSourceStudy {

    /** 用 volatile 保证 COUNT 在多个线程中可见*/
    static volatile int COUNT = 0;

    public static void main(String[] args) throws InterruptedException {

        long start = System.currentTimeMillis();
        /** 线程数量 */
        int threadCount = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 模拟 threadCount 个线程对 COUNT 进行++操作，每个线程加10次
        for (int i = 0; i < threadCount; i++) {
            new Thread(()->{
                try {
                    for (int j = 0; j < 10; j++) {
                        request2();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }finally {
                    latch.countDown();
                }
            }).start();
        }

        // 等上面的线程跑完了再执行这里
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("一共耗时： " + (end - start) + "毫秒 , count = " + COUNT);
    }

    /**
     * 该方法会有多线程安全问题
     * @return: void
     * @date: 2021/4/18 4:11 下午
     * @auther: panfeng.wen@hand-china.com
     */
    public static synchronized void request() throws InterruptedException {
        /**
         * COUNT++; 不是一个原子操作，一共有三步：
         * 获取COUNT 的值 ： A = COUNT
         * 将A的值加一，然后赋值给B ： B = A + 1
         * 将B赋值给count ： COUNT = B
         */
        TimeUnit.MICROSECONDS.sleep(5);
        // 这里会有多线程安全问题
        COUNT++;
    }

    /**
     * 手动模拟CAS
     * @return: void
     * @date: 2021/4/19 10:51 上午
     * @auther: panfeng.wen@hand-china.com
     */
    public static void request2() throws InterruptedException {
        TimeUnit.MICROSECONDS.sleep(5);
        // 期望值
        int expectValue;
        while (!compareAndSwap(expectValue = getCount(),expectValue + 1)){}
    }

    /**
     * 自己实现的cas
     * @param expectValue 期待的值
     * @param newValue 要更新的值
     * @auther: panfeng.wen@hand-china.com
     */
    public static synchronized boolean compareAndSwap(int expectValue, int newValue){
        // 只有 expectValue = 原来的值的时候采取更新 COUNT ，否则直接返回false
        if(expectValue == getCount()){
            COUNT = newValue;
            return true;
        }
        return false;
    }

    public static int getCount(){
        return COUNT;
    }

}
