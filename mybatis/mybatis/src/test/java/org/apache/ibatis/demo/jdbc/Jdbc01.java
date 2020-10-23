package org.apache.ibatis.demo.jdbc;
/**
 * 源码学院-Monkey
 * 只为培养BAT程序员而生
 * http://bat.ke.qq.com
 * 往期视频加群:516212256 暗号:6
 */


import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/***
 * 非对象
 */
public class Jdbc01 {

  public static void main(String[] args) {

    insert("Monkey",18);
  }

  static void insert(String name,int age)
  {
    String sql="insert into user(username,age) value(?,?)";
    Connection conn= DbUtil.open();
    try {
      PreparedStatement pstmt=(PreparedStatement) conn.prepareStatement(sql);
      pstmt.setString(1,name);
      pstmt.setInt(2,age);
      pstmt.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    finally {
      DbUtil.close(conn);
    }

  }


}
