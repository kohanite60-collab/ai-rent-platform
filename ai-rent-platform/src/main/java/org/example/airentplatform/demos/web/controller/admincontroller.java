package org.example.airentplatform.demos.web.controller;


import org.example.airentplatform.demos.web.mapper.AiTaskMapper;
import org.example.airentplatform.demos.web.mapper.TokenSpuMapper;
import org.example.airentplatform.demos.web.mapper.UserMapper;
import org.example.airentplatform.demos.web.pojo.AiTask;
import org.example.airentplatform.demos.web.pojo.Result;
import org.example.airentplatform.demos.web.pojo.TokenSpu;
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

    @Autowired
    private TokenSpuMapper spuMapper;

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

    //新增套餐

    @PostMapping("/addspu")
    public Result add(String name,int money,int rmb){



        if (name.equals(null)||money==0||rmb==0)

        { return Result.error("信息内容不完整或不合规"); }

        TokenSpu spu=new TokenSpu();
        spu.setName(name);
        spu.setMoney(money);
        spu.setRmb(rmb);

        spu.setStatus(0);       //初始保持下架状态
        int row=spuMapper.insert(spu);

        if (row>0){return Result.success("套餐添加成功");}
        else {return Result.error("套餐添加失败");}



    }

    //查看套餐列表
    @GetMapping("/readspu")
    public Result<List<TokenSpu>> readspu(){


        List<TokenSpu> spus=spuMapper.selectList(null);
        return Result.success(spus);
    }

    //修改套餐信息
    @PostMapping("/revisespu")
    public Result reviseSpu(int id,String name,int money,int rmb){
            TokenSpu spu=new TokenSpu();
            spu.setId(id);
            spu.setName(name);
            spu.setMoney(money);
            spu.setRmb(rmb);
            int row=spuMapper.updateById(spu);

            if (row>0){return Result.success("修改成功");}
            else {return Result.error("请勿提交重复数据");}

    }

    //上,下架套餐
    @PostMapping("/revisespu/status")
    public Result upspu(int id,int status){
        TokenSpu spu=new TokenSpu();
        spu.setId(id);
        spu.setStatus(status);
        int row=spuMapper.updateById(spu);
        if (row>0){return Result.success("上架状态修改成功");}
        return null;
    }

    //删除套餐
    @PostMapping("/deletespu")
    public Result deletespu(int id){


        int row=spuMapper.deleteById(id);
        if (row>0){return Result.success("删除成功");}
        else {return Result.error("删除失败");}
    }


}
