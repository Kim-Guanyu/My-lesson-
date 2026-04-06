package com.mdkj.util;

import lombok.Data;

@Data
public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    public Result() {
    }

    public Result(ResultCode resultCode, String message) {
        this.code = resultCode.getCODE();
        this.message = message;
    }

    public Result(ResultCode resultCode) {
        this.code = resultCode.getCODE();
        this.message = resultCode.getMESSAGE();
    }

    public Result(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.getCODE());
        result.setMessage(ResultCode.SUCCESS.getMESSAGE());
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error(ResultCode resultCode) {
        Result<T> result = new Result<>();
        result.setCode(resultCode.getCODE());
        result.setMessage(resultCode.getMESSAGE());
        return result;
    }

    public static <T> Result<T> error(ResultCode resultCode, String message) {
        Result<T> result = new Result<>();
        result.setCode(resultCode.getCODE());
        result.setMessage(message);
        return result;
    }

    public Result(T data) {
        this.code = ResultCode.SUCCESS.getCODE();
        this.message = ResultCode.SUCCESS.getMESSAGE();
        this.data = data;
    }
}
