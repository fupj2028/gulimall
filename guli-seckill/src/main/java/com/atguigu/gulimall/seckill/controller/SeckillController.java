package com.atguigu.gulimall.seckill.controller;

import com.atguigu.gulimall.common.utils.R;
import com.atguigu.gulimall.seckill.exception.SeckillException;
import com.atguigu.gulimall.seckill.service.SeckillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    @GetMapping("/kill/current")
    public R current() {
        return R.ok().put("data", seckillService.currentSeckillSkus());
    }

    @GetMapping("/kill/sku/{skuId}")
    public R sku(@PathVariable("skuId") Long skuId,
                 @RequestParam(value = "session", required = false) Long sessionId) {
        return R.ok().put("data", seckillService.seckillSkuInfo(skuId, sessionId));
    }

    @PostMapping("/kill")
    public R kill(@RequestParam("sessionId") Long sessionId,
                  @RequestParam("skuId") Long skuId,
                  @RequestParam("code") String code,
                  @RequestParam(value = "num", defaultValue = "1") Integer num) {
        try {
            return R.ok().put("data", seckillService.kill(sessionId, skuId, code, num));
        } catch (SeckillException e) {
            return R.error(e.getMessage());
        }
    }
}
