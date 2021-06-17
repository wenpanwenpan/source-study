package com.wp.thread.service.impl;

import com.wp.thread.entity.Blog;
import com.wp.thread.mapper.BlogMapper;
import com.wp.thread.service.BlogInsertService2;
import org.springframework.stereotype.Service;

/**
 * @description:
 * @author: 文攀 Mr_wenpan@163.com
 * @date: 2020-12-02 19:40
 **/
@Service
public class BlogInsertService2Impl implements BlogInsertService2 {

	@Override
	public Integer call(Blog blog, BlogMapper blogMapper){

		try {
			System.out.println(blog.getUsername() + " 线程名称： " + Thread.currentThread().getName());
			blogMapper.insert(blog);
			// 手动制造异常，看是否不插入数据
			Integer.parseInt("aa");
			return 1;
		}catch (Exception e){
			e.printStackTrace();
			System.out.println(Thread.currentThread().getName()+" failure");
		}
		return 99;
	}
}
