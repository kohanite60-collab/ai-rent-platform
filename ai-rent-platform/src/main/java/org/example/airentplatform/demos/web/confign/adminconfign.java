package org.example.airentplatform.demos.web.confign;

import org.example.airentplatform.demos.web.intercepter.adminintercepter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Component
public class adminconfign implements WebMvcConfigurer {

    @Autowired
    private adminintercepter adminintercepter;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {


        registry.addInterceptor(adminintercepter)
                .addPathPatterns("/admin") ;// 拦截所有 URL。

    }


}
