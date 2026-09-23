package org.example.airentplatform.demos.web.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("aitask")
public class AiTask {
    private int id;
    private String taskname;
    private String username;
    private String prompt;
    private String status;

    // AI 生成的诗文正文。消费者 RabbitMQConsumer 生成完成后写入，
    // /ai/status 轮询与 /user/aitask 查询都会把它带回前端。
    private String content;

    // 表 aitask 的该列名是驼峰 taskNo（不是 task_no），必须显式指定，
    // 否则 MyBatis-Plus 的下划线映射会生成 task_no 导致 Unknown column。
    // @TableField 只影响实体生成的 SQL 列名，代码里手写的 eq("taskNo", ...) 不受影响。
    @TableField("taskNo")
    private String taskNo;
}
