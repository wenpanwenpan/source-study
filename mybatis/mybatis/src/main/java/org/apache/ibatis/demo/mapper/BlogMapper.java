package org.apache.ibatis.demo.mapper;


import org.apache.ibatis.demo.entity.Blog;

public interface BlogMapper {

  // @Select("SELECT * FROM blog WHERE id = #{id}")
  Blog selectBlog(int id);

}
