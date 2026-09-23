package org.example.airentplatform.demos.web.utils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.servlet.http.HttpSession;
import org.example.airentplatform.demos.web.mapper.UserMapper;
import org.example.airentplatform.demos.web.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * 邮件发送工具类
 * 用法：注入后调用 sendToSessionUser(session, 消息内容)
 */
@Component
public class MailUtil {

    @Autowired
    private JavaMailSender jms;

    @Autowired
    private UserMapper usermapper;

    // 发件人，取 application.yml 的 spring.mail.username（环境变量 EMAIL）
    @Value("${spring.mail.username}")
    private String from;

    /**
     * 给当前登录用户绑定的邮箱发送消息
     *
     * @param session 当前用户的 session（取用户名查库）
     * @param text    要发送的消息内容
     * @return true=发送成功；false=未登录/用户不存在/未绑定邮箱（未绑定邮箱直接返回，不发送）
     */
    public boolean sendToSessionUser(HttpSession session, String text) {

        String username = (String) session.getAttribute("user");
        if (username == null) {
            return false;
        }

        return sendToUsername(username, text);
    }

    /**
     * 按用户名查库后向其绑定邮箱发送消息。
     * 适用场景：调用方拿不到 HttpSession，如支付宝异步回调 /pay/notify。
     */
    public boolean sendToUsername(String username, String text) {

        User user = usermapper.selectOne(new QueryWrapper<User>().eq("username", username));
        // 未绑定邮箱：直接返回，不发送
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return false;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);              // 发送者：yml 配置的邮箱
        message.setTo(user.getEmail());     // 接收者：用户绑定的邮箱
        message.setSubject("AI算力租赁平台通知");
        message.setText(text);
        jms.send(message);

        return true;
    }
}
