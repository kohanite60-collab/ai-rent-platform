package org.example.airentplatform.demos.web.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;


/**
 * 用户实体类
 * 使用@Data注解自动生成getter、setter、toString等方法
 */
@Data
public class User {

    private String username;  // 用户名

    private String password;  // 密码

    private int money;

    @TableId
    private int id;

}
