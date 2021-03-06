package com.wp.thread.mapper;


import com.wp.thread.entity.Blog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface BlogMapper {

	/**
	 * 插入blog对象
	 * @param blog
	 * @return
	 */
	@Insert("insert into blog(username,context) values (#{username},#{context})")
	Integer insert(Blog blog);

	/**
	 * 通过id查找blog
	 * @param blog
	 * @return
	 */
	@Select("select * from blog where id = #{id}")
	List<Blog> selectBlogById(Blog blog);
}
