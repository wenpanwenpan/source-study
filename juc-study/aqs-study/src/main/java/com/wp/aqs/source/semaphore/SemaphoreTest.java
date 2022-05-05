package com.wp.aqs.source.semaphore;

import java.util.concurrent.Semaphore;

/**
 *
 * @author wenpan
 *  @create  2021/06/17 11:27
 */
public class SemaphoreTest {

    public static void main(String[] args) throws InterruptedException {
        final Semaphore semaphore = new Semaphore(4);
        // 获取许可
        semaphore.acquire();

        // 释放许可
        semaphore.release();
    }
}
