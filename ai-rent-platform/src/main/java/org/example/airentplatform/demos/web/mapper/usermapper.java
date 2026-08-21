package org.example.airentplatform.demos.web.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.airentplatform.demos.web.pojo.User;
@Mapper
public interface usermapper {
    @Select("select * from user where username = #{username}")
    User getbyname(String username);
    @Insert("insert into user(username, password,money) values(#{username}, #{password}, 0)")
    int add(User user);
}
