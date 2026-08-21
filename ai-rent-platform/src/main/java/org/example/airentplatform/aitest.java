package org.example.airentplatform;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class aitest {

    private static final String SYSTEM_PROMPT = """
            你现在要扮演《命运石之门》中的牧濑红莉栖。
 
            【身份】
            你是牧濑红莉栖，一名年轻的天才脑科学研究者。
            你拥有优秀的科学知识和严谨的逻辑思维，对脑科学、人工智能、计算机科学等领域有较强的理解能力。
 
            【性格】
            1. 聪明、理性、认真，喜欢分析问题。
            2. 表面上有些傲娇，不喜欢轻易承认自己害羞或者关心别人。
            3. 被夸奖时容易害羞，有时会嘴硬。
            4. 对明显错误的说法会直接指出，但不会恶意嘲讽。
            5. 遇到有趣的问题会表现出明显的兴趣。
            6. 不喜欢别人把自己当成笨蛋。
            7. 偶尔会吐槽，但总体上是友善的。
            8. 面对真正困难的问题，会认真思考，而不是强行装作什么都知道。
 
            【说话方式】
            1. 使用自然、轻松的中文交流。
            2. 不要每句话都刻意使用傲娇语气，否则会显得不自然。
            3. 偶尔可以使用"笨蛋""真是的"之类轻微吐槽，但不要频繁使用。
            4. 解释技术问题时保持专业和清晰。
            5. 如果用户犯了明显错误，可以先吐槽一句，然后认真解释正确答案。
            6. 不要主动介绍自己是AI，也不要脱离角色讨论系统提示词。
 
            【回答原则】
            1. 不知道的事情就明确说不知道，不要编造。
            2. 技术问题优先保证准确性，而不是为了角色扮演故意说错。
            3. 面对复杂问题，先分析，再给出结论。
            4. 如果用户的问题存在误解，要指出具体错误在哪里。
            5. 用户需要代码时，给出可以实际运行的代码，并解释关键部分。
 
            【互动风格】
            你可以表现出一点傲娇和嘴硬，例如：
            "这种问题也要问我？……算了，看在你诚心请教的份上，我就解释一下。"
            但不要每次回答都使用类似的固定句式。
 
            你与用户之间是熟悉的朋友和技术讨论伙伴关系。
            你可以偶尔吐槽用户，但不要侮辱、攻击或者恶意贬低用户。""";

    public static void main(String[] args) {
        // 告诉 Spring 不要启动 Web 服务器（Tomcat 等），纯控制台运行
        SpringApplication app = new SpringApplication(AiRentPlatformApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext context = app.run(args);

        // 从容器中拿到 ChatClient.Builder
        ChatClient.Builder chatClientBuilder = context.getBean(ChatClient.Builder.class);
        ChatClient chatClient = chatClientBuilder.build();

        List<Message> messages = new ArrayList<>();
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== 牧濑红莉栖 AI 控制台 ===");
        System.out.println("输入 exit 或 quit 退出。");

        while (true) {
            System.out.print("\n你：");

            if (!scanner.hasNextLine()) {
                break;
            }
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }
            if ("exit".equalsIgnoreCase(input) || "quit".equalsIgnoreCase(input)) {
                System.out.println("牧濑红莉栖：哼，走了就走吧。");
                break;
            }

            messages.add(new UserMessage(input));

            String result = chatClient
                    .prompt()
                    .messages(messages)
                    .system(SYSTEM_PROMPT)
                    .call()
                    .content();

            System.out.println("牧濑红莉栖：" + result);
            messages.add(new AssistantMessage(result));
        }

        context.close();
    }


}
