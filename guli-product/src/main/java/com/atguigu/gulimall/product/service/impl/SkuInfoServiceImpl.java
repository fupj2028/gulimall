package com.atguigu.gulimall.product.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gulimall.common.utils.PageUtils;
import com.atguigu.gulimall.common.utils.Query;

import com.atguigu.gulimall.product.dao.SkuInfoDao;
import com.atguigu.gulimall.product.dao.SpuInfoDao;
import com.atguigu.gulimall.product.entity.BrandEntity;
import com.atguigu.gulimall.product.entity.SkuInfoEntity;
import com.atguigu.gulimall.product.entity.SpuInfoEntity;
import com.atguigu.gulimall.product.service.BrandService;
import com.atguigu.gulimall.product.service.SkuInfoService;
import com.atguigu.gulimall.common.to.SkuSeckillInfoVo;
import com.atguigu.gulimall.product.vo.SkuOrderInfoVo;


@Service("skuInfoService")
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoDao, SkuInfoEntity> implements SkuInfoService {

    @Autowired
    private SpuInfoDao spuInfoDao;

    @Autowired
    private BrandService brandService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                new QueryWrapper<SkuInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SkuInfoEntity> wrapper = new QueryWrapper<>();

        String key = (String) params.get("key");
        if (StringUtils.hasText(key)) {
            wrapper.and(w -> w.eq("sku_id", key).or().like("sku_name", key));
        }

        String catelogId = (String) params.get("catelogId");
        if (StringUtils.hasText(catelogId)&&!"0".equalsIgnoreCase(catelogId)) {
            wrapper.eq("catalog_id", catelogId);
        }

        String brandId = (String) params.get("brandId");
        if (StringUtils.hasText(brandId) && !"0".equals(brandId)) {
            wrapper.eq("brand_id", brandId);
        }

        String min = (String) params.get("min");
        if (StringUtils.hasText(min)) {
            wrapper.ge("price", min);
        }

        String max = (String) params.get("max");
        if (StringUtils.hasText(max) && !"0".equals(max)) {
            wrapper.le("price", max);
        }

        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Override
    public Map<Long, SkuOrderInfoVo> getOrderSkuInfo(List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SkuInfoEntity> skus = this.listByIds(skuIds);
        List<Long> spuIds = skus.stream().map(SkuInfoEntity::getSpuId).distinct().collect(Collectors.toList());
        List<Long> brandIds = skus.stream().map(SkuInfoEntity::getBrandId).distinct().collect(Collectors.toList());
        Map<Long, SpuInfoEntity> spuMap = spuInfoDao.selectBatchIds(spuIds).stream()
                .collect(Collectors.toMap(SpuInfoEntity::getId, e -> e));
        Map<Long, BrandEntity> brandMap = brandService.listByIds(brandIds).stream()
                .collect(Collectors.toMap(BrandEntity::getBrandId, e -> e));
        return skus.stream().collect(Collectors.toMap(SkuInfoEntity::getSkuId, sku -> {
            SkuOrderInfoVo vo = new SkuOrderInfoVo();
            vo.setSkuId(sku.getSkuId());
            vo.setSpuId(sku.getSpuId());
            vo.setCategoryId(sku.getCatalogId());
            SpuInfoEntity spu = spuMap.get(sku.getSpuId());
            if (spu != null) {
                vo.setSpuName(spu.getSpuName());
            }
            BrandEntity brand = brandMap.get(sku.getBrandId());
            if (brand != null) {
                vo.setSpuBrand(brand.getName());
            }
            return vo;
        }));
    }

    @Override
    public Map<Long, SkuSeckillInfoVo> getSeckillInfo(List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SkuInfoEntity> skus = this.listByIds(skuIds);
        return skus.stream().collect(Collectors.toMap(SkuInfoEntity::getSkuId, sku -> {
            SkuSeckillInfoVo vo = new SkuSeckillInfoVo();
            vo.setSkuId(sku.getSkuId());
            vo.setSkuName(sku.getSkuName());
            vo.setSkuDefaultImg(sku.getSkuDefaultImg());
            vo.setPrice(sku.getPrice());
            return vo;
        }));
    }


}