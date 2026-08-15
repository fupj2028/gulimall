package com.atguigu.gulimall.product.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gulimall.common.utils.PageUtils;
import com.atguigu.gulimall.common.utils.Query;


import com.atguigu.gulimall.product.dao.BrandDao;
import com.atguigu.gulimall.product.dao.CategoryBrandRelationDao;
import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.BrandEntity;
import com.atguigu.gulimall.product.entity.CategoryBrandRelationEntity;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.atguigu.gulimall.product.vo.BrandSelectVo;

import lombok.RequiredArgsConstructor;


@Service("categoryBrandRelationService")
@RequiredArgsConstructor
public class CategoryBrandRelationServiceImpl extends ServiceImpl<CategoryBrandRelationDao, CategoryBrandRelationEntity> implements CategoryBrandRelationService {

    private final BrandDao brandDao;
    private final CategoryDao categoryDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryBrandRelationEntity> page = this.page(
                new Query<CategoryBrandRelationEntity>().getPage(params),
                new QueryWrapper<CategoryBrandRelationEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    @Transactional
    public void saveDetail(Long brandId, Long catelogId) {
        long count = this.count(new QueryWrapper<CategoryBrandRelationEntity>()
                .eq("brand_id", brandId)
                .eq("catelog_id", catelogId));
        if (count > 0) {
            throw new IllegalArgumentException("该品牌已关联此分类，请勿重复添加");
        }

        BrandEntity brand = brandDao.selectById(brandId);
        CategoryEntity category = categoryDao.selectById(catelogId);
        if (brand == null) {
            throw new IllegalArgumentException("品牌不存在");
        }
        if (category == null) {
            throw new IllegalArgumentException("分类不存在");
        }

        CategoryBrandRelationEntity entity = new CategoryBrandRelationEntity();
        entity.setBrandId(brandId);
        entity.setCatelogId(catelogId);
        entity.setBrandName(brand.getName());
        entity.setCatelogName(category.getName());
        this.save(entity);
    }

    @Override
    public void updateBrandName(Long brandId, String brandName) {
        baseMapper.updateBrandName(brandId, brandName);
    }

    @Override
    public void updateCatelogName(Long catelogId, String catelogName) {
        baseMapper.updateCatelogName(catelogId, catelogName);
    }

    @Override
    public List<CategoryBrandRelationEntity> listByBrandId(Long brandId) {
        return this.list(new QueryWrapper<CategoryBrandRelationEntity>().eq("brand_id", brandId));
    }

    @Override
    public List<BrandSelectVo> listBrandsByCatelogId(Long catelogId) {
        List<CategoryBrandRelationEntity> relations = this.list(
                new QueryWrapper<CategoryBrandRelationEntity>().eq("catelog_id", catelogId));
        return relations.stream()
                .map(r -> new BrandSelectVo(r.getBrandId(), r.getBrandName()))
                .collect(Collectors.toList());
    }

}