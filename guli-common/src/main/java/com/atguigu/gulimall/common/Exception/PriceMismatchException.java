package com.atguigu.gulimall.common.Exception;

/**
 * 验价不一致异常
 */
public class PriceMismatchException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public PriceMismatchException() {
        super("商品价格可能有更新");
    }

    public PriceMismatchException(String message) {
        super(message);
    }
}
