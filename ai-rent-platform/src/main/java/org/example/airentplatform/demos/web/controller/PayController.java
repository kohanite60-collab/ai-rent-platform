package org.example.airentplatform.demos.web.controller;

import com.alipay.api.AlipayApiException;
import org.example.airentplatform.demos.web.pojo.Order;
import org.example.airentplatform.demos.web.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;

@Component
public class PayController {
    @Autowired
    private PaymentService paymentService;


    @PostMapping("/pay")
    public String pay(String product,String user,int rmb,int money) throws AlipayApiException {
        Order order = new Order();
        order.setProduct(product);
        order.setUser(user);
        order.setRmb(rmb);
        order.setMoney(money);
        String status="订单创建中";
        order.setStatus(status);

        return paymentService.aliPay(order);
    }
}
