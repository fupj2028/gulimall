package com.atguigu.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ConfirmItemVo {

    private List<MemberAddressVo> memberAddresses;

    private List<OrderItemVo> items;

    private Integer integration;

    private BigDecimal total;

    private BigDecimal payPrice;

    private Map<Long, Boolean> stockMap;

    private FareVo fareVo;

    private String orderToken;
}
