package com.atguigu.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gulimall.common.utils.PageUtils;
import com.atguigu.gulimall.ware.entity.WareSkuEntity;

import com.atguigu.gulimall.common.to.SkuHasStockVo;
import com.atguigu.gulimall.common.to.StockLockedTo;
import com.atguigu.gulimall.common.to.WareSkuLockVo;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author fupengju
 * @email 3545485659@qq.com
 * @date 2026-07-05 17:58:54
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void addStock(Long skuId, Long wareId, Integer skuNum);

    List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds);

    StockLockedTo lockStock(WareSkuLockVo wareSkuLockVo);

    void unlockStock(StockLockedTo to);

    void deductStock(StockLockedTo to);
}

