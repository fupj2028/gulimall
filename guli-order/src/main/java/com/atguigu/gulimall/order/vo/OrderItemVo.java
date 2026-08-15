package com.atguigu.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderItemVo {

    private Long skuId;

    private Boolean check = true;
    
    private String title;

    private String image;

    private Integer count;

    private BigDecimal price;

    private List<String> attrs;

}
