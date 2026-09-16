package org.example.airentplatform.demos.web.pojo;


import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ai_production")
public class AiProduction {

    @TableId
    private int id; //唯一主键
    private int sortid; //分类id

    private String user; //作者

    private String title;
    private String data;
    private int view;   //浏览量，用于按热度排序
}
