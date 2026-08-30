package org.example.airentplatform.demos.web.payment;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import org.example.airentplatform.demos.web.confign.AliPayConfig;
import org.example.airentplatform.demos.web.pojo.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;


@Component
public class AliPay {

    @Autowired
    private AliPayConfig alipayConfig;


    public String pay(Order order) throws AlipayApiException {

        // 支付宝网关
        String serverUrl = alipayConfig.getGatewayUrl();
        // APPID
        String appId = alipayConfig.getAppId();
        // 商户私钥, 即PKCS8格式RSA2私钥
        String privateKey = alipayConfig.getAppPrivateKey();
        // 格式化为 json 格式
        String format = "json";
        // 字符编码格式
        String charset = alipayConfig.getCharset();
        // 支付宝公钥, 即对应APPID下的支付宝公钥
        String alipayPublicKey = alipayConfig.getAlipayPublicKey();
        // 签名方式
        String signType = alipayConfig.getSignType();

        // 1、获得初始化的AlipayClient
        AlipayClient alipayClient = new DefaultAlipayClient(
                serverUrl, appId, privateKey, format, charset, alipayPublicKey, signType);

        // 2、设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();

        // 封装参数(以json格式封装)
        Map<String, Object> bizContent = new HashMap<>();

        bizContent.put("out_trade_no", String.valueOf(System.currentTimeMillis()));
        bizContent.put("total_amount", String.valueOf(order.getRmb()));
        bizContent.put("subject", order.getProduct());
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");

        alipayRequest.setBizContent(JSON.toJSONString(bizContent));

        // 3、请求支付宝进行付款，并获取支付结果
        String result = alipayClient.pageExecute(alipayRequest).getBody();
        // 返回付款信息
        return result;
    }
}
