package com.atguigu.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gulimall.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.AttrEntity;
import com.atguigu.gulimall.product.entity.ProductAttrValueEntity;
import com.atguigu.gulimall.product.vo.AttrVo;

import java.util.List;
import java.util.Map;

/**
 * @author fupengju
 * @email 3545485659@qq.com
 * @date 2026-07-05 17:59:52
 */
public interface AttrService extends IService<AttrEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveAttr(AttrVo vo);

    void updateAttr(AttrVo vo);

    AttrVo getAttrInfo(Long attrId);

    PageUtils queryPageByType(Map<String, Object> params, Long catelogId, String type);

    List<ProductAttrValueEntity> getProductAttrListBySpuId(Long spuId);

    void updateAttrBySpuId(List<ProductAttrValueEntity> entities, Long spuId);

    List<Long> getSearchAttrIds(List<Long> attrIds);
}

