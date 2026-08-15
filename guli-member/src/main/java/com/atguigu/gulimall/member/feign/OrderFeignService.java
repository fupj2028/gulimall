package com.atguigu.gulimall.member.feign;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.atguigu.gulimall.common.vo.MemberOrderVo;

@FeignClient("guli-order")
public interface OrderFeignService {
    @RequestMapping("/order/order/list/{memberId}")
    List<MemberOrderVo> listByMember(@PathVariable("memberId") Long memberId);
}
