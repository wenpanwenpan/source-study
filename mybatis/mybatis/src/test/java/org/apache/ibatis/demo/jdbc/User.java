package org.apache.ibatis.demo.jdbc;

import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * 源码学院-Monkey
 * 只为培养BAT程序员而生
 * http://bat.ke.qq.com
 * 往期视频加群:516212256 暗号:6
 */

@ToString
public class User implements Serializable{

  private Integer id;
  private String username;
  private  Integer age;
  private String phone;

  public Date getDesc() {
    return desc;
  }

  public void setDesc(Date desc) {
    this.desc = desc;
  }

  private Date desc;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public Integer getAge() {
    return age;
  }

  public void setAge(Integer age) {
    this.age = age;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }



  @Override
  public String toString() {
    return "User{" +
           "id=" + id +
           ", username='" + username + '\'' +
           ", age=" + age +
           ", phone='" + phone + '\'' +
           ", desc='" + desc + '\'' +
           '}';
  }
}
