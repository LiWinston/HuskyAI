package com.AI.Budgerigar.chatbot.result;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 后端统一返回结果
 * @param <T>
 */
@Data
public class Result<T> implements Serializable {

    private Integer code; //编码：1成功，0和其它数字为失败
    private String msg; //错误信息
    private T data; //数据

    public static <T> Result<T> success() {
        Result<T> result = new Result<T>();
        result.code = 1;
        return result;
    }

    public static <T> Result<T> success(T object) {
        Result<T> result = new Result<T>();
        result.data = object;
        result.code = 1;
        return result;
    }

    public static <T> Result<T> error(String msg) {
        Result result = new Result();
        result.msg = msg;
        result.code = 0;
        return result;
    }

    // 新增方法：用于处理参数验证错误
    public static <T> Result<T> error(String msg, Map<String, String> errors) {
        Result<T> result = new Result<>();
        result.msg = msg;
        result.code = 0;
        result.data = (T) errors; // 将错误详情作为数据返回
        return result;
    }

}
