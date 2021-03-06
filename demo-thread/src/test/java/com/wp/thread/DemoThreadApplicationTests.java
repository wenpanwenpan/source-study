package com.wp.thread;

import com.wp.thread.service.BlogService;
import com.wp.thread.service.MyTestService;
import com.wp.thread.service.TestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DemoThreadApplicationTests {

	@Autowired
	private BlogService blogService;
	@Autowired
	private MyTestService myTestService;
	@Autowired
	private TestService testService;

	@Test
	void contextLoads() {}

	/*@Test
	public void test() throws Exception{
		blogService.batchAdd();
	}


	@Test
	public void test1() throws Exception{
		myTestService.multiInsert();
	}*/

	@Test
	public void test2() throws Exception{
		myTestService.multiInsert1();
	}

	@Test
	public void test3() throws Exception{
		testService.multiInsert1();
	}

}
