package org.example.airentplatform.demos.web.pojo;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 订单实体。
 *
 * <p>表名必须显式声明：该表原名就叫 {@code order}，而 ORDER 是 MySQL 的保留字
 * （ORDER BY 用的），MyBatis-Plus 拼 SQL 时不会自动加反引号，生成的
 * {@code INSERT INTO order (...) / SELECT ... FROM order} 会被 MySQL 以
 * 1064 语法错误直接拒绝 —— 下单接口 {@code POST /pay} 与支付回调
 * {@code /pay/notify} 曾因此全部失败（表现为前端提示「后端服务异常 HTTP 500」）。
 *
 * <p>现已把库表重命名为 {@code pay_order}（保留字外）并在此显式绑定，
 * 请勿改回，也不要删掉这个注解。
 */
@Data
@TableName("pay_order")
public class Order {
    private String id; //唯一订单号
    private String user;
    private int spuId;

    private int status;  //0未完成，1待付款，2支付成功，3交易关闭，4交易完成
}
