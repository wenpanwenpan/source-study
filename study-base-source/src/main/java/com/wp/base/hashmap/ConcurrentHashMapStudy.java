package com.wp.base.hashmap;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingDeque;

/**
 *
 * @author wenpan
 *  @create  2021/06/19 15:49
 */
public class ConcurrentHashMapStudy {

    public static void main(String[] args) throws InterruptedException {
        final ConcurrentHashMap<String, String> ma = new ConcurrentHashMap<>();
        int i = (1<<30);
        int j = i>>>1;
        System.out.println("i = " + i);
        System.out.println("j = " + j);
        System.out.println(0x7fffffff);


        System.out.println((1<<29)-1);

        System.out.println(-1<< 29);
        System.out.println(3<< 29);

        final LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
        new ArrayBlockingQueue<String>(1);
        new CopyOnWriteArrayList<String>();
    }
}
