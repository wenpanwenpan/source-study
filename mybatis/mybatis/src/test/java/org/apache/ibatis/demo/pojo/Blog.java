

package org.apache.ibatis.demo.pojo;

public class Blog {
  private Integer id;
  private String username;
  private String context;

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

  public String getContext() {
    return context;
  }

  public void setContext(String context) {
    this.context = context;
  }

  @Override
  public String toString() {
    return "Blog{" +
      "id=" + id +
      ", username='" + username + '\'' +
      ", context='" + context + '\'' +
      '}';
  }
}
