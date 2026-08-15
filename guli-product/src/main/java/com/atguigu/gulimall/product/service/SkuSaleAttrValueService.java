package com.atguigu.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gulimall.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.SkuSaleAttrValueEntity;
import com.atguigu.gulimall.product.vo.SpuSaleAttrVo;

import java.util.List;
import java.util.Map;

/**
 * @author fupengju
 * @email 3545485659@qq.com
 * @date 2026-07-05 17:59:52
 */
public interface SkuSaleAttrValueService extends IService<SkuSaleAttrValueEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<SpuSaleAttrVo> getSpuSaleAttrVoListBySpuId(Long spuId);

    List<String> querySaleAttrAsString(Long skuId);

}

