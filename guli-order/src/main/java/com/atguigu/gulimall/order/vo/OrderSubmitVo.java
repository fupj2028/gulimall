package com.atguigu.gulimall.order.vo;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class OrderSubmitVo {

    private Long addrId; //地址id

    private String payType;  //支付方式

    //无需提交需要购买的商品，去购物车再获取一遍

    private String orderToken;  //防重令牌

    private BigDecimal payPrice;  //应付总额，验价格

    //秒杀下单参数（非秒杀下单时为 null）
    private Long sessionId;

    private Long skuId;

    private Integer num;

    //用户相关信息直接去session中获取
}
