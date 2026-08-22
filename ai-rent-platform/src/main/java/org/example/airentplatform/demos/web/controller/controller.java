package org.example.airentplatform.demos.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.servlet.http.HttpSession;
import org.example.airentplatform.demos.web.mapper.UserMapper;
import org.example.airentplatform.demos.web.pojo.Result;
import org.example.airentplatform.demos.web.pojo.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;



@RestController
@RequestMapping("/user")
public class controller {


    @Autowired
    private UserMapper UserMapper;

    @PostMapping("/login")
    public Result<String> login(String username, String password, HttpSession session) {



        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);

        User user = UserMapper.selectOne(queryWrapper);
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (!user.getPassword().equals(password)) {
            return Result.error("密码错误");
        }

        session.setAttribute("user", username);
        return Result.success("登录成功");

    }
    @PostMapping("/register")
    public Result<String> register(String username, String password) {

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);


        User user = UserMapper.selectOne(queryWrapper);
        if (user != null) {
            return Result.error("用户已存在");
        }
        user = new User();
        user.setUsername(username);
        user.setPassword(password);
        int t=UserMapper.insert(user);
        if (t>0){return Result.success("注册成功");}
        return Result.error("注册失败");
    }






}
