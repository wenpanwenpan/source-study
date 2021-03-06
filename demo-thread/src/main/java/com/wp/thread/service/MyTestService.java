package com.wp.thread.service;

import java.util.concurrent.ExecutionException;

public interface MyTestService {

	void multiInsert() throws ExecutionException, InterruptedException;
	void multiInsert1() throws ExecutionException, InterruptedException;

}
