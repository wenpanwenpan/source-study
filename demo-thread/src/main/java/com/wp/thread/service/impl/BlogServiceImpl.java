package com.wp.thread.service.impl;

import com.wp.thread.config.MyExecutor;
import com.wp.thread.config.SqlSessionContext;
import com.wp.thread.entity.Blog;
import com.wp.thread.mapper.BlogMapper;
import com.wp.thread.service.BlogInsertService1;
import com.wp.thread.service.BlogInsertService2;
import com.wp.thread.service.BlogService;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * @description:
 * @author: 文攀 Mr_wenpan@163.com
 * @date: 2020-12-02 19:26
 **/
@Service
public class BlogServiceImpl implements BlogService {

	@Autowired
	private SqlSessionContext sqlSessionContext;
	@Autowired
	private MyExecutor myExecutor;
	@Autowired
	private BlogInsertService1 blogInsertService1;
	@Autowired
	private BlogInsertService2 blogInsertService2;
	@Autowired
	private BlogMapper blogMapper;
	/**
	 * 直接使用spring容器中获取SqlSession
	 */
	/* @Autowired
	 private SqlSession sqlSession;
	 */

	/**
	 * 多线程插入数据库blog记录
	 * @return
	 * @throws Exception
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public Boolean batchAdd() throws Exception {

		boolean success = true;

		// 获取执行器
		ExecutorService executorService = myExecutor.getThreadPool();

		// 构建对象信息
		Blog blog1 = buildLog("wenpan1");
		Blog blog2 = buildLog("wenpan2");
		Blog blog3 = buildLog("wenpan3");

		List<Callable<Integer>> callableList = new ArrayList<>();
		this.blogMapper.insert(blog3);

		// 获取数据库连接（内部创建了自有事务）
		SqlSession sqlSession = sqlSessionContext.getSqlSession();
		Connection connection = sqlSession.getConnection();
		// 设置手动提交
		connection.setAutoCommit(false);
		BlogMapper blogMapper = sqlSession.getMapper(BlogMapper.class);

		try {
			// 多线程调用
			callableList.add(()->blogInsertService1.call(blog1,blogMapper));
			callableList.add(()->blogInsertService2.call(blog2,blogMapper));
			// 使用线程池执行多个线程
			List<Future<Integer>> futures = executorService.invokeAll(callableList);

			// 统计执行结果
			int num = 0;
			for (Future<Integer> future : futures) {
				num += future.get();
			}
			// 所有线程执行成功则=2，直接提交，否则回滚（判断是否达到我们的预期）
			if (num == 2){
				connection.commit();
			}else {
				success = false;
				connection.rollback();
			}
		}catch (Exception e){
			e.printStackTrace();
			connection.rollback();
			success = false;
		}finally {

			/*if(connection != null){
				System.out.println("============关闭连接。。。。。。");
				connection.close();
			}*/

			if(sqlSession != null){
				System.out.println("============sqlSession。。。。。。");
				sqlSession.close();
			}
		}

		// 制造异常
		// int i = 1 / 0;

		return success;
	}

	@Override
	public List<Blog> selectBlogById(final Blog blog) {
		List<Blog> blogList = this.blogMapper.selectBlogById(blog);
		return blogList;
	}

	/**
	 * 构建Log对象
	 * @param username
	 */
	private Blog buildLog(String username){
		Blog blog = new Blog();
		blog.setUsername(username);
		return blog;
	}
}
