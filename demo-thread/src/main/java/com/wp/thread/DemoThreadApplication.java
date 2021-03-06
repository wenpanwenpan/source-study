package com.wp.thread;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.wp.thread.mapper")
public class DemoThreadApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoThreadApplication.class, args);
	}

}
