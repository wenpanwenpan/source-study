package com.wp.thread.service;

import com.wp.thread.entity.Blog;

import java.util.List;

public interface BlogService {

	/**
	 * 测试多线程往数据库插数据
	 * @return
	 * @throws Exception
	 */
	Boolean batchAdd() throws Exception;

	/**
	 * 通过id查找blog
	 * @return
	 */
	List<Blog> selectBlogById(Blog blog);
}
