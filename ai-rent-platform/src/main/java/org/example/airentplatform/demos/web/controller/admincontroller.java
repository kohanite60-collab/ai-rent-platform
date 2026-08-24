package org.example.airentplatform.demos.web.controller;


import org.example.airentplatform.demos.web.mapper.UserMapper;
import org.example.airentplatform.demos.web.pojo.Result;
import org.example.airentplatform.demos.web.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Component
@RequestMapping("/admin")
public class admincontroller {

    @Autowired
    private UserMapper UserMapper;


    @GetMapping("/read")
    public Result<List<User>> read(){
        List<User> users=UserMapper.selectList(null);
        return Result.success(users);

    }



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



}
