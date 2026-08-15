package com.atguigu.gulimall.common.to;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MemberPriceTo {
    private Long skuId;
    private Long memberLevelId;
    private String memberLevelName;
    private BigDecimal memberPrice;
}
