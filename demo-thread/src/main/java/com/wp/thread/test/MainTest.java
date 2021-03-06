package com.wp.thread.test;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @description:
 * @author: 文攀 Mr_wenpan@163.com
 * @date: 2020-12-07 16:36
 **/
public class MainTest {

	public static void main(String[] args) {
		for (int i = 0; i < 10; i++) {
			try {
				if(i == 5){
					int j = 1/0;
					// 出现异常后不会继续走try下面的东西了
					System.out.println("hahahahhahaha");
				}

			}catch (Exception ex){
				ex.printStackTrace();
				System.out.println("出现异常啦，i = " + i);
				// throw new RuntimeException("XXXXXX");
			}
			// 但是走完catch以后会继续走这里。。。。,一旦在catch里使用throw抛出异常后，就不会走下面的了，
			// 会直接终止程序
			System.out.println("=========: " + i);
		}

		BlockingDeque<Boolean> blockingDeque = new LinkedBlockingDeque<Boolean>();
		blockingDeque.add(false);
		blockingDeque.add(false);
		blockingDeque.add(true);
		blockingDeque.add(false);
		blockingDeque.add(false);

		System.out.println("blockingDeque.size(): " + blockingDeque.size());
		System.out.println("blockingDeque.contains(): " + blockingDeque.contains(true));

	}
	
}
