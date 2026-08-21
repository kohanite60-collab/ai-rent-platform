package org.example.airentplatform;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AiRentPlatformApplicationTests {

    @Autowired
    private ChatClient.Builder chatClientBuilder;


    @Test
    void contextLoads() {

        ChatClient chatClient = chatClientBuilder.build();

        String result = chatClient
                .prompt()
                .user("你好，请简单介绍一下你自己")
                .call()
                .content();

        System.out.println(result);

    }

}
