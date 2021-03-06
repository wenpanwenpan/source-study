package com.wp.thread.thread3;

import org.junit.platform.commons.function.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * @description: 带事务的多线程工具类
 * @author: 文攀 Mr_wenpan@163.com
 * @date: 2020-12-07 13:48
 **/
public class TransactionThreadUtil {

	private static final Logger LOG = LoggerFactory.getLogger(TransactionThreadUtil.class);
	private static final String SUCCESS = "success";

	/**
	 * 开启多线程
	 * @param transactionManager 事务管理器
	 * @param dataList 要使用多线程处理的数据集合
	 * @param maxThread 最大线程数量（设置为）
	 * @param threadSize 每个线程处理的数据量
	 * @param function 函数接口
	 * @param <T> 参数
	 * @param <R> 返回值
	 * @return
	 */
	public static <T, R> List<R> excuteTask(final DataSourceTransactionManager transactionManager,
	                                        DefaultTransactionDefinition def,
	                                        final ThreadPoolTaskExecutor taskPool,
	                                        final List<T> dataList,
	                                        final int maxThread,
	                                        final int threadSize,
	                                        final Function<List<T>, List<R>> function) {

		// ==================================================================================
		// 先按照每个线程处理的数量进行拆分，如果拆分出来的要开启的子线程数量大于核心线程数量
		// 则按照 【核心线程数量-1】重新拆分，注意： 核心线程数量必须大于 1
		// ==================================================================================
		// 要创建的线程数量
		int count;
		List<List<T>> splitList;
		List<R> resDataList = new ArrayList<>();
		// 线程池中的核心线程数量
		int corePoolSize = taskPool.getCorePoolSize();

		if (dataList == null || dataList.size() == 0 || threadSize < 1) {
			count = 0;
		} else {
			final int size = dataList.size();
			// 得到要创建的线程数量
			count = (size + threadSize - 1) / threadSize;
		}

		// 如果拆分出来的集合大于核心线程数量，就按照 【核心线程数量-1】重新拆分
		if(count > corePoolSize){
			splitList = ListSplitUtil.averageAssign(dataList, corePoolSize - 1);
		} else {
			// 将List集合进行拆分切片
			splitList = ListSplitUtil.splitList(dataList, threadSize);
		}

		if (CollectionUtils.isEmpty(splitList)) {
			LOG.error("request data is null return null");
			return null;
		}

		// todo 子线程数量 【经测试，子线程的数量一定要小于线程池的核心线程数量，核心线程数量要小于cpu核数】
		int threadNum = splitList.size();
		if (threadNum == 1) {
			resDataList = function.apply(dataList);
		} else {

			// 多线程分配策略
			final AtomicInteger taskCounter = new AtomicInteger(0);
			// 监控子线程的任务执行
			CountDownLatch childMonitor = new CountDownLatch(threadNum);
			// 存储子线程任务的返回结果，返回true表示不需要回滚，反之，则回滚
			BlockingDeque<Boolean> subThreadResultQueue = new LinkedBlockingDeque(threadNum);
			// 监控主线程，是否需要回滚
			CountDownLatch mainMonitor = new CountDownLatch(1);
			// 是否需要回滚(默认需要回滚事务，防止服务突然挂掉的情况事务不回滚的情况)
			RollBack rollback = new RollBack(true);

			int ii = 0;
			for (List<T> currentBatchProcess : splitList) {
				// 创建多线程
				taskRunner(ii,def,transactionManager,subThreadResultQueue,rollback,childMonitor,
						mainMonitor,currentBatchProcess,maxThread,taskPool,taskCounter,resDataList,function);
				ii++;
			}

			//  1、主线程将任务分发给子线程，然后使用childMonitor.await();阻塞主线程，等待所有子线程处理向数据库中插入的业务
			try {
				childMonitor.await();
			} catch (InterruptedException e) {
				throw new RuntimeException("中断异常.......");
			}

			LOG.info("parent thread : {} start execute check sub thread run status.",Thread.currentThread().getName());

			// 判断是否有子线程需要回滚的（默认没有）
			boolean needRollbackFlag = false;

			// todo 根据返回结果和个数一致来确定是否回滚【待测试】
			if(subThreadResultQueue.size() != threadNum || subThreadResultQueue.contains(true)){
				needRollbackFlag = true;
			}

			if(!needRollbackFlag){
				rollback.setNeedRoolBack(false);
			}

			//  3、主线程检查子线程执行任务的结果，若有失败结果出现，主线程标记状态告知子线程回滚，
			// 然后使用mainMonitor.countDown();将程序控制权再次交给子线程，子线程检测回滚标志，判断是否回滚。           
			// 在执行该句之前出现了异常，导致子线程不被唤醒，子线程一直阻塞？？？？
			mainMonitor.countDown();

		}

		return resDataList;
	}

	/**
	 * 执行任务
	 * @param transactionManager
	 * @param blockingDeque 存储线程是否需要返回（即每一个子线程是否正常执行完毕）
	 * @param rollback 是否需要回滚标识
	 * @param childMonitor 子线程闭锁监控
	 * @param mainMonitor 主线程闭锁监控
	 * @param preProcessDatas 子线程要处理的数据List
	 * @param taskTotal 任务总数
	 * @param taskPool 线程池，用于提交执行线程
	 * @param taskCounter 任务计数器
	 * @param resDataList 返回结果集
	 * @param function
	 * @param <T>
	 * @param <R>
	 * @return
	 */
	public static <T, R> Future<String> taskRunner(int i,
	                                               DefaultTransactionDefinition def,
	                                               final DataSourceTransactionManager transactionManager,
	                                               final BlockingDeque<Boolean> blockingDeque,
	                                               final RollBack rollback,
	                                               final CountDownLatch childMonitor,
	                                               final CountDownLatch mainMonitor,
	                                               final List<T> preProcessDatas,
	                                               final int taskTotal,
	                                               final ThreadPoolTaskExecutor taskPool,
	                                               final AtomicInteger taskCounter,
	                                               final List<R> resDataList,
	                                               final Function<List<T>, List<R>> function) {

		final BaseTenantAwareCallable<String> taskRunner = new BaseTenantAwareCallable<String>() {

			@Override
			protected String performActualWork() {
				def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
				TransactionStatus status = transactionManager.getTransaction(def);
				taskCounter.incrementAndGet();
				try {
					LOG.info("SourceThreadUtil start new thread,taskRunner process ({}) records.", preProcessDatas.size());
					if(!CollectionUtils.isEmpty(preProcessDatas)){
						// 执行业务方法
						resDataList.addAll(function.apply(preProcessDatas));
						if(i==1){
							int k  = 1 / 0;
						}
						System.out.println("=====>>>>i = " + i);
						blockingDeque.add(false);
					}
				} catch (final Exception e) {
					// 发生异常需要回滚，这里自己将异常捕获不抛出，并且设置回滚队列
					blockingDeque.add(true);
					LOG.error("SourceThreadUtil taskRunner erroroccured: {}", e.getMessage());
				} finally {
					// 子线程门闩减一
					childMonitor.countDown();
					// 当前线程执行完毕，将数量减一
					taskCounter.decrementAndGet();
				}

				try {
					// 等待主线程的判断逻辑执行完，执行下面的是否回滚逻辑           
					mainMonitor.await();
				} catch (Exception e) {
					// 如果被系统中断，代表mainMonitor已经被唤醒
					LOG.error(e.getMessage());
				}

				LOG.info("sub thread : {} start execute commit or rollback!", Thread.currentThread().getName());

				//3、主线程检查子线程执行任务的结果，若有失败结果出现，主线程标记状态告知子线程回滚，
				// 然后使用mainMonitor.countDown();将程序控制权再次交给子线程，子线程检测回滚标志，判断是否回滚。
				if (rollback.isNeedRoolBack()) {
					System.out.println(Thread.currentThread().getName() + "==============>>>>>>>>>>>>>>>>>>开始回滚事务......");
					try {
						transactionManager.rollback(status);
					} catch( Exception e) {
						LOG.error("TransactionThreadUtil.taskRunner() rollback error {}", e.getMessage());
					}
					System.out.println(Thread.currentThread().getName() + "==============>>>>>>>>>>>>>>>>>>结束回滚事务......");
				} else {
					System.out.println(Thread.currentThread().getName() + "==============>>>>>>>>>>>>>>>>>>开始提交事务......");
					//事务提交           
					transactionManager.commit(status);
					System.out.println(Thread.currentThread().getName() + "==============>>>>>>>>>>>>>>>>>>结束提交事务......");
				}

				return SUCCESS;
			}

		};

		while (true) {
			if (taskCounter.get() < taskTotal) {
				return taskPool.submit(taskRunner);
			}
		}

	}
}
