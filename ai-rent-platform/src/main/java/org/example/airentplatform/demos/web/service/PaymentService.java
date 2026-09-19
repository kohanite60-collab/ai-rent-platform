package org.example.airentplatform.demos.web.service;

import com.alipay.api.AlipayApiException;
import org.example.airentplatform.demos.web.pojo.PayOrderParams;

public interface PaymentService {
    String aliPay(PayOrderParams order) throws AlipayApiException;
}
