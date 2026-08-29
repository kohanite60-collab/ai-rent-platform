package org.example.airentplatform.demos.web.service;

import com.alipay.api.AlipayApiException;
import org.example.airentplatform.demos.web.pojo.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AliPayImpl implements PaymentService{
    @Autowired
    private Alipay alipay;

    @Override
    public String aliPay(Order order) throws AlipayApiException {
        return alipay.pay(order);
    }


}
