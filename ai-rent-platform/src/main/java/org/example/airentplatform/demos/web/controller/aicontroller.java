package org.example.airentplatform.demos.web.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import jakarta.servlet.http.HttpSession;
import org.example.airentplatform.demos.web.mapper.AiProductionMapper;
import org.example.airentplatform.demos.web.mapper.AiTaskMapper;
import org.example.airentplatform.demos.web.mapper.UserMapper;
import org.example.airentplatform.demos.web.pojo.AiProduction;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
@RestController
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

    @Autowired
    private AiProductionMapper aiProductionMapper;

    //ai生成古诗接口
    @PostMapping("/poem")
    public Result create(HttpSession session,String prompt) throws Exception {
        //获取用户名
        String username=(String) session.getAttribute("user");

        //扣除算力
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);

        User user = userMapper.selectOne(queryWrapper);
        User user1=new User();
        if (user.getMoney()<50){
            return Result.error("余额不足");
        }
        user1.setMoney(user.getMoney()-50);
        userMapper.update(user1, queryWrapper);

        //创建任务
        AiTask aitask=new AiTask();
        String taskNo= UUID.randomUUID().toString();//唯一任务编号


        aitask.setTaskNo(taskNo);
        aitask.setPrompt(prompt);
        aitask.setStatus("排队中");
        aitask.setUsername(username);
        aitask.setTaskname("ai写诗");
        aiTaskMapper.insert(aitask);

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
        return Result.success(aitask);
    }

    //ai作品展示接口
    @GetMapping("/show/list")
    public Result show(int sortid){


        QueryWrapper<AiProduction> sortid1 = new QueryWrapper<>();
        sortid1.eq("sortid", sortid);
        sortid1.orderByDesc("view");    //按热度降序排列

        List<AiProduction> list = aiProductionMapper.selectList(sortid1);
        if (list!=null){
            return Result.success(list);
        }

        else return Result.success("暂无对应内容");
    }


    //ai作品详情接口
    @GetMapping("/show/{id}")
    public Result show(String id){
        AiProduction aiProduction = aiProductionMapper.selectById(id);

        if (aiProduction!=null){
            aiProduction.setView(aiProduction.getView()+1);//浏览量加一

            aiProductionMapper.updateById(aiProduction);
            return Result.success(aiProduction);
        }
        else return Result.success("暂无对应内容");
    }

    //ai优质作品上传接口
    @PostMapping("/upload")
    public Result upload(HttpSession session,String title,String data,int sortid){

        String username=(String) session.getAttribute("user");
        AiProduction aiProduction=new AiProduction();
        aiProduction.setTitle(title);
        aiProduction.setData(data);
        aiProduction.setUser(username);
        aiProduction.setSortid(sortid);
        aiProduction.setView(0);


        aiProductionMapper.insert(aiProduction);

        return null;
    }



}
