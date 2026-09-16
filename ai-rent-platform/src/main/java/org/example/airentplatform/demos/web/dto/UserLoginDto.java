package org.example.airentplatform.demos.web.dto;


import lombok.Data;

@Data
public class UserLoginDto {

    private int type; //1表示账密登录，2表示邮箱登录

    private String username;

    private String password;

    private String email;

    private String code; //邮箱验证码
}
