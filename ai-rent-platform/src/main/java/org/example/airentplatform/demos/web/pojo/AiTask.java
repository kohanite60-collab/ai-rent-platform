package org.example.airentplatform.demos.web.pojo;

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
    private String taskNo;
}
