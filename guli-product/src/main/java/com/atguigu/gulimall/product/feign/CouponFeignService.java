package com.atguigu.gulimall.product.feign;

import com.atguigu.gulimall.common.to.MemberPriceTo;
import com.atguigu.gulimall.common.to.SkuFullReductionTo;
import com.atguigu.gulimall.common.to.SkuLadderTo;
import com.atguigu.gulimall.common.to.SpuBoundsTo;
import com.atguigu.gulimall.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("guli-coupon")
public interface CouponFeignService {

    @PostMapping("/coupon/spubounds/save")
    R saveSpuBounds(@RequestBody SpuBoundsTo spuBounds);

    @PostMapping("/coupon/skufullreduction/saveBatch")
    R saveSkuFullReductions(@RequestBody List<SkuFullReductionTo> reductions);

    @PostMapping("/coupon/skuladder/saveBatch")
    R saveSkuLadders(@RequestBody List<SkuLadderTo> ladders);

    @PostMapping("/coupon/memberprice/saveBatch")
    R saveMemberPrices(@RequestBody List<MemberPriceTo> prices);
}
