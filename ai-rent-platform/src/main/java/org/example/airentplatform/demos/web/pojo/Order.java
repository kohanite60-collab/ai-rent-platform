package org.example.airentplatform.demos.web.pojo;


import lombok.Data;

@Data
public class Order {
    private String id; //唯一订单号
    private String user;
    private int spuId;

    private int status;  //0未完成，1待付款，2支付成功，3交易关闭，4交易完成
}
