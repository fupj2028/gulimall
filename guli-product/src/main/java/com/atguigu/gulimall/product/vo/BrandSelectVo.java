package com.atguigu.gulimall.product.vo;

import lombok.Data;

@Data
public class BrandSelectVo {
    private Long brandId;
    private String brandName;

    public BrandSelectVo(Long brandId, String brandName) {
        this.brandId = brandId;
        this.brandName = brandName;
    }
}
