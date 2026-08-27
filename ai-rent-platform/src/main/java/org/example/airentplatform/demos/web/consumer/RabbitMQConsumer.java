package org.example.airentplatform.demos.web.consumer;

import org.example.airentplatform.demos.web.confign.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RabbitMQConsumer {

    @RabbitListener(queues = RabbitMQConfig.RABBITMQ_DEMO_TOPIC)
    public void receiveMsg(String message) {
        System.out.println("收到消息：" + message);
    }
}