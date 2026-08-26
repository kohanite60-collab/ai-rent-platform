package org.example.airentplatform.demos.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import jakarta.servlet.http.HttpSession;
import org.example.airentplatform.demos.web.mapper.UserMapper;
import org.example.airentplatform.demos.web.pojo.Result;
import org.example.airentplatform.demos.web.pojo.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;


@RestController
@RequestMapping("/check")
public class controller {


    @Autowired
    private UserMapper UserMapper;

    @Autowired
    @Qualifier("redisTemplate")
    private RedisTemplate redistemplate;

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

    @PostMapping("/logout")
    public Result<String> logout(HttpSession session) {
        session.invalidate();
        return Result.success("退出成功");
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
        user.setMoney(0);
        user.setRole("user");
        int t=UserMapper.insert(user);
        if (t>0){return Result.success("注册成功");}
        return Result.error("注册失败");
    }





    @PostMapping("/sign")
    public Result<String> sign(HttpSession session) {

        if (session.getAttribute("user") == null) {
            return Result.error("未登录");
        }



        String username = (String) session.getAttribute("user");
        String value = (String) redistemplate.opsForValue().get(username);
        if (value != null) {
            return Result.error("今天已签到,请明日再来");
        }



        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);

        User user0 = UserMapper.selectOne(queryWrapper);
        int money=user0.getMoney()+100;
        user0.setMoney(money);

        UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("username", username);


        LocalDateTime now = LocalDateTime.now();

        LocalDateTime tomorrow = LocalDate.now()
                .plusDays(1)
                .atStartOfDay();

        long seconds = Duration.between(now, tomorrow).getSeconds();




        int row=UserMapper.update(user0, updateWrapper);
        if (row>0){
            redistemplate.opsForValue().set(username,"1",seconds, TimeUnit.SECONDS);
                                         };


        return Result.success("每日签到领取100算力成功");}

}





