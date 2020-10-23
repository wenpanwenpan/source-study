package com.wp.mybatis.demo.main;

import com.wp.mybatis.demo.entity.Blog;
import com.wp.mybatis.demo.mapper.MyBlogMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;

/**
 * @description:
 * @author: 文攀 Mr_wenpan@163.com
 * @date: 2020-10-18 12:28
 **/
public class TestMain {

  /**
   * 使用动态代理对象执行CRUD
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {
    // 通过字节流读取配置文件
    InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
    // 创建SqlSessionFacory，这步会解析xml配置文件
    SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(inputStream);
    // 获取sqlSession
    SqlSession sqlSession = factory.openSession();
    //这里不再调用SqlSession 的api，而是获得了接口对象，调用接口中的方法。
    MyBlogMapper mapper = sqlSession.getMapper(MyBlogMapper.class);
    Blog blog = mapper.selectBlog(1);
    System.out.println("blog:"+blog);
  }
}
