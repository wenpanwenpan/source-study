package com.wp.aqs.source.cas;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicStampedReference;

/**
 *   CAS的ABA问题解决
 * @author wenpan
 *  @create  2021/04/18 17:20
 */
public class CasABAQuestionSolve {

    /** 给定初始值和初始版本，并制定 AtomicStampedReference 的泛型为 Integer */
    static AtomicStampedReference<Integer> atomicStampedReference = new AtomicStampedReference(1,1);
    static CountDownLatch countDownLatch = new CountDownLatch(2);

    public static void main(String[] args) throws InterruptedException {

        Thread thread1 = new Thread(() -> {
            try {

                // 期望值
                int expectReference = atomicStampedReference.getReference();
                // 新值
                int newNum = expectReference + 1;
                // 期望版本
                int expectStamp = atomicStampedReference.getStamp();
                // 新版本
                int newStamp = expectStamp + 1;
                System.out.println("thread-1 获取到期望值 : " + expectReference);
                // 线程1线睡1s，让出CPU
                TimeUnit.SECONDS.sleep(1);
                // 使用cas，如果 atomicStampedReference 的期望值等于 expectReference，
                // 并且 atomicStampedReference 的期望版本等于 expectStamp
                // 则更新 atomicStampedReference 的值为新值newNum，更新 atomicStampedReference的版本为新版本newStamp
                boolean flag = atomicStampedReference.compareAndSet(expectReference, newNum, expectStamp, newStamp);
                System.out.println("thread-1 通过 atomicStampedReference cas 更新值是否成功： " + flag);
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
                // 期望值
                int expectNum = atomicStampedReference.getReference();
                // 新值
                int newNum = expectNum + 1;
                // 期望版本
                int expectStamp = atomicStampedReference.getStamp();
                int newStamp = expectStamp + 1;
                System.out.println("thread-2 获取到期望值 ： " + expectNum + "， 新值是：" + newNum);
                // 使用cas，如果 atomicStampedReference 的期望值等于 expectReference，
                // 并且 atomicStampedReference 的期望版本等于 expectStamp
                // 则更新 atomicStampedReference 的值为新值newNum，更新 atomicStampedReference的版本为新版本newStamp
                atomicStampedReference.compareAndSet(expectNum,newNum,expectStamp,newStamp);
                System.out.println("thread-2 更新完毕，atomicStampedReference 的值是： " + atomicStampedReference.getReference()
                        + " 版本号是： " + atomicStampedReference.getStamp());
                // 更新完毕后将 atomicInteger 值减一
                atomicStampedReference.compareAndSet(atomicStampedReference.getReference(),atomicStampedReference.getReference() -1,
                        atomicStampedReference.getStamp(),atomicStampedReference.getStamp());
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
