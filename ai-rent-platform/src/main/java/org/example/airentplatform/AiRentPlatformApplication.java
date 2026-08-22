package org.example.airentplatform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("org.example.airentplatform.demos.web.mapper")
public class AiRentPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiRentPlatformApplication.class, args);
    }





}

