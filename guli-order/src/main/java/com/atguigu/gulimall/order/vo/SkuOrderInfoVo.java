package com.atguigu.gulimall.order.vo;

import lombok.Data;

@Data
public class SkuOrderInfoVo {
    private Long skuId;
    private Long spuId;
    private String spuName;
    private String spuBrand;
    private Long categoryId;
}
