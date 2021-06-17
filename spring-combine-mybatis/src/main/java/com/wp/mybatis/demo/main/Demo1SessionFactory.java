package com.wp.mybatis.demo.main;

import com.wp.mybatis.demo.entity.Blog;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;

public class Demo1SessionFactory {

    /**
     * 直接调用session API执行CRUD测试
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
      // 全局配置
      String resource = "mybatis-config.xml";
      InputStream inputStream = Resources.getResourceAsStream(resource);
      // 创建session工厂
      SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
      // 获取一个SqlSession，session中保存了配置信息，事务、执行器、建立一个数据库连接等
      SqlSession session = sqlSessionFactory.openSession();
      // 通过SqlSession执行操作
      Blog blog = session.selectOne("org.apache.ibatis.demo.mapper.BlogMapper.selectBlog", 1);
      System.out.println("blog: "+blog);
      session.selectList("org.apache.ibatis.demo.mapper.BlogMapper.selectBlog",1);
    }
}
