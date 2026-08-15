package com.atguigu.gulimall.product.feign;

import com.atguigu.gulimall.common.to.SkuHasStockVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("guli-ware")
public interface WareFeignService {

    @PostMapping("/ware/waresku/hasstock")
    List<SkuHasStockVo> getSkuHasStock(@RequestBody List<Long> skuIds);
}
