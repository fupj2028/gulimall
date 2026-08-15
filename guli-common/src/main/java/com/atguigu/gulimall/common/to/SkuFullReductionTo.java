package com.atguigu.gulimall.common.to;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SkuFullReductionTo {
    private Long skuId;
    private BigDecimal fullPrice;
    private BigDecimal reducePrice;
    private Integer addOther;
}
