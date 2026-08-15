package com.atguigu.gulimall.product.vo;

import java.util.List;

import lombok.Data;

@Data
public class SpuSaleAttrVo {
    private Long attrId;
    private String saleAttrName;
    private List<String> spuSaleAttrValueList;
}
