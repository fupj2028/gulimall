package com.atguigu.gulimall.product.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gulimall.common.constant.ProductConstant;
import com.atguigu.gulimall.common.to.MemberPriceTo;
import com.atguigu.gulimall.common.to.SkuFullReductionTo;
import com.atguigu.gulimall.common.to.SkuHasStockVo;
import com.atguigu.gulimall.common.to.SkuLadderTo;
import com.atguigu.gulimall.common.to.SpuBoundsTo;
import com.atguigu.gulimall.common.to.es.SkuEsModel;
import com.atguigu.gulimall.common.utils.PageUtils;
import com.atguigu.gulimall.common.utils.Query;
import com.atguigu.gulimall.common.utils.R;
import com.atguigu.gulimall.product.dao.SpuInfoDao;
import com.atguigu.gulimall.product.entity.AttrEntity;
import com.atguigu.gulimall.product.entity.ProductAttrValueEntity;
import com.atguigu.gulimall.product.entity.SkuImagesEntity;
import com.atguigu.gulimall.product.entity.SkuInfoEntity;
import com.atguigu.gulimall.product.entity.SkuSaleAttrValueEntity;
import com.atguigu.gulimall.product.entity.BrandEntity;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.entity.SpuInfoDescEntity;
import com.atguigu.gulimall.product.entity.SpuInfoEntity;
import com.atguigu.gulimall.product.entity.SpuImagesEntity;
import com.atguigu.gulimall.product.feign.CouponFeignService;
import com.atguigu.gulimall.product.feign.SearchFeignService;
import com.atguigu.gulimall.product.feign.WareFeignService;
import com.atguigu.gulimall.product.service.AttrService;
import com.atguigu.gulimall.product.service.BrandService;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.service.ProductAttrValueService;
import com.atguigu.gulimall.product.service.SkuImagesService;
import com.atguigu.gulimall.product.service.SkuInfoService;
import com.atguigu.gulimall.product.service.SkuSaleAttrValueService;
import com.atguigu.gulimall.product.service.SpuInfoDescService;
import com.atguigu.gulimall.product.service.SpuInfoService;
import com.atguigu.gulimall.product.service.SpuImagesService;
import com.atguigu.gulimall.product.vo.BaseAttrVo;
import com.atguigu.gulimall.product.vo.MemberPriceVo;
import com.atguigu.gulimall.product.vo.SkuImageVo;
import com.atguigu.gulimall.product.vo.SkuSaleAttrVo;
import com.atguigu.gulimall.product.vo.SkuVo;
import com.atguigu.gulimall.product.vo.SpuSaveVo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("spuInfoService")
@RequiredArgsConstructor
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    private final SpuInfoDescService spuInfoDescService;
    private final SpuImagesService spuImagesService;
    private final ProductAttrValueService productAttrValueService;
    private final AttrService attrService;
    private final SkuInfoService skuInfoService;
    private final SkuImagesService skuImagesService;
    private final SkuSaleAttrValueService skuSaleAttrValueService;
    private final CouponFeignService couponFeignService;
    private final BrandService brandService;
    private final CategoryService categoryService;
    private final SearchFeignService searchFeignService;
    private final WareFeignService wareFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    @Transactional
    public void saveSpuInfo(SpuSaveVo vo) {
        // 1. 保存 pms_spu_info
        SpuInfoEntity spu = new SpuInfoEntity();
        spu.setSpuName(vo.getSpuName());
        spu.setSpuDescription(vo.getSpuDescription());
        spu.setCatalogId(vo.getCatalogId());
        spu.setBrandId(vo.getBrandId());
        spu.setWeight(vo.getWeight());
        spu.setPublishStatus(vo.getPublishStatus() != null ? vo.getPublishStatus() : ProductConstant.StatusEnum.NEW_SPU.getCode());
        spu.setCreateTime(new Date());
        spu.setUpdateTime(new Date());
        this.save(spu);
        Long spuId = spu.getId();

        // 2. 保存 pms_spu_info_desc
        if (vo.getDecript() != null && !vo.getDecript().isEmpty()) {
            SpuInfoDescEntity desc = new SpuInfoDescEntity();
            desc.setSpuId(spuId);
            desc.setDecript(String.join(",", vo.getDecript()));
            spuInfoDescService.save(desc);
        }

        // 3. 保存 pms_spu_images
        if (vo.getImages() != null && !vo.getImages().isEmpty()) {
            List<SpuImagesEntity> images = vo.getImages().stream().map(key -> {
                SpuImagesEntity img = new SpuImagesEntity();
                img.setSpuId(spuId);
                img.setImgUrl(key);
                return img;
            }).collect(Collectors.toList());
            spuImagesService.saveBatch(images);
        }

        // 4. 保存 pms_product_attr_value (baseAttrs)
        if (vo.getBaseAttrs() != null && !vo.getBaseAttrs().isEmpty()) {
            List<Long> attrIds = vo.getBaseAttrs().stream()
                    .map(BaseAttrVo::getAttrId).collect(Collectors.toList());
            Map<Long, String> attrNameMap = attrService.listByIds(attrIds).stream()
                    .collect(Collectors.toMap(AttrEntity::getAttrId, AttrEntity::getAttrName));

            List<ProductAttrValueEntity> attrValues = vo.getBaseAttrs().stream().map(base -> {
                ProductAttrValueEntity pav = new ProductAttrValueEntity();
                pav.setSpuId(spuId);
                pav.setAttrId(base.getAttrId());
                pav.setAttrName(attrNameMap.get(base.getAttrId()));
                pav.setAttrValue(base.getAttrValues());
                pav.setQuickShow(base.getShowDesc());
                return pav;
            }).collect(Collectors.toList());
            productAttrValueService.saveBatch(attrValues);
        }

        // 5. 保存 sms_spu_bounds (Feign)
        if (vo.getBounds() != null && (vo.getBounds().getBuyBounds().compareTo(BigDecimal.ZERO) > 0
                || vo.getBounds().getGrowBounds().compareTo(BigDecimal.ZERO) > 0)) {
            SpuBoundsTo boundsTo = new SpuBoundsTo();
            boundsTo.setSpuId(spuId);
            boundsTo.setBuyBounds(vo.getBounds().getBuyBounds());
            boundsTo.setGrowBounds(vo.getBounds().getGrowBounds());
            R r = couponFeignService.saveSpuBounds(boundsTo);
            if ((int) r.get("code") != 0) {
                log.error("远程调用coupon保存spuBounds失败, spuId={}", spuId);
            }
        }

        // 6. 保存 SKU 级数据
        if (vo.getSkus() != null && !vo.getSkus().isEmpty()) {
            saveSkus(spuId, vo);
        }
    }

    private void saveSkus(Long spuId, SpuSaveVo vo) {
        List<SkuLadderTo> ladderTos = new ArrayList<>();
        List<SkuFullReductionTo> reductionTos = new ArrayList<>();
        List<MemberPriceTo> memberPriceTos = new ArrayList<>();

        for (SkuVo skuVo : vo.getSkus()) {
            // 6a. 保存 pms_sku_info
            SkuInfoEntity sku = new SkuInfoEntity();
            sku.setSpuId(spuId);
            sku.setSkuName(skuVo.getSkuName());
            sku.setSkuTitle(skuVo.getSkuTitle());
            sku.setSkuSubtitle(skuVo.getSkuSubtitle());
            sku.setPrice(skuVo.getPrice());
            sku.setCatalogId(vo.getCatalogId());
            sku.setBrandId(vo.getBrandId());
            sku.setSaleCount(0L);
            if (skuVo.getImages() != null) {
                skuVo.getImages().stream()
                        .filter(img -> img.getDefaultImg() != null && img.getDefaultImg() == 1)
                        .findFirst()
                        .ifPresent(img -> sku.setSkuDefaultImg(img.getImgUrl()));
            }
            skuInfoService.save(sku);
            Long skuId = sku.getSkuId();

            // 6b. 保存 pms_sku_images
            if (skuVo.getImages() != null && !skuVo.getImages().isEmpty()) {
                List<SkuImagesEntity> skuImages = skuVo.getImages().stream()
                        .filter(imgVo -> StringUtils.hasText(imgVo.getImgUrl()))
                        .map(imgVo -> {
                            SkuImagesEntity si = new SkuImagesEntity();
                            si.setSkuId(skuId);
                            si.setImgUrl(imgVo.getImgUrl());
                            si.setDefaultImg(imgVo.getDefaultImg());
                            return si;
                        }).collect(Collectors.toList());
                if (!skuImages.isEmpty()) {
                    skuImagesService.saveBatch(skuImages);
                }
            }

            // 6c. 保存 pms_sku_sale_attr_value
            if (skuVo.getAttr() != null && !skuVo.getAttr().isEmpty()) {
                List<SkuSaleAttrValueEntity> saleAttrs = skuVo.getAttr().stream().map(attrVo -> {
                    SkuSaleAttrValueEntity sa = new SkuSaleAttrValueEntity();
                    sa.setSkuId(skuId);
                    sa.setAttrId(attrVo.getAttrId());
                    sa.setAttrName(attrVo.getAttrName());
                    sa.setAttrValue(attrVo.getAttrValue());
                    return sa;
                }).collect(Collectors.toList());
                skuSaleAttrValueService.saveBatch(saleAttrs);
            }

            // 6d. sms_sku_ladder (countStatus=1 且数值有意义才存)
            if (skuVo.getCountStatus() != null && skuVo.getCountStatus() == 1
                    && skuVo.getFullCount() != null && skuVo.getFullCount() > 0
                    && skuVo.getDiscount() != null && skuVo.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
                SkuLadderTo ladder = new SkuLadderTo();
                ladder.setSkuId(skuId);
                ladder.setFullCount(skuVo.getFullCount());
                ladder.setDiscount(skuVo.getDiscount());
                ladder.setAddOther(1);
                ladderTos.add(ladder);
            }

            // 6e. sms_sku_full_reduction (priceStatus=1 且数值有意义才存)
            if (skuVo.getPriceStatus() != null && skuVo.getPriceStatus() == 1
                    && skuVo.getFullPrice() != null && skuVo.getFullPrice().compareTo(BigDecimal.ZERO) > 0
                    && skuVo.getReducePrice() != null && skuVo.getReducePrice().compareTo(BigDecimal.ZERO) > 0) {
                SkuFullReductionTo reduction = new SkuFullReductionTo();
                reduction.setSkuId(skuId);
                reduction.setFullPrice(skuVo.getFullPrice());
                reduction.setReducePrice(skuVo.getReducePrice());
                reduction.setAddOther(1);
                reductionTos.add(reduction);
            }

            // 6f. sms_member_price
            if (skuVo.getMemberPrice() != null && !skuVo.getMemberPrice().isEmpty()) {
                for (MemberPriceVo mp : skuVo.getMemberPrice()) {
                    if (mp.getPrice() == null || mp.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                        continue;
                    }
                    MemberPriceTo priceTo = new MemberPriceTo();
                    priceTo.setSkuId(skuId);
                    priceTo.setMemberLevelId(mp.getId());
                    priceTo.setMemberLevelName(mp.getName());
                    priceTo.setMemberPrice(mp.getPrice());
                    memberPriceTos.add(priceTo);
                }
            }
        }

        if (!ladderTos.isEmpty()) {
            R r = couponFeignService.saveSkuLadders(ladderTos);
            if ((int) r.get("code") != 0) {
                log.error("远程调用coupon保存skuLadder失败");
            }
        }
        if (!reductionTos.isEmpty()) {
            R r = couponFeignService.saveSkuFullReductions(reductionTos);
            if ((int) r.get("code") != 0) {
                log.error("远程调用coupon保存skuFullReduction失败");
            }
        }
        if (!memberPriceTos.isEmpty()) {
            R r = couponFeignService.saveMemberPrices(memberPriceTos);
            if ((int) r.get("code") != 0) {
                log.error("远程调用coupon保存memberPrice失败");
            }
        }
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SpuInfoEntity> wrapper = new QueryWrapper<>();

        String key = (String) params.get("key");
        if (StringUtils.hasText(key)) {
            wrapper.and(w -> w.eq("id", key).or().like("spu_name", key));
        }

        String status = (String) params.get("status");
        if (StringUtils.hasText(status)) {
            wrapper.eq("publish_status", status);
        }

        String brandId = (String) params.get("brandId");
        if (StringUtils.hasText(brandId) && !"0".equals(brandId)) {
            wrapper.eq("brand_id", brandId);
        }

        String catelogId = (String) params.get("catelogId");
        if (StringUtils.hasText(catelogId) && !"0".equals(catelogId)) {
            wrapper.eq("catalog_id", catelogId);
        }

        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Override
    @Transactional
    public void upSkus(Long spuId) {
        // 1. 检查 SPU
        SpuInfoEntity spu = this.getById(spuId);
        if (spu == null) {
            throw new IllegalArgumentException("SPU不存在，id=" + spuId);
        }
        if (spu.getPublishStatus() == ProductConstant.StatusEnum.SPU_UP.getCode()) {
            log.info("SPU已上架，跳过，spuId={}", spuId);
            return;
        }

        // 2. 获取所有 SKU
        List<SkuInfoEntity> skus = skuInfoService.list(
                new QueryWrapper<SkuInfoEntity>().eq("spu_id", spuId)
        );
        if (skus.isEmpty()) {
            throw new IllegalArgumentException("SPU没有SKU，无法上架，id=" + spuId);
        }

        // 3. 获取品牌和分类名称
        BrandEntity brand = brandService.getById(spu.getBrandId());
        String brandName = brand != null ? brand.getName() : "";
        String brandImage = brand != null ? brand.getLogo() : "";

        CategoryEntity category = categoryService.getById(spu.getCatalogId());
        String catalogName = category != null ? category.getName() : "";

        // 4. 获取可检索的基本属性
        List<ProductAttrValueEntity> baseAttrs = productAttrValueService.list(
                new QueryWrapper<ProductAttrValueEntity>().eq("spu_id", spuId)
        );
        List<Long> attrIds = baseAttrs.stream()
                .map(ProductAttrValueEntity::getAttrId).collect(Collectors.toList());
        List<Long> searchAttrIds = attrService.getSearchAttrIds(attrIds);
        List<SkuEsModel.Attr> searchAttrs = baseAttrs.stream()
                .filter(attr -> searchAttrIds.contains(attr.getAttrId()))
                .map(attr -> {
                    SkuEsModel.Attr a = new SkuEsModel.Attr();
                    a.setAttrId(attr.getAttrId());
                    a.setAttrName(attr.getAttrName());
                    a.setAttrValue(attr.getAttrValue());
                    return a;
                }).collect(Collectors.toList());

        // 5. 远程查询库存
        List<Long> skuIds = skus.stream().map(SkuInfoEntity::getSkuId).collect(Collectors.toList());
        List<SkuHasStockVo> stockVos;
        try {
            stockVos = wareFeignService.getSkuHasStock(skuIds);
        } catch (Exception e) {
            log.error("远程调用ware查询库存失败, spuId={}", spuId, e);
            stockVos = List.of();
        }
        Map<Long, Boolean> stockMap = stockVos.stream()
                .collect(Collectors.toMap(SkuHasStockVo::getSkuId, SkuHasStockVo::getHasStock));

        // 6. 构建 ES 模型列表
        List<SkuEsModel> esModels = skus.stream().map(sku -> {
            SkuEsModel model = new SkuEsModel();
            model.setSkuId(sku.getSkuId());
            model.setSpuId(spuId);
            model.setSkuTitle(sku.getSkuTitle());
            model.setSkuPrice(sku.getPrice());
            model.setSkuImage(sku.getSkuDefaultImg());
            model.setSaleCount(sku.getSaleCount() != null ? sku.getSaleCount() : 0L);
            model.setHotScore(0L);
            model.setBrandId(sku.getBrandId());
            model.setCatelogId(sku.getCatalogId());
            model.setBrandName(brandName);
            model.setBrandImage(brandImage);
            model.setCatalogName(catalogName);
            model.setAttrs(searchAttrs);
            model.setHasStock(stockMap.getOrDefault(sku.getSkuId(), true));
            return model;
        }).collect(Collectors.toList());

        // 6. 调用 search 服务保存到 ES
        R r = searchFeignService.saveProduct(esModels);
        if ((int) r.get("code") != 0) {
            log.error("远程调用search保存商品失败, spuId={}", spuId);
            throw new RuntimeException("上架失败");
        }

        // 7. 更新 SPU 发布状态
        spu.setPublishStatus(ProductConstant.StatusEnum.SPU_UP.getCode());
        spu.setUpdateTime(new Date());
        this.updateById(spu);
    }

}
