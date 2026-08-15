package com.atguigu.gulimall.auth.feign;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.atguigu.gulimall.common.utils.R;

@FeignClient("guli-member")
public interface MemberFeignService {

    @PostMapping("/member/member/save")
    R save(@RequestBody Map<String, Object> params);

    @PostMapping("/member/member/login")
    R login(@RequestBody Map<String, Object> params);

    @PostMapping("/member/member/register")
    R register(@RequestBody Map<String, Object> params);

    @GetMapping("/member/member/findByUsername")
    R findByUsername(@RequestParam("username") String username);
}
