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
}