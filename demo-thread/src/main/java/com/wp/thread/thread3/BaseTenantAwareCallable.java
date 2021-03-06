package com.wp.thread.thread3;

import java.util.concurrent.Callable;

/**
 * @description: 抽取基础线程实现类
 * @author: 文攀 Mr_wenpan@163.com
 * @date: 2020-12-03 17:19
 **/
public abstract class BaseTenantAwareCallable<T> implements Callable<T> {

	@Override
	public T call() throws Exception {
		return this.performActualWork();
	}

	protected abstract T performActualWork();
}