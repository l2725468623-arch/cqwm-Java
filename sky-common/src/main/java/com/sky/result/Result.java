package com.sky.result;
import java.io.Serializable;
import lombok.Data;
@Data
public class Result<T> implements Serializable {
    private Integer code;
    private String msg;
    private T data;
    public static <T> Result<T> success() { return success(null); }
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.code = 1;
        result.data = data;
        return result;
    }
    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.code = 0;
        result.msg = message;
        return result;
    }
}
