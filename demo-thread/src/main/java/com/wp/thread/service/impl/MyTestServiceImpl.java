package com.wp.thread.service.impl;

import com.wp.thread.config.MyExecutor;
import com.wp.thread.config.SqlSessionContext;
import com.wp.thread.entity.Blog;
import com.wp.thread.mapper.BlogMapper;
import com.wp.thread.service.BlogInsertService1;
import com.wp.thread.service.BlogInsertService2;
import com.wp.thread.service.MyTestService;
import com.wp.thread.thread3.TransactionThreadUtil;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @description:
 * @author: 文攀 Mr_wenpan@163.com
 * @date: 2020-12-03 10:01
 **/
@Service
public class MyTestServiceImpl implements MyTestService {

	@Autowired
	private BlogMapper blogMapper;
	@Autowired
	private ThreadPoolTaskExecutor executor;

	@Autowired
	private DataSourceTransactionManager dataSourceTransactionManager;
	@Autowired
	private DefaultTransactionDefinition transactionDefinition;

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void multiInsert()  {

		// 做一系列操作

		// 开启多个线程，在其中一个线程制造异常
		FutureTask<Integer> futureTask1 = new FutureTask<>(() -> {
			System.out.println("执行线程名称： " + Thread.currentThread().getName());
			Blog blog = new Blog();
			blog.setUsername("wenpan11");
			blogMapper.insert(blog);
			return 1;
		});
		FutureTask<Integer> futureTask2 = new FutureTask<>(() -> {
			try {
				System.out.println("执行线程名称： " + Thread.currentThread().getName());
				Blog blog = new Blog();
				blog.setUsername("wenpan22");
				blogMapper.insert(blog);
				// 手动制造异常
				int i = 1/0;

			}catch (Exception e){
				throw new RuntimeException("子线程发生异常啦。。。。。");
			}
			return 1;
		});

		// 手动创建线程
		new Thread(futureTask1, "threadA: ").start();
		new Thread(futureTask2, "threadB: ").start();

		try {
			Integer i = futureTask1.get();
			// 如果该处发生异常，主线程在这里中断
			Integer j = futureTask2.get();
			System.out.println("i = " + i + " j = " + j);

			// 即使这里触发异常，数据库数据也不会回滚
			if(i != 2 || j != 1){
				throw new RuntimeException("手动触发异常，回滚事务！");
			}
		}catch (Exception e){
			throw new RuntimeException("手动触发异常，回滚事务........");
		}


	}


	// @Transactional(rollbackFor = Exception.class)
	@Override
	public void multiInsert1()  {

		// 造数据
		List<Blog> dataList = new ArrayList<>();

		for (int i = 0; i < 10; i++) {
			Blog blog = new Blog();
			blog.setUsername("wenpan--" + i);
			dataList.add(blog);
		}

		TransactionThreadUtil.excuteTask(dataSourceTransactionManager,transactionDefinition,executor,dataList,8,5,function->{
			for (final Blog blog : function) {
				blogMapper.insert(blog);
			}
			return function;
		});

	}

}
