package com.atguigu.gulimall.order.vo;

import java.math.BigDecimal;
import java.util.List;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.entity.OrderItemEntity;

import lombok.Data;

/**
 * OrderCreateTo
 */
@Data
public class OrderCreateTo {

    private String orderSn;

    private List<OrderItemEntity> orders;

    private BigDecimal payload;  //应付总额

    private Integer fare;   //运费

    private OrderEntity orderEntity;   //订单主表

}
