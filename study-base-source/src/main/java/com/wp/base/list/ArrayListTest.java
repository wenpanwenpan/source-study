package com.wp.base.list;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * arrayList测试
 * transient 修饰变量表示该变量不能直接被序列化
 *
 * @author Mr_wenpan@163.com 2022/05/01 16:01
 */
public class ArrayListTest {

    public static void main(String[] args) {
        createArrayListTest();
        test1();
    }

    /**
     * 错误的使用ArrayList的构造函数导致频繁扩容问题
     * ArrayList容量初始值为0的情况一般用于只有极少个元素的的情况，因为可以避免过多的空间浪费（默认初始容量是10）
     */
    private static void test1(){
        // 给个初始容量0
        ArrayList<String> list = new ArrayList<>(0);
        // 请问扩容了多少次
        list.add("a");
        list.add("b");
        list.add("c");
        list.add("d");
        list.add("e");
        list.add("f");
        list.add("g");
        list.add("h");
        list.add("i");
        list.add("j");
    }

    /**测试创建ArrayList的几种方式（常见错误）*/
    private static void createArrayListTest(){
        // 通过Collections来创建一个空list，这种在开发中很常用，很容易踩坑
        List<String> list = Collections.emptyList();
        // 会报错
        list.add("hello");

        // 通过Arrays来创建ArrayList
        // 一般只有list元素个数不再改变的情况才使用Arrays.asList();，其他情况一般不要使用
        // 一旦list创建后就不允许再往list里增减数据了
        List<String> list1 = Arrays.asList();
        // 会报错，因为他new的是自己的实现的ArrayList，add方法没有实现
        list1.add("wenpan");
    }
}
