package com.wp.mybatis.demo.test;

import com.wp.mybatis.demo.entity.Blog;
import com.wp.mybatis.demo.mapper.MyBlogMapper;
import org.apache.ibatis.session.SqlSession;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Arrays;
import java.util.List;

/**
 * @description: Spring整合Mybatis测试
 * @author: 文攀 Mr_wenpan@163.com
 * @date: 2020-10-22 11:46
 **/
public class SpringCombineMybatisTest {

	ApplicationContext applicationContext;

	@Before
	public void init(){
		applicationContext = new ClassPathXmlApplicationContext("spring-config.xml");
	}

	/**
	 * spring整合mybatis源码测试
	 */
	@Test
	public void test(){
		MyBlogMapper mapper = (MyBlogMapper)applicationContext.getBean("myBlogMapper");
		Blog blog = mapper.selectBlog(1);
		System.out.println(blog);
		Blog blog1 = mapper.selectBlog(1);

		// 动态代理注入源对象
		SqlSession bean = applicationContext.getBean(SqlSession.class);
		System.out.println("bean: "+ bean);
		String[] beanNamesForType = applicationContext.getBeanNamesForType(SqlSession.class);
		List<String> list = Arrays.asList(beanNamesForType);
		list.stream().forEach(item -> System.out.println(item));
	}
}
