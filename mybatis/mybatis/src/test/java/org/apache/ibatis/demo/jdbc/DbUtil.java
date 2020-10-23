package org.apache.ibatis.demo.jdbc;
/**
 * 源码学院-Monkey
 * 只为培养BAT程序员而生
 * http://bat.ke.qq.com
 * 往期视频加群:516212256 暗号:6
 */

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbUtil {

    /*
     * 打开数据库
     */
    private static String driver;//连接数据库的驱动
    private static String url;
    private static String username;
    private static String password;

    static {
      driver="com.mysql.jdbc.Driver";//需要的数据库驱动
      url="jdbc:mysql://localhost:3306/mybatis";//数据库名路径
      username="root";
      password="123456";
    }
    public static Connection open()
    {
      try {
        Class.forName(driver);
        return (Connection) DriverManager.getConnection(url,username, password);
      } catch (Exception e) {
        System.out.println("数据库连接失败！");
        e.printStackTrace();
      }//加载驱动

      return null;
    }

    /*
     * 关闭数据库
     */
    public static void close(Connection conn)
    {
      if(conn!=null)
      {
        try {
          conn.close();
        } catch (SQLException e) {
          e.printStackTrace();
        }
      }
    }


  }
