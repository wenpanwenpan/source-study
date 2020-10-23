package bat.ke.qq.com.mapper;

import bat.ke.qq.com.pojo.Blog;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

public interface BlogMapper {

  // @Select("SELECT * FROM blog WHERE id = #{id}")
  Blog selectBlog(int id);

}
