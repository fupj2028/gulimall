package com.atguigu.gulimall.seckill.to;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class SeckillSkuRedisTo implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long sessionId;
    private Long skuId;
    private BigDecimal seckillPrice;
    private BigDecimal seckillCount;
    private BigDecimal seckillLimit;
    private Integer seckillSort;
    private String randomCode;
    private Date startTime;
    private Date endTime;
}
