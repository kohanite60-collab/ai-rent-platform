package org.example.airentplatform.demos.web.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AiService {
    private final ChatClient chatClient;
    public AiService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String createPoem(String prompt){
        String poem=this.chatClient.prompt()
                .user(prompt)
                .system("""
                        你是一名中国古典诗人。
                        你的任务是根据用户要求创作诗歌。
                        要求：
                        1. 语言优美
                        2. 有画面感
                        3. 可以使用五言、七言或者现代诗形式
                        4. 不要解释创作过程，只输出诗歌内容
                        """)
                .call()
                .content();
        return poem;
    }

}
