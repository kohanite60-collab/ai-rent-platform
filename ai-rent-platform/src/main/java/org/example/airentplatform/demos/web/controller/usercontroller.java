package org.example.airentplatform.demos.web.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import jakarta.mail.Session;
import jakarta.servlet.http.HttpSession;
import org.example.airentplatform.demos.web.mapper.AiTaskMapper;
import org.example.airentplatform.demos.web.mapper.UserMapper;
import org.example.airentplatform.demos.web.pojo.AiTask;
import org.example.airentplatform.demos.web.pojo.Result;
import org.example.airentplatform.demos.web.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static javax.swing.text.html.CSS.getAttribute;
@RestController
@Component
@RequestMapping("/user")
public class usercontroller {

    @Autowired
    UserMapper UserMapper;

    @Autowired
    AiTaskMapper aiTaskMapper;

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

        // 只对「显式传了值」的列做 set，其余列一概不碰。
        //
        // 不能用 `new User()` 当更新载体：User.money 是基本类型 int（默认 0，永远不为 null），
        // 而 MyBatis-Plus 默认的 FieldStrategy.NOT_NULL 只跳过 null、不跳过 0，
        // 于是「只改用户名」也会生成 `SET money = 0`，把用户算力静默清零。
        // username / password / email / role 都是 String，没设就是 null 会被跳过，
        // 所以此前只有算力中枪。（同类事故：admincontroller 上架套餐时 money/rmb 被清零）
        boolean changed = false;
        if (username != null && !username.isBlank()) {
            updateWrapper.set("username", username);
            changed = true;
        }
        if (password != null && !password.isBlank()) {
            updateWrapper.set("password", password);
            changed = true;
        }
        // 两个参数都没传时不能放行：wrapper 里一个 set 都没有，
        // 会生成 `UPDATE user SET WHERE username = ?`，MySQL 直接语法报错
        if (!changed) {
            return Result.error("没有需要修改的内容");
        }

            int t=UserMapper.update(null,updateWrapper);

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

    //查看当前用户自己的 AI 任务（含生成的诗文），最新在前
    //前端 AiCreate「我的任务」用它替代此前借用 /admin/readai 的做法：
    //那个接口要求 admin，普通用户必被 adminintercepter 拦下（空响应体 → forbidden）
    //身份完全取自 Session，不接收任何用户名参数：
    //前端无从传别人的名字，也就不存在借这个接口遍历他人创作记录的口子
    @GetMapping("/aitask")
    public Result<List<AiTask>> myAitask(HttpSession session) {

        String current = (String) session.getAttribute("user");

        // /user/** 本就在登录拦截器保护内；这里兜一层，防 Session 失效时拿 null 去查库
        if (current == null || current.isBlank()) {
            return Result.error("请先登录");
        }

        QueryWrapper<AiTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", current);
        queryWrapper.orderByDesc("id");     //最新提交的排在最前

        List<AiTask> list = aiTaskMapper.selectList(queryWrapper);

        return Result.success(list);
    }





}












