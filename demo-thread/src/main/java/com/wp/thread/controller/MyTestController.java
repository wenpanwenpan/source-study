package com.wp.thread.controller;

import com.wp.thread.entity.Blog;
import com.wp.thread.service.BlogService;
import com.wp.thread.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @description:
 * @author: 文攀 Mr_wenpan@163.com
 * @date: 2020-12-03 22:13
 **/
@RestController("MyTestController.v1")
@RequestMapping({"/test"})
public class MyTestController {

	@Autowired
	private BlogService blogService;
	@Autowired
	private TestService testService;

	@GetMapping("/get-blog")
	public List<Blog> selectBlogById(Blog blog){
		return this.blogService.selectBlogById(blog);
	}

	@GetMapping("/start")
	public String start(){
		return "success.........";
	}

	/*@GetMapping("/test")
	public void test(){
		testService.multiInsert1();
	}*/

	@GetMapping("/start-insert")
	public String insertTractionAndSqlsession() throws Exception {
		blogService.batchAdd();
		return "start-insert.........";
	}
}
