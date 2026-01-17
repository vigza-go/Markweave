package com.vigza.markweave.common;


import lombok.Data;

@Data
public class Result<T> {
    private Integer code; // 状态码
    private String message;
    private T data; // 具体数据负载
    private long timestamp ;


    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public static <T>  Result<T> success(T data) {
        return new Result<T>(200,"操作成功",data);
    }

    public static <T>  Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<T>(code, message, null);
    }
}
