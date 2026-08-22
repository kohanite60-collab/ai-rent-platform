package org.example.airentplatform.demos.web.intercepter;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.airentplatform.demos.web.mapper.UserMapper;
import org.example.airentplatform.demos.web.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;


@Component
public class loginintercepter implements HandlerInterceptor {
    @Autowired
    private UserMapper usermapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {


        HttpSession session=request.getSession();
        String name = (String)session.getAttribute("user");

        if (name == null) {

            response.setContentType(
                    "application/json;charset=UTF-8");

            response.getWriter().write(
                    "{\"msg\":\"请先登录\"}");

            return false;
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", name);
        // 用户不存在
        if (usermapper.selectOne(queryWrapper) == null) {

            response.setContentType(
                    "application/json;charset=UTF-8");

            response.getWriter().write(
                    "{\"msg\":\"用户不存在，请重新登录\"}");

            return false;
        }
        return true;


    }


}