package org.example.airentplatform.demos.web.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import jakarta.mail.Session;
import jakarta.servlet.http.HttpSession;
import org.example.airentplatform.demos.web.mapper.UserMapper;
import org.example.airentplatform.demos.web.pojo.Result;
import org.example.airentplatform.demos.web.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static javax.swing.text.html.CSS.getAttribute;
@RestController
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

        // 入库前查重：新名字已被别人占用则拒绝，避免库里同名两行
        // 导致 selectOne 抛 TooManyResultsException（同名提交=没改名，不算重复）
        if (username != null && !username.isBlank() && !username.equals(username0)) {
            QueryWrapper<User> checkWrapper = new QueryWrapper<>();
            checkWrapper.eq("username", username);
            if (UserMapper.selectOne(checkWrapper) != null) {
                return Result.error("用户名已存在");
            }
        }

            int t=UserMapper.update(user,updateWrapper);

            // 改了用户名时同步刷新 Session，否则后续请求按旧名查库查不到，
            // 会被登录拦截器判为"用户不存在"强制登出，管理员侧还会触发 NPE
            if (t>0){
                if (username != null && !username.isBlank()) {
                    session.setAttribute("user", username);
                }
                return Result.success("修改成功");
            }





        return Result.error("与先前数据相同，修改失败");


    }

    //绑定邮箱
    @PostMapping("/email")
    public Result updateemail(HttpSession session, String email) {

        String username = (String) session.getAttribute("user");
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        User user = UserMapper.selectOne(queryWrapper);
        user.setEmail(email);

        int row=UserMapper.update(user,queryWrapper);

        if (row>0){ return Result.success("绑定成功");}

        else { return Result.error("请勿绑定相同信息");}



    }





}












