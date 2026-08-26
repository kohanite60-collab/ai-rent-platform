package org.example.airentplatform;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import static com.fasterxml.jackson.databind.jsonFormatVisitors.JsonValueFormat.EMAIL;
@Component
@SpringBootTest
class AiRentPlatformApplicationTests {


    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private JavaMailSender jms;

    @Test
    void contextLoads() {

        // 写入
        redisTemplate.opsForValue().set("name", "张三");

        // 读取
        Object value = redisTemplate.opsForValue().get("name");

        System.out.println(value);


    }

    @Test
    public void sendSimpleMail( ) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(username);       // 发送者
            message.setTo("2635521871@qq.com");       // 接受者
            message.setSubject("测试发送");   // 邮件主题
            message.setText("你好");         // 邮件正文

            jms.send(message);
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
            System.out.println(username);
        }
    }

        @Test
        public void test() {
            System.out.println(username);
            System.out.println(password);
        }


}
