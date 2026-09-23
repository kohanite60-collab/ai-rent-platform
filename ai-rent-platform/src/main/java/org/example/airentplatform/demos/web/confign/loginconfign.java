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
        // 注意：必须放行 /error。Controller 抛异常时 Spring 会转发到 /error，
        // 若 /error 被拦截器拦下，客户端只会收到 {"msg":"请先登录"}，
        // 真正的异常（状态码 500）会被完全掩盖，极难排查。
        //
        // /spu/list 是套餐的只读展示接口（spucontroller），首页需要让未登录用户也能
        // 看到卖什么，所以放行。这里刻意写精确路径而不是 /spu/**：
        // 浏览可以放宽，交易不能放宽 —— 下单接口 POST /pay 必须继续要求登录。
        registry.addInterceptor(loginintercepter).addPathPatterns("/**").excludePathPatterns("/check/login","/check/register","/check/sendcode","/spu/list","/index.html","/static/**","/","/css/**","/js/**","/images/**","/mail/**","/error"); //表示拦截所有请求
    }


}
