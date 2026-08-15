package com.atguigu.gulimall.common.Exception;

/**
 * 库存不足异常
 */
public class NoStockException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private Long skuId;

    public NoStockException(Long skuId) {
        super("商品库存不足, skuId=" + skuId);
        this.skuId = skuId;
    }

    public Long getSkuId() {
        return skuId;
    }
}
