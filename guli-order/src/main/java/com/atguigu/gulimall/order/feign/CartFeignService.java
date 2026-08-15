package com.atguigu.gulimall.order.feign;

import com.atguigu.gulimall.order.vo.OrderItemVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient("guli-cart")
public interface CartFeignService {

    @GetMapping("/cart/checkedItems")
    List<OrderItemVo> checkedItems();

    @PostMapping("/cart/deleteItem")
    void deleteCartItem(@RequestParam("skuId") Long skuId);
}