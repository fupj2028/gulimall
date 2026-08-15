package com.atguigu.gulimall.auth.feign;

import com.atguigu.gulimall.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("third-party")
public interface SmsFeignService {

    @PostMapping("/sms/send")
    R sendSms(@RequestParam("phone") String phone, @RequestParam("code") String code);
}
