package com.wp.thread.service.impl;

import com.wp.thread.entity.Blog;
import com.wp.thread.mapper.BlogMapper;
import com.wp.thread.service.TestService;
import com.wp.thread.thread3.TransactionThreadUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 *
 *  @author  文攀 Mr_wenpan@163.com
 * @date: 2020-12-07 19:51
 **/
@Service
public class TestServiceImpl implements TestService {

	@Autowired
	private BlogMapper blogMapper;
	@Autowired
	private ThreadPoolTaskExecutor executor;

	@Autowired
	private DataSourceTransactionManager dataSourceTransactionManager;
	@Autowired
	private DefaultTransactionDefinition transactionDefinition;

	@Override
	public void multiInsert1()  {

		// 造数据
		List<Blog> dataList = new ArrayList<>();

		for (int i = 0; i < 10; i++) {
			Blog blog = new Blog();
			blog.setUsername("wenpan--" + i);
			dataList.add(blog);
		}

		TransactionThreadUtil.excuteTask(dataSourceTransactionManager, transactionDefinition, executor,dataList,8,1, function->{
			for (final Blog blog : function) {
				blogMapper.insert(blog);
			}
			return function;
		});

	}
}
