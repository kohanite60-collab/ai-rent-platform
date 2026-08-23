package org.example.airentplatform;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
class AiRentPlatformApplicationTests {

    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    void contextLoads() {

        // 写入
        redisTemplate.opsForValue().set("name", "张三");

        // 读取
        Object value = redisTemplate.opsForValue().get("name");

        System.out.println(value);


    }
}
