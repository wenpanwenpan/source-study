package bat.ke.qq.com.excutor;

import bat.ke.qq.com.pojo.Blog;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.submitted.basetest.User;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ExecutorTest {

  public static void main(String[] args) throws IOException {

    String resource = "mybatis-config.xml";//全局配置
    InputStream inputStream = Resources.getResourceAsStream(resource);
    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    SqlSession session = sqlSessionFactory.openSession();
    for (int i=0;i<100;i++){
      session.insert("org.mybatis.example.BlogMapper.insertBlog",  new Blog(i,"monkey","monkey"+i));

    }
    session.commit();




  }
}
