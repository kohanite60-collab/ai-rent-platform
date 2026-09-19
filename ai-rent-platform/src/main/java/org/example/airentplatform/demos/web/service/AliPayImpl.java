package org.example.airentplatform.demos.web.service;

import com.alipay.api.AlipayApiException;
import org.example.airentplatform.demos.web.payment.AliPay;
import org.example.airentplatform.demos.web.pojo.PayOrderParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AliPayImpl implements PaymentService{
    @Autowired
    private AliPay alipay;

    @Override
    public String aliPay(PayOrderParams order) throws AlipayApiException {
        return alipay.pay(order);
    }


}
