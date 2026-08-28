package org.example.airentplatform.demos.web.pojo;

import lombok.Data;

@Data
public class AiTask {
    private int id;
    private String taskname;
    private String username;
    private String prompt;
    private String status;
    private String taskNo;
}
