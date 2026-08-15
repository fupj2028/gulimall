package com.atguigu.gulimall.product.vo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.entity.SkuImagesEntity;
import com.atguigu.gulimall.product.entity.SpuInfoDescEntity;

import lombok.Data;

@Data
public class SkuItemVo {
    private Long skuId;
    private String skuName;
    private String skuDefaultImg;
    private BigDecimal price;
    private BigDecimal weight;
    private List<SkuImagesEntity> skuImageList;

    private String brandName;
    private String spuName;
    private String skuDesc;
    private List<CategoryEntity> catelogPath;

    private List<SpuSaleAttrVo> saleAttrs;
    private SpuInfoDescEntity spuDesc;
    private List<String> decriptImages;
    private Map<Long, String> currentSkuSaleAttrValues;
    private String valuesSku;
    private List<SpuItemAttrGroupVo> attrGroups;
}
