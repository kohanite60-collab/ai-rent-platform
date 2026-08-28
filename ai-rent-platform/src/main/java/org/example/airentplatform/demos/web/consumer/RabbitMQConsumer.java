package org.example.airentplatform.demos.web.consumer;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.airentplatform.demos.web.confign.RabbitMQConfig;
import org.example.airentplatform.demos.web.mapper.AiTaskMapper;
import org.example.airentplatform.demos.web.pojo.AiTask;
import org.example.airentplatform.demos.web.pojo.User;
import org.example.airentplatform.demos.web.service.AiService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RabbitMQConsumer {

    @Autowired
    private AiTaskMapper aiTaskMapper;

    @Autowired
    private AiService aiService;

    @RabbitListener(queues = RabbitMQConfig.RABBITMQ_DEMO_TOPIC)
    public void receiveMsg(String message) {

        //查出唯一任务编号的信息
        QueryWrapper<AiTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("taskNo", message);
        AiTask aitask=aiTaskMapper.selectOne(queryWrapper);

        //将生成任务交给ai
        aitask.setStatus("进行中");
        aiTaskMapper.update(aitask,queryWrapper);
        String poem = aiService.createPoem(aitask.getPrompt());

        //将构建状态同步到数据库
        if (poem!=null){

            aitask.setStatus("构建完成");
            aiTaskMapper.update(aitask,queryWrapper);

        }else {

            aitask.setStatus("构建失败");
            aiTaskMapper.update(aitask,queryWrapper);
        }

    }
}