package com.atguigu.gulimall.seckill.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SeckillSkuVo {
    private Long sessionId;
    private Long skuId;
    private String skuName;
    private String skuDefaultImg;
    private BigDecimal price;
    private BigDecimal seckillPrice;
    private BigDecimal seckillCount;
    private BigDecimal seckillLimit;
    private Long startTime;
    private Long endTime;
    private String randomCode;
}
