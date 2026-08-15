package com.atguigu.gulimall.seckill.feign;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import com.atguigu.gulimall.common.to.SeckillSessionWithSkusTo;

@FeignClient("guli-coupon")
public interface CouponFeignService {

    @GetMapping("/coupon/seckillsession/in3days")
    List<SeckillSessionWithSkusTo> getSeckillSessionsIn3Days();
}
