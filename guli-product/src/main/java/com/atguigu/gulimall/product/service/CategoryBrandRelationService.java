package com.atguigu.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gulimall.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.CategoryBrandRelationEntity;
import com.atguigu.gulimall.product.vo.BrandSelectVo;

import java.util.List;
import java.util.Map;

/**
 * 品牌分类关联
 *
 * @author fupengju
 * @email 3545485659@qq.com
 * @date 2026-07-05 17:59:52
 */
public interface CategoryBrandRelationService extends IService<CategoryBrandRelationEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveDetail(Long brandId, Long catelogId);

    void updateBrandName(Long brandId, String brandName);

    void updateCatelogName(Long catelogId, String catelogName);

    List<CategoryBrandRelationEntity> listByBrandId(Long brandId);

    List<BrandSelectVo> listBrandsByCatelogId(Long catelogId);
}

