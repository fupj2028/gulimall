package com.atguigu.gulimall.common.Exception;

public enum ErrorCode {
    InvalidParameter(10000, "参数错误"),
    UnknownError(10001, "未知错误"),
    PRODUCT_UP_ERROR(11000, "商品上架失败"),
    PRODUCT_UP_PARTIAL_ERROR(11001, "部分SKU索引失败");

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
