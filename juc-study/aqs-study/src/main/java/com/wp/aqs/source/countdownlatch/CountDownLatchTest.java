package com.wp.aqs.source.countdownlatch;

import java.util.concurrent.CountDownLatch;

/**
 * @description:
 * @author: panfeng.wen@hand-china
 * @create: 2021/06/17 14:11
 */
public class CountDownLatchTest {

    public static void main(String[] args) throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(5);
        countDownLatch.await();
        countDownLatch.countDown();
    }
}
