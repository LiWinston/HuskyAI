package com.AI.Budgerigar.chatbot.result;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Map;

/**
 * Unified backend return result.
 *
 * @param <T>
 */
@Data
@Getter
@Setter
public class Result<T> implements Serializable {

    private Integer code; // Code: 1 for success, 0 and other numbers for failure.

    private String msg; // error message

    private T data; // data

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

    public static <T> Result<T> success(T object, String msg) {
        Result<T> result = new Result<T>();
        result.data = object;
        result.code = 1;
        result.msg = msg;
        return result;
    }

    public static <T> Result<T> error(String msg) {
        Result result = new Result();
        result.msg = msg;
        result.code = 0;
        return result;
    }

    public static <T> Result<T> error(T object, String msg) {
        Result<T> result = new Result<T>();
        result.data = object;
        result.msg = msg;
        result.code = 0;
        return result;
    }

    // New method: for handling parameter validation errors.
    public static <T> Result<T> error(String msg, Map<String, String> errors) {
        Result<T> result = new Result<>();
        result.msg = msg;
        result.code = 0;
        result.data = (T) errors; // Return the error details as data.
        return result;
    }

}
