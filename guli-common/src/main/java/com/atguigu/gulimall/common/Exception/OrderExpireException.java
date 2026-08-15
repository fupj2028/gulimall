package com.atguigu.gulimall.common.Exception;

/**
 * 订单过期异常
 */
public class OrderExpireException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public OrderExpireException() {
        super("订单已过期");
    }

    public OrderExpireException(String message) {
        super(message);
    }
}
