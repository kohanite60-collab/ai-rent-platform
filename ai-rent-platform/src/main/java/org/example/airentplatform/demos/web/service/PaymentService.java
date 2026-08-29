package org.example.airentplatform.demos.web.service;

import com.alipay.api.AlipayApiException;
import org.example.airentplatform.demos.web.pojo.Order;

public interface PaymentService {
    String aliPay(Order order) throws AlipayApiException;
}
