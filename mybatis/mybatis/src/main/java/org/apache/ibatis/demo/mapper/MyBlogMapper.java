package org.apache.ibatis.demo.mapper;

import org.apache.ibatis.demo.entity.Blog;

/**
 * @description:
 * @author: 文攀 Mr_wenpan@163.com
 * @date: 2020-10-18 12:30
 **/
public interface MyBlogMapper {

  // @Select("SELECT * FROM blog WHERE id = #{id}")
  Blog selectBlog(int id);

  Integer insertBlog(Blog blog);

}
