package thread2.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @description:
 * @author: 文攀 Mr_wenpan@163.com
 * @date: 2020-12-06 22:40
 **/
public class ThreadPoolTool {

	private static final Logger log = LoggerFactory.getLogger(ThreadPoolTool.class);

	/**
	 * 多线程任务
	 * @param transactionManager 事务管理器
	 * @param data 要操作的数据
	 * @param threadCount 线程数量
	 * @param params
	 * @param clazz
	 */
	public void excuteTask(DataSourceTransactionManager transactionManager,
	                       List data, int threadCount, Map params, Class clazz) {

		if (data == null || data.size() == 0) {
			return;
		}

		int batch = 0;

		// 创建一个线程池【这里可以注入】
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		//监控子线程的任务执行
		CountDownLatch childMonitor = new CountDownLatch(threadCount);
		//监控主线程，是否需要回滚
		CountDownLatch mainMonitor = new CountDownLatch(1);
		//存储任务的返回结果，返回true表示不需要回滚，反之，则回滚
		BlockingDeque<Boolean> results = new LinkedBlockingDeque(threadCount);

		RollBack rollback = new RollBack(false);

		try {
			LinkedBlockingQueue<List> queue = splitQueue(data, threadCount);

			while (true) {
				// 从队列里取出一个元素
				List list = queue.poll();
				if (list == null) {
					break;
				}
				batch++;
				params.put("batch", batch);
				Constructor constructor = clazz.getConstructor(new Class[]{CountDownLatch.class,
																			CountDownLatch.class, BlockingDeque.class,
																			RollBack.class, DataSourceTransactionManager.class,
																			Object.class, Map.class});
				ThreadTask task = (ThreadTask) constructor.newInstance(childMonitor, mainMonitor, results, rollback, transactionManager, list, params);
				// 扔进线程池执行
				executor.submit(task);
				// executor.execute(task);
			}

			//  1、主线程将任务分发给子线程，然后使用childMonitor.await();阻塞主线程，等待所有子线程处理向数据库中插入的业务。           
			childMonitor.await();

			System.out.println("主线程开始执行检查任务......");

			//根据返回结果来确定是否回滚
			for (int i = 0; i < threadCount; i++) {
				Boolean result = results.take();
				if (!result) {
					//有线程执行异常，需要回滚子线程
					rollback.setNeedRoolBack(true);
				}
			}

			//  3、主线程检查子线程执行任务的结果，若有失败结果出现，主线程标记状态告知子线程回滚，
			// 然后使用mainMonitor.countDown();将程序控制权再次交给子线程，子线程检测回滚标志，判断是否回滚。           
			// 在执行该句之前出现了异常，导致子线程不被唤醒，子线程一直阻塞？？？？
			mainMonitor.countDown();

		} catch (Exception e) {
			log.error(e.getMessage());
		} finally {
			//关闭线程池，释放资源           
			executor.shutdown();
		}
	}


	/**
	 * @param data
	 * @param threadCount
	 * @return
	 */
	private LinkedBlockingQueue splitQueue(List data, int threadCount) {

		LinkedBlockingQueue queueBatch = new LinkedBlockingQueue();
		int total = data.size();
		int oneSize = total / threadCount;
		int start;
		int end;

		for (int i = 0; i < threadCount; i++) {
			start = i * oneSize;
			end = (i + 1) * oneSize;
			if (i < threadCount - 1) {
				queueBatch.add(data.subList(start, end));
			} else {
				queueBatch.add(data.subList(start, data.size()));
			}
		}

		return queueBatch;
	}

}

