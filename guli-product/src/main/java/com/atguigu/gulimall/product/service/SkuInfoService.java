package com.atguigu.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gulimall.common.utils.PageUtils;
import com.atguigu.gulimall.common.to.SkuSeckillInfoVo;
import com.atguigu.gulimall.product.entity.SkuInfoEntity;
import com.atguigu.gulimall.product.vo.SkuOrderInfoVo;

import java.util.List;
import java.util.Map;

/**
 * sku信息
 *
 * @author fupengju
 * @email 3545485659@qq.com
 * @date 2026-07-05 17:59:52
 */
public interface SkuInfoService extends IService<SkuInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryPageByCondition(Map<String,Object> params);

    Map<Long, SkuOrderInfoVo> getOrderSkuInfo(List<Long> skuIds);

    Map<Long, SkuSeckillInfoVo> getSeckillInfo(List<Long> skuIds);
}

