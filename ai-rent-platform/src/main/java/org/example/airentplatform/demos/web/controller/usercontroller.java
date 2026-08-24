package org.example.airentplatform.demos.web.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import jakarta.servlet.http.HttpSession;
import org.example.airentplatform.demos.web.mapper.UserMapper;
import org.example.airentplatform.demos.web.pojo.Result;
import org.example.airentplatform.demos.web.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static javax.swing.text.html.CSS.getAttribute;

@Component
@RequestMapping("/user")
public class usercontroller {

    @Autowired
    UserMapper UserMapper;

    //查看信息
    @GetMapping("/profile")
    public Result readprofile(HttpSession session) {
        String username = (String) session.getAttribute("user");

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        User user = UserMapper.selectOne(queryWrapper);

        return Result.success(user);


    }


    //修改信息
    @PostMapping("/profile")
    public Result updateprofile(HttpSession session, String username, String password) {

            User user = new User();
            user.setUsername(username);
            user.setPassword(password);


            String username0=(String)session.getAttribute("user");
        UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("username", username0);

            int t=UserMapper.update(user,updateWrapper);


            if (t>0){return Result.success("修改成功");}





        return Result.error("与先前数据相同，修改失败");


    }









}












