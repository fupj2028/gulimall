package com.atguigu.gulimall.search.feign;

import com.atguigu.gulimall.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("third-party")
public interface ThirdPartyFeignService {

    @RequestMapping("/cos/access")
    R access(@RequestParam("key") String key);
}
