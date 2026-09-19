package org.example.airentplatform.demos.web.confign;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "alipay")
@Data
public class AliPayConfig {

    /**
     * 沙箱 APPID
     */
    private String appId;

    /**
     * 应用私钥
     */
    private String appPrivateKey;

    /**
     * 支付宝公钥
     */
    private String alipayPublicKey;

    /**
     * 支付宝网关
     */
    private String gatewayUrl;

    /**
     * 签名方式
     */
    private String signType;

    /**
     * 字符编码
     */
    private String charset;

    /**
     * 数据格式
     */
    private String format;


    /**
     * 服务器异步通知页面路径,需http://格式的完整路径
     * 踩坑:不能加?type=abc这类自定义参数
     *
     * 这里用于支付宝支付完成后，支付宝支付信息调用后端的路径
     */
    private String notifyUrl;


    /**
     * 页面跳转同步通知页面路径,需http://格式的完整路径
     * 踩坑:不能加?type=abc这类自定义参数
     *
     * 这里用于用户完成支付之后，跳转到的前端页面
     */
    private String returnUrl;





}