package com.wp.aqs.source;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author panfeng.wen@hand-china.com
 * @Description
 * @Date 2021/1/20 10:16
 */
public class Main {

    public static void main(String[] args) {
        // 锁
        final ReentrantLock reentrantLock = new ReentrantLock();

        // 创建并开启3个线程
        for (int i = 0; i < 3; i++) {
            new Thread(()->Main.ooxx(reentrantLock)).start();
        }

    }

    public static void ooxx(Lock lock){
        // 加锁
        lock.lock();
        try {
            // 业务逻辑代码.....
            Thread.sleep(1000);
            System.out.println("业务代码执行完毕....");
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            // 释放锁
            lock.unlock();
        }

    }

}
