package com.atguigu.gulimall.common.to.es;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class SkuEsModel {

    private Long spuId;

    private Long skuId;

    private String skuTitle;

    private BigDecimal skuPrice;

    private String skuImage;

    private Long saleCount;

    private Boolean hasStock;

    private Long hotScore;

    private Long brandId;

    private Long catelogId;

    private String brandName;

    private String brandImage;

    private String catalogName;

    private List<Attr> attrs;

    @Data
    public static class Attr{
        private Long attrId;

        private String attrName;

        private String attrValue;
    }

}
