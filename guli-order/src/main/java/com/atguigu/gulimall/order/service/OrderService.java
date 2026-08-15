package com.atguigu.gulimall.order.service;

import com.atguigu.gulimall.common.to.StockLockedTo;
import com.atguigu.gulimall.common.vo.MemberOrderVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gulimall.common.utils.PageUtils;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.vo.ConfirmItemVo;
import com.atguigu.gulimall.order.vo.FareVo;
import com.atguigu.gulimall.order.vo.OrderSubmitVo;
import com.atguigu.gulimall.order.vo.PayAsyncVo;
import com.atguigu.gulimall.order.vo.PayVo;
import com.atguigu.gulimall.order.vo.SubmitOrderResponseVo;

import java.util.List;
import java.util.Map;

public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);

    ConfirmItemVo confirmOrder();

    ConfirmItemVo confirmSeckillOrder(Long sessionId, Long skuId, Integer num);

    FareVo getFare(Long addressId, Long memberId);

    SubmitOrderResponseVo submitOrder(OrderSubmitVo submitVo);

    void closeOrder(StockLockedTo to);

    Integer getOrderStatus(String orderSn);

    void handlePayNotify(PayAsyncVo payAsyncVo);

    void closeOrderByAlipay(String orderSn);

    List<MemberOrderVo> listOrdersByMember(Long memberId);

    PayVo getPayVo(String orderSn);
}

