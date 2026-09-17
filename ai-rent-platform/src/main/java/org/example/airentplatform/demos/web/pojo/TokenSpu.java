package org.example.airentplatform.demos.web.pojo;


import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("token_spu")
public class TokenSpu {


    @TableId
    private int id;

    private String name;

    private int money;

    private int rmb;

    private int status;//1代表上架，0代表下架


}
