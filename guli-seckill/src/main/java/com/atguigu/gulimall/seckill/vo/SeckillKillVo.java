package com.atguigu.gulimall.seckill.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SeckillKillVo {
    private Long sessionId;
    private Long skuId;
    private BigDecimal seckillPrice;
    private Integer num;
    private Long payDeadline;
}