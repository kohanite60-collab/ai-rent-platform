package org.example.airentplatform.demos.web.controller;

import com.alipay.api.AlipayApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.example.airentplatform.demos.web.mapper.OrderMapper;
import org.example.airentplatform.demos.web.mapper.TokenSpuMapper;
import org.example.airentplatform.demos.web.mapper.UserMapper;
import org.example.airentplatform.demos.web.pojo.Order;
import org.example.airentplatform.demos.web.pojo.PayOrderParams;
import org.example.airentplatform.demos.web.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
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


    //支付宝异步通知
    @PostMapping("/notify")
    public String notify(HttpServletRequest request) {

        // 接收支付宝返回的参数
        String orderNo = request.getParameter("out_trade_no");
        String tradeNo = request.getParameter("trade_no");
        String tradeStatus = request.getParameter("trade_status");
        String totalAmount = request.getParameter("total_amount");

        Order order=orderMapper.selectById(orderNo);
        if (tradeStatus.equals("WAIT_BUYER_PAY")) {order.setStatus(1);}
        if (tradeStatus.equals("TRADE_SUCCESS")) {order.setStatus(2);}
        if (tradeStatus.equals("TRADE_CLOSED")) {order.setStatus(3);}
        if (tradeStatus.equals("TRADE_FINISHED")) {order.setStatus(4);}

        orderMapper.updateById(order);//更新订单状态


        // 告诉支付宝：我已经收到通知了
        return "success";
    }








}
