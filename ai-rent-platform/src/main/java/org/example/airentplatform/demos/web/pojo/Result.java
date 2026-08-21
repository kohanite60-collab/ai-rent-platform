package org.example.airentplatform.demos.web.pojo;
import lombok.Data;

@Data
public class Result<T> {

    private Integer code;
    private String msg;
    private T data;

    public Result() {}

    public Result(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    // 成功
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    // 成功（无数据）
    public static <T> Result<T> success() {
        return new Result<>(200, "success", null);
    }

    // 失败
    public static <T> Result<T> error(String msg) {
        return new Result<>(500, msg, null);
    }

    // getter / setter 自己生成
}