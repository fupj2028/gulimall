package com.atguigu.gulimall.cart.feign;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.atguigu.gulimall.common.utils.R;

@FeignClient("guli-product")
public interface ProductFeignService {

    @GetMapping("/product/skuinfo/info/{skuId}")
    public R info(@PathVariable("skuId") Long skuId);

    @GetMapping("/product/skusaleattrvalue/stringlist/{skuId}")
    public List<String> stringList(@PathVariable("skuId") Long skuId);
}
