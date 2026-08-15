package com.atguigu.gulimall.common.to;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SkuLadderTo {
    private Long skuId;
    private Integer fullCount;
    private BigDecimal discount;
    private BigDecimal price;
    private Integer addOther;
}
