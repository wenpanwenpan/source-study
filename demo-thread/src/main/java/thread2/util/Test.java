package thread2.util;

import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 *  @author  文攀 Mr_wenpan@163.com
 * @date: 2020-12-06 23:16
 **/
public class Test {

	/**
	 * 执行多线程任务方法
	 * */
	public void testThreadTask() {

		ThreadPoolTool threadPoolTool = new ThreadPoolTool();
		DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();
		try {
			int threadCount = 5;
			//需要分批处理的数据
			List objectList =new ArrayList<>();
			Map params =new HashMap<>();
			params.put("objectList",objectList);
			params.put("testService","testService");
			//调用多线程工具方法
			threadPoolTool.excuteTask(transactionManager,objectList,threadCount,params, Test.class);
		}catch (Exception e){
			throw new RuntimeException(e.getMessage());
		}
	}
}
