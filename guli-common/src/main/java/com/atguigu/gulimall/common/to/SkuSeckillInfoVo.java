package com.atguigu.gulimall.common.to;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SkuSeckillInfoVo {
    private Long skuId;
    private String skuName;
    private String skuDefaultImg;
    private BigDecimal price;
}