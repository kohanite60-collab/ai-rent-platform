package org.example.airentplatform.demos.web.intercepter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.airentplatform.demos.web.mapper.UserMapper;
import org.example.airentplatform.demos.web.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class adminintercepter implements HandlerInterceptor {


    @Autowired
    private UserMapper usermapper;



    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {


        HttpSession session=request.getSession();
        if(session.getAttribute("user")!=null){

            String username=(String)session.getAttribute("user");
            QueryWrapper<User> queryWrapper=new QueryWrapper<>();
            queryWrapper.eq("username",username);

            // Session 里的用户名在库中可能已不存在（如用户改过用户名），
            // 不判空会 NPE 导致 /admin/** 全部 500
            User dbUser=usermapper.selectOne(queryWrapper);
            if(dbUser==null){
                return false;
            }

            String role=dbUser.getRole();

            if(role.equals("admin")){

                return true;
            }


            return false;
        }



    return false;

}







}
