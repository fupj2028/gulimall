package com.atguigu.gulimall.order.feign;

import com.atguigu.gulimall.common.to.SkuHasStockVo;
import com.atguigu.gulimall.common.to.StockLockedTo;
import com.atguigu.gulimall.common.to.WareSkuLockVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("guli-ware")
public interface WareFeignService {

    @PostMapping("/ware/waresku/hasstock")
    List<SkuHasStockVo> getSkuHasStock(@RequestBody List<Long> skuIds);

    @PostMapping("/ware/waresku/lockstock")
    StockLockedTo lockStock(@RequestBody WareSkuLockVo wareSkuLockVo);
}