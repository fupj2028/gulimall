package com.atguigu.gulimall.order.feign;

import com.atguigu.gulimall.common.to.SkuSeckillInfoVo;
import com.atguigu.gulimall.order.vo.SkuOrderInfoVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@FeignClient("guli-product")
public interface ProductFeignService {

    @PostMapping("/product/skuinfo/prices")
    Map<Long, BigDecimal> getPrices(@RequestBody List<Long> skuIds);

    @PostMapping("/product/skuinfo/orderinfo")
    Map<Long, SkuOrderInfoVo> getSkuOrderInfos(@RequestBody List<Long> skuIds);

    @PostMapping("/product/skuinfo/seckillinfo")
    Map<Long, SkuSeckillInfoVo> getSeckillInfo(@RequestBody List<Long> skuIds);
}