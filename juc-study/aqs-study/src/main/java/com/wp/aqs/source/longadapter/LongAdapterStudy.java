package com.wp.aqs.source.longadapter;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 *   LongAdapter学习
 * @author wenpan
 *  @create  2021/04/19 22:33
 */
public class LongAdapterStudy {

    static volatile Long COUNT = 1L;

    public static void main(final String[] args) throws InterruptedException {

        LongAdder longAdder = new LongAdder();
        // longAdder.add(COUNT);
        CountDownLatch latch = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {

            new Thread(()->{
                try {
                    TimeUnit.SECONDS.sleep(1);
                    for (int j = 0; j < 10000; j++) {
                        longAdder.increment();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }finally {
                    latch.countDown();
                }
            }).start();
        }


        /*new Thread(()->{
            try {
                TimeUnit.SECONDS.sleep(1);
                for (int i = 0; i < 1000; i++) {
                    longAdder.increment();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }finally {
                latch.countDown();
            }
        }).start();

        new Thread(()->{
            try {
                TimeUnit.MICROSECONDS.sleep(100);
                for (int i = 0; i < 2000; i++) {
                    longAdder.increment();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }finally {
                latch.countDown();
            }
        }).start();*/

        latch.await();
        long value = longAdder.longValue();
        System.out.println(value);
        long sum = longAdder.sum();
        System.out.println(sum);

        // 增加10
        longAdder.add(10);
        // 自增一
        longAdder.increment();
        // 自减一
        longAdder.decrement();
        // 获取longAdder的值（方法内部也是调用的sum方法）
        longAdder.longValue();
        // 求和： 即 base + 每个cell的值
        longAdder.sum();
        // 将long值变为string
        longAdder.toString();

        // 重置 longAdapter 的每个 cell ，将每个cell的值变为 0
        longAdder.reset();
        // 先求出 longAdapter的值然后重置每个cell
        longAdder.sumThenReset();

        // 将longAdapter的值强转为 int 类型
        longAdder.intValue();
        // 将longAdapter的值强转为 double 类型
        longAdder.doubleValue();
        // 将longAdapter的值强转为 float 类型
        longAdder.floatValue();
    }
}
