package org.example.airentplatform.demos.web.confign;

import org.example.airentplatform.demos.web.intercepter.loginintercepter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class loginconfign implements WebMvcConfigurer {


    @Autowired
    private loginintercepter loginintercepter;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {			//拦截路径
        registry.addInterceptor(loginintercepter).addPathPatterns("/**").excludePathPatterns("/user/login","/user/register","/index.html","/static/**","/","/ccs/**","/js/**","/images/**","/mail/**"); //表示拦截所有请求
    }


}