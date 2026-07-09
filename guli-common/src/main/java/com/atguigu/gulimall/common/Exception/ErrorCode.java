package com.atguigu.gulimall.common.Exception;

public enum ErrorCode {
    InvalidParameter(10000, "参数错误"),
    UnknownError(10001, "未知错误");

    private final int code;
    private final String message;
    
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
