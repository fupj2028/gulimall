package com.atguigu.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gulimall.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.CategoryEntity;

import com.atguigu.gulimall.product.vo.Catalog2Vo;
import java.util.List;
import java.util.Map;

/**
 * 商品三级分类
 *
 * @author fupengju
 * @email 3545485659@qq.com
 * @date 2026-07-05 17:59:52
 */
public interface CategoryService extends IService<CategoryEntity> {

    PageUtils queryPage(Map<String, Object> params);
    List<CategoryEntity> listWithTree();
    List<CategoryEntity> listLevel1();
    void removeMenuByIds(List<Long> catIds);
    Long[] findCatelogPath(Long catelogId);
    Map<String, List<Catalog2Vo>> getCatalogJson();
}

