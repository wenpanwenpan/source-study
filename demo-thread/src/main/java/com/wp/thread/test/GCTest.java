package com.wp.thread.test;

/**
 * description
 *
 * @author jiancheng.ma 2020/12/21 14:03
 */
public class GCTest {
    private static final int _2MB = 2 * 1024 * 1024;
    public static void main(String[] args) {
        byte[] bytes,bytes2,bytes4,bytes3,bytes5,bytes6,bytes7;
        bytes = new byte[1024*1024*1];
        bytes2 = new byte[1024*1024*4];
        // 此处 进行 Minor Gc 后  为什么会再进行Full GC
        bytes4 = new byte[1024*1024*1];
        bytes3 = new byte[1024*1024*1];
        // -XX:+UseParNewGC
//        bytes4 = new byte[1024*1024*1];
//        bytes4 = null;
//        bytes5 = new byte[1024*1024*2];
//        bytes5 = null;
//        bytes6 = new byte[1024*1024*1];
        bytes6 = null;
//        bytes7 = new byte[1024*1024*1];

//        bytes4 = null;
//        bytes4 = new byte[1024*1024*4];
//        byte[] bytes6 = new byte[1024*1024*4];
//        byte[] bytes5 = new byte[1024*1024*1];
    }

}
