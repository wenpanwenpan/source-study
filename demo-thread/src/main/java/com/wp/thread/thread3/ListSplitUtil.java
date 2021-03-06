package com.wp.thread.thread3;

import java.util.ArrayList;
import java.util.List;

/**
 * @description: List集合拆分工具
 * @author: 文攀 Mr_wenpan@163.com
 * @date: 2020-12-03 17:22
 **/
public class ListSplitUtil {

	/**
	 * 将一个list均分成n个list,主要通过偏移量来实现的
	 *
	 * @param source
	 * @param subListCount 要均分成多少个子集合
	 * @return
	 */
	public static <T> List<List<T>> averageAssign(final List<T> source, final int subListCount) {

		final List<List<T>> result = new ArrayList<List<T>>();
		// (先计算出余数)
		int remaider = source.size() % subListCount;
		// 然后是商
		final int number = source.size() / subListCount;
		// 偏移量
		int offset = 0;
		for (int i = 0; i < subListCount; i++) {
			final List<T> value;
			if (remaider > 0) {
				value = source.subList(i * number + offset, (i + 1) * number + offset + 1);
				remaider--;
				offset++;
			} else {
				value = source.subList(i * number + offset, (i + 1) * number + offset);
			}
			result.add(value);
		}
		return result;
	}

	/**
	 * 根据指定的集合的数据量大小来切分List
	 * @param sourceList 总共要处理的数据集合
	 * @param perListSize 每个线程要处理的数据集合大小
	 * @return 子List集合
	 * @author panfeng.wen
	 * @description 按指定大小，分隔集合List，将集合按规定个数分为n个部分
	 */
	public static <T> List<List<T>> splitList(final List<T> sourceList, final int perListSize) {
		if (sourceList == null || sourceList.size() == 0 || perListSize < 1) {
			return null;
		}

		final List<List<T>> result = new ArrayList<List<T>>();

		final int size = sourceList.size();
		// 得到要创建的线程数量
		final int count = (size + perListSize - 1) / perListSize;

		for (int i = 0; i < count; i++) {
			final List<T> subList = sourceList.subList(i * perListSize, ((i + 1) * perListSize > size ? size : perListSize * (i + 1)));
			result.add(subList);
		}
		return result;
	}
}
