package com.wp.thread.service;


import com.wp.thread.entity.Blog;
import com.wp.thread.mapper.BlogMapper;

public interface BlogInsertService1 {

	/**
	 * 插入Blog记录
	 * @param blog
	 * @param blogMapper
	 * @return
	 */
	Integer call(Blog blog, BlogMapper blogMapper);
}
