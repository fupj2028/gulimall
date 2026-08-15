package com.atguigu.gulimall.ware.vo;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class PurchaseDetailDoneVo {

    private Long id;

    private Integer status;

    private String message;

    private Integer total;

    private BigDecimal price;
}
