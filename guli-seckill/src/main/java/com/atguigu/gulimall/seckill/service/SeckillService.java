package com.atguigu.gulimall.seckill.service;

import com.atguigu.gulimall.seckill.vo.SeckillKillVo;
import com.atguigu.gulimall.seckill.vo.SeckillSkuVo;

import java.util.List;

public interface SeckillService {

    List<SeckillSkuVo> currentSeckillSkus();

    SeckillSkuVo seckillSkuInfo(Long skuId, Long sessionId);

    SeckillKillVo kill(Long sessionId, Long skuId, String code, Integer num);
}
