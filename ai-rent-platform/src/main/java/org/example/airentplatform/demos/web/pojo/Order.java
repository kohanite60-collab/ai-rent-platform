package org.example.airentplatform.demos.web.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain=true)
public class Order {
    private int id;
    private String user;
    private String product;
    private int rmb;
    private int money;
    private String status;

}
