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

        //任务不存在，直接结束，避免空指针
        if (aitask == null) {
            return;
        }

        //将生成任务交给ai
        aitask.setStatus("进行中");
        aiTaskMapper.update(aitask,queryWrapper);

        //将构建状态同步到数据库
        try {

            String poem = aiService.createPoem(aitask.getPrompt());

            if (poem!=null){

                aitask.setStatus("构建完成");
                // 诗句必须落库：poem 是局部变量，方法返回即被回收，
                // 不写进 content 这次生成的结果就永久丢失（前端只能看到"构建完成"却无诗）。
                // update(entity, wrapper) 只更新非 null 字段，此处 content 一旦赋值即会写回。
                aitask.setContent(poem);
                aiTaskMapper.update(aitask,queryWrapper);

            }else {

                aitask.setStatus("构建失败");
                aiTaskMapper.update(aitask,queryWrapper);
            }

        } catch (Exception e) {

            aitask.setStatus("构建失败");
            aiTaskMapper.update(aitask,queryWrapper);
            e.printStackTrace();
        }

    }
}
