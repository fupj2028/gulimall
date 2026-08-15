package com.atguigu.gulimall.common.to;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SeckillSkuRelationTo {
    private Long id;
    private Long promotionId;
    private Long promotionSessionId;
    private Long skuId;
    private BigDecimal seckillPrice;
    private BigDecimal seckillCount;
    private BigDecimal seckillLimit;
    private Integer seckillSort;
}
