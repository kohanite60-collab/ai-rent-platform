package org.example.airentplatform.demos.web.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.servlet.http.HttpSession;
import org.example.airentplatform.demos.web.mapper.AiTaskMapper;
import org.example.airentplatform.demos.web.mapper.UserMapper;
import org.example.airentplatform.demos.web.pojo.AiTask;
import org.example.airentplatform.demos.web.pojo.Result;
import org.example.airentplatform.demos.web.pojo.User;
import org.example.airentplatform.demos.web.service.AiService;
import org.example.airentplatform.demos.web.service.RabbitMQService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@Component
@RequestMapping("/ai")
public class aicontroller {

    @Autowired
    private AiService aiService;

    @Autowired
    private RabbitMQService rabbitMQService;

    @Autowired
    private AiTaskMapper aiTaskMapper;

    @Autowired
    private UserMapper userMapper;

    //ai生成古诗接口
    @PostMapping("/poem")
    public Result create(HttpSession session,String prompt) throws Exception {
        //获取用户名
        String username=(String) session.getAttribute("username");

        //扣除算力
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);

        User user = userMapper.selectOne(queryWrapper);
        User user1=new User();
        user1.setMoney(user.getMoney()-50);
        userMapper.update(user1, queryWrapper);

        //创建任务
        AiTask aitask=new AiTask();
        String taskNo= UUID.randomUUID().toString();
        aitask.setTaskNo(taskNo);
        aitask.setPrompt(prompt);
        aitask.setStatus("排队中");
        aitask.setUsername(username);
        aitask.setTaskname("ai写诗");

        //返回任务编号
        rabbitMQService.sendMsg(aitask.getTaskNo());
        return Result.success(taskNo);

    }
    //ai生成任务状态轮询接口
    @GetMapping("/status")
    public Result status(String taskNo){
        QueryWrapper<AiTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("taskNo", taskNo);
        AiTask aitask=aiTaskMapper.selectOne(queryWrapper);
        return Result.success(aitask.getStatus());
    }

    //ai作品展示接口
    @GetMapping("/show")
    public Result show(String id){
        return null;
    }



}
