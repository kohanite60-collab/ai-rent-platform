package org.example.airentplatform.demos.web.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.example.airentplatform.demos.web.confign.AliPayConfig;
import org.example.airentplatform.demos.web.mapper.OrderMapper;
import org.example.airentplatform.demos.web.mapper.TokenSpuMapper;
import org.example.airentplatform.demos.web.mapper.UserMapper;
import org.example.airentplatform.demos.web.pojo.Order;
import org.example.airentplatform.demos.web.pojo.PayOrderParams;
import org.example.airentplatform.demos.web.pojo.TokenSpu;
import org.example.airentplatform.demos.web.pojo.User;
import org.example.airentplatform.demos.web.service.PaymentService;
import org.example.airentplatform.demos.web.utils.MailUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@Component
public class PayController {
    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TokenSpuMapper tokenSpuMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private AliPayConfig alipayConfig;

    @Autowired
    private MailUtil mailUtil;

    private static final Logger log = LoggerFactory.getLogger(PayController.class);


    @PostMapping("/pay")
    public String pay(int spuId, HttpSession session) throws AlipayApiException {

        String user = (String) session.getAttribute("user");


        String product = tokenSpuMapper.selectById(spuId).getName();
        int rmb = tokenSpuMapper.selectById(spuId).getRmb();


        Order order = new Order();
        PayOrderParams params= new PayOrderParams();

        String uuid = UUID.randomUUID().toString();
        order.setId(uuid);
        order.setUser(user);
        order.setSpuId(spuId);
        order.setStatus(0);  //表示订单未完成
        orderMapper.insert(order);


        params.setOut_trade_no(uuid);
        params.setSubject(product);
        params.setTotal_amount(String.valueOf(rmb));

        return paymentService.aliPay(params);
    }


    //支付宝异步通知（已在 loginconfign 放行，是公开接口，安全靠下面的验签保证）
    //@Transactional：让「加算力」与「改订单状态」处在同一事务里。否则一旦中间失败，
    //会出现「算力已加、订单状态还是 0」，支付宝重试时会再加一次算力（重复到账）。
    @PostMapping("/pay/notify")
    @Transactional
    public String notify(HttpServletRequest request) {

        // 1. 验签：确认请求真的来自支付宝，防止伪造请求给账号刷算力
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (String name : requestParams.keySet()) {
            String[] values = requestParams.get(name);
            StringBuilder valueStr = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                valueStr.append(i == values.length - 1 ? values[i] : values[i] + ",");
            }
            params.put(name, valueStr.toString());
        }
        boolean signVerified;
        try {
            signVerified = AlipaySignature.rsaCheckV1(params, alipayConfig.getAlipayPublicKey(),
                    alipayConfig.getCharset(), alipayConfig.getSignType());
        } catch (AlipayApiException e) {
            return "fail";
        }
        if (!signVerified) {
            return "fail";
        }

        String orderNo = request.getParameter("out_trade_no");
        String tradeStatus = request.getParameter("trade_status");
        if (orderNo == null || tradeStatus == null) {
            return "fail";
        }

        Order order = orderMapper.selectById(orderNo);
        if (order == null) {
            return "fail";
        }

        // 2. 幂等：这笔订单已成功加过算力，直接确认，防止支付宝重复投递时重复加。
        //    注意：Order.status 是 int 基本类型，不能与 null 比较（写 `!= null` 直接编译不过），
        //    只判 `== 2` 即可。
        if (order.getStatus() == 2) {
            return "success";
        }

        if (tradeStatus.equals("WAIT_BUYER_PAY")) {order.setStatus(1);}

        if (tradeStatus.equals("TRADE_CLOSED")) {order.setStatus(3);}
        if (tradeStatus.equals("TRADE_FINISHED")) {order.setStatus(4);}

        if (tradeStatus.equals("TRADE_SUCCESS")) {order.setStatus(2);
        String username= order.getUser();

        int id = order.getSpuId();
        TokenSpu spu=tokenSpuMapper.selectById(id);
        if (spu == null) { return "fail"; }
        int increment = spu.getMoney();

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        User dbUser = userMapper.selectOne(queryWrapper);
        if (dbUser == null) { return "fail"; }
        int money = dbUser.getMoney();

        User user=new User();
        user.setMoney(money+increment);

        userMapper.update(user,queryWrapper);  //更新用户余额

        // 3. 发送购买成功邮件（用户未绑定邮箱时 MailUtil 内部直接跳过）
        //    邮件只是附加功能：SMTP 认证失败 / 网络不通 / 被限流都可能抛异常。
        //    一旦异常逃出去，本次回调会返回 500 而不是 "success"，支付宝判定失败并重试；
        //    而重试时订单仍是 status=0、幂等判断拦不住，算力就会被重复叠加。
        //    因此这里必须兜住：邮件发不出去只记日志，绝不影响算力到账。
        try {
            mailUtil.sendToUsername(username, "购买算力套餐" + spu.getName() + "成功");
        } catch (Exception e) {
            log.warn("购买成功邮件发送失败（不影响算力到账）：user={}, err={}", username, e.getMessage());
        }
        }


        orderMapper.updateById(order);//更新订单状态

        // 告诉支付宝：我已经收到通知了
        return "success";
    }








}
