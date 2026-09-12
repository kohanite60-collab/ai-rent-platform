package org.example.airentplatform.demos.web.controller;


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
@RestController
@Component
@RequestMapping("/admin")
public class admincontroller {

    @Autowired
    private UserMapper UserMapper;

    @Autowired
    private AiTaskMapper AiTaskMapper;

    //查询所有用户
    @GetMapping("/read")
    public Result<List<User>> read(){
        List<User> users=UserMapper.selectList(null);
        return Result.success(users);

    }


    //修改用户信息
    @PostMapping("/revise")
    public Result revise(int id,String role,int money){

        User user=new User();
        user.setId(id);
        user.setRole(role);
        user.setMoney(money);

        int row=UserMapper.updateById(user);
        if (row>0){

                return Result.success("修改成功");}
        else{
            return Result.error("修改失败");
        }


    }

    //查看所有ai任务情况
    @GetMapping("/readai")
    public Result<List<AiTask>> readai(){
        List<AiTask> aitasks= AiTaskMapper.selectList(null);
        return Result.success(aitasks);


    }



}
