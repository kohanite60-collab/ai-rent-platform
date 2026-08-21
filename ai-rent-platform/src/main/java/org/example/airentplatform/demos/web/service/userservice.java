package org.example.airentplatform.demos.web.service;

import org.example.airentplatform.demos.web.mapper.usermapper;
import org.example.airentplatform.demos.web.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class userservice {
    @Autowired
    private usermapper usermapper;
    public User getbyname(String username) {
        return usermapper.getbyname(username);
    }

    public int add(User user) {
        return usermapper.add(user) ;
    }
}
