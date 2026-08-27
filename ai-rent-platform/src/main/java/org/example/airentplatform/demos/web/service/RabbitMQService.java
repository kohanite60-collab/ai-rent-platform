package org.example.airentplatform.demos.web.service;

import jakarta.annotation.Resource;
import org.example.airentplatform.demos.web.confign.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;

@Service
public class RabbitMQService {

    @Resource
    private RabbitTemplate rabbitTemplate;

    public String sendMsg(String msg) throws Exception {
        try {

            rabbitTemplate.convertAndSend(RabbitMQConfig.RABBITMQ_DEMO_DIRECT_EXCHANGE, RabbitMQConfig.RABBITMQ_DEMO_DIRECT_ROUTING, msg);
            return "ok";
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }
}
