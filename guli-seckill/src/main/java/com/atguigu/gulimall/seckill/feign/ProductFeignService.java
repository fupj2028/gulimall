package com.atguigu.gulimall.seckill.feign;

import com.atguigu.gulimall.common.to.SkuSeckillInfoVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient("guli-product")
public interface ProductFeignService {

    @PostMapping("/product/skuinfo/seckillinfo")
    Map<Long, SkuSeckillInfoVo> getSeckillInfo(@RequestBody List<Long> skuIds);
}
