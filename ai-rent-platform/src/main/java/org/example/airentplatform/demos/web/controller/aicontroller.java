package org.example.airentplatform.demos.web.controller;


import org.example.airentplatform.demos.web.pojo.Result;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Component
@RequestMapping("/ai")
public class aicontroller {


    private final ChatClient chatClient;

    public aicontroller(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("""
                        你是一名中国古典诗人。
                        你的任务是根据用户要求创作诗歌。
                        要求：
                        1. 语言优美
                        2. 有画面感
                        3. 可以使用五言、七言或者现代诗形式
                        4. 不要解释创作过程，只输出诗歌内容
                        """)
                .build();
    }

    @PostMapping("/poem")
    public Result create(String prompt){

        String response=this.chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        return Result.success(response);

    }


    @GetMapping("/show")
    public Result show(String id){
        return null;
    }



}
