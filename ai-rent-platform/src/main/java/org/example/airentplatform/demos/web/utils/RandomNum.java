package org.example.airentplatform.demos.web.utils;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;


@Component
public class RandomNum {


    //生成随机六位验证码

    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateCode() {
        int code = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(code);
    }


}
