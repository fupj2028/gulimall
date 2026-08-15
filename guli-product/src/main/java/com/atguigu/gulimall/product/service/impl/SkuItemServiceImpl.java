package com.atguigu.gulimall.product.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.atguigu.gulimall.common.utils.R;
import com.atguigu.gulimall.product.dao.SkuSaleAttrValueDao;
import com.atguigu.gulimall.product.entity.AttrEntity;
import com.atguigu.gulimall.product.entity.BrandEntity;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.entity.ProductAttrValueEntity;
import com.atguigu.gulimall.product.entity.SkuImagesEntity;
import com.atguigu.gulimall.product.entity.SkuInfoEntity;
import com.atguigu.gulimall.product.entity.SkuSaleAttrValueEntity;
import com.atguigu.gulimall.product.entity.SpuInfoDescEntity;
import com.atguigu.gulimall.product.entity.SpuInfoEntity;
import com.atguigu.gulimall.product.feign.ThirdPartyFeignService;
import com.atguigu.gulimall.product.service.AttrGroupService;
import com.atguigu.gulimall.product.service.BrandService;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.service.ProductAttrValueService;
import com.atguigu.gulimall.product.service.SkuImagesService;
import com.atguigu.gulimall.product.service.SkuInfoService;
import com.atguigu.gulimall.product.service.SkuItemService;
import com.atguigu.gulimall.product.service.SpuInfoDescService;
import com.atguigu.gulimall.product.service.SpuInfoService;
import com.atguigu.gulimall.product.vo.AttrGroupWithAttrsVo;
import com.atguigu.gulimall.product.vo.SkuItemVo;
import com.atguigu.gulimall.product.vo.SpuItemAttrGroupVo;
import com.atguigu.gulimall.product.vo.SpuItemAttrValueVo;
import com.atguigu.gulimall.product.vo.SpuSaleAttrVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class SkuItemServiceImpl implements SkuItemService {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(SkuItemServiceImpl.class);

    @Autowired
    private ThirdPartyFeignService thirdPartyFeignService;
    @Autowired
    private SkuSaleAttrValueDao skuSaleAttrValueDao;
    @Autowired
    @Qualifier("skuItemExecutor")
    private ThreadPoolTaskExecutor executor;
    @Autowired
    private SkuInfoService skuInfoService;
    @Autowired
    private SkuImagesService skuImagesService;
    @Autowired
    private SpuInfoService spuInfoService;
    @Autowired
    private SpuInfoDescService spuInfoDescService;
    @Autowired
    private BrandService brandService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private ProductAttrValueService productAttrValueService;
    @Autowired
    private AttrGroupService attrGroupService;

    @Override
    public SkuItemVo buildSkuItem(Long skuId) {
        SkuItemVo item = new SkuItemVo();

        CompletableFuture<SkuInfoEntity> skuFuture = CompletableFuture.supplyAsync(
                () -> skuInfoService.getById(skuId), executor);

        CompletableFuture<Void> basicFuture = skuFuture.thenAcceptAsync(skuInfo -> {
            BeanUtils.copyProperties(skuInfo, item);
            item.setSkuDefaultImg(resolveImageUrl(item.getSkuDefaultImg()));

            List<SkuImagesEntity> images = skuImagesService.list(
                    new QueryWrapper<SkuImagesEntity>().eq("sku_id", skuId));
            images.forEach(img -> img.setImgUrl(resolveImageUrl(img.getImgUrl())));
            item.setSkuImageList(images);

            SpuInfoEntity spuInfo = spuInfoService.getById(skuInfo.getSpuId());
            if (spuInfo != null) {
                item.setWeight(spuInfo.getWeight());
                item.setSpuName(spuInfo.getSpuName());
            }

            BrandEntity brand = brandService.getById(skuInfo.getBrandId());
            if (brand != null) {
                item.setBrandName(brand.getName());
            }

            Long spuId = skuInfo.getSpuId();

            SpuInfoDescEntity spuDesc = spuInfoDescService.getById(spuId);
            if (spuDesc != null && StringUtils.hasText(spuDesc.getDecript())) {
                List<String> urls = Arrays.stream(spuDesc.getDecript().split(","))
                        .map(this::resolveImageUrl)
                        .collect(Collectors.toList());
                item.setDecriptImages(urls);
            }
            item.setSpuDesc(spuDesc);
        }, executor);

        CompletableFuture<Void> catelogFuture = skuFuture.thenAcceptAsync(skuInfo -> {
            Long[] catelogPathIds = categoryService.findCatelogPath(skuInfo.getCatalogId());
            if (catelogPathIds != null) {
                List<CategoryEntity> catelogPath = Arrays.stream(catelogPathIds)
                        .map(categoryService::getById).collect(Collectors.toList());
                item.setCatelogPath(catelogPath);
            }
        }, executor);

        CompletableFuture<Void> saleFuture = skuFuture.thenAcceptAsync(skuInfo -> {
            Long spuId = skuInfo.getSpuId();
            List<SkuSaleAttrValueEntity> allEntities = skuSaleAttrValueDao.selectSaleAttrValuesBySpuId(spuId);

            Map<Long, List<SkuSaleAttrValueEntity>> groupMap = allEntities.stream()
                    .collect(Collectors.groupingBy(
                            SkuSaleAttrValueEntity::getAttrId,
                            LinkedHashMap::new,
                            Collectors.toList()));

            List<SpuSaleAttrVo> saleAttrs = new ArrayList<>();
            groupMap.forEach((attrId, list) -> {
                SpuSaleAttrVo vo = new SpuSaleAttrVo();
                vo.setAttrId(attrId);
                vo.setSaleAttrName(list.get(0).getAttrName());
                vo.setSpuSaleAttrValueList(list.stream()
                        .map(SkuSaleAttrValueEntity::getAttrValue)
                        .distinct()
                        .collect(Collectors.toList()));
                saleAttrs.add(vo);
            });
            item.setSaleAttrs(saleAttrs);

            Map<Long, String> currentSkuSaleAttrMap = allEntities.stream()
                    .filter(e -> e.getSkuId().equals(skuId))
                    .collect(Collectors.toMap(
                            SkuSaleAttrValueEntity::getAttrId,
                            SkuSaleAttrValueEntity::getAttrValue,
                            (a, b) -> a));
            item.setCurrentSkuSaleAttrValues(currentSkuSaleAttrMap);

            Map<Long, Map<String, List<Long>>> skuMap = new LinkedHashMap<>();
            for (SkuSaleAttrValueEntity e : allEntities) {
                if (!skuMap.containsKey(e.getAttrId())) {
                    skuMap.put(e.getAttrId(), new LinkedHashMap<>());
                }
                Map<String, List<Long>> valueMap = skuMap.get(e.getAttrId());
                if (!valueMap.containsKey(e.getAttrValue())) {
                    valueMap.put(e.getAttrValue(), new ArrayList<>());
                }
                valueMap.get(e.getAttrValue()).add(e.getSkuId());
            }
            try {
                item.setValuesSku(objectMapper.writeValueAsString(skuMap));
            } catch (Exception e) {
                item.setValuesSku("{}");
            }
        }, executor);

        CompletableFuture<List<ProductAttrValueEntity>> baseFuture = skuFuture.thenApplyAsync(skuInfo -> {
            return productAttrValueService.list(
                    new QueryWrapper<ProductAttrValueEntity>().eq("spu_id", skuInfo.getSpuId()));
        }, executor);

        CompletableFuture<List<AttrGroupWithAttrsVo>> groupFuture = skuFuture.thenApplyAsync(skuInfo -> {
            return attrGroupService.getAttrGroupWithAttrsByCatelogId(skuInfo.getCatalogId());
        }, executor);

        CompletableFuture<Void> specFuture = baseFuture.thenAcceptBothAsync(groupFuture, (baseAttrs, groups) -> {
            Map<Long, String> attrValueMap = baseAttrs.stream()
                    .collect(Collectors.toMap(
                            ProductAttrValueEntity::getAttrId,
                            ProductAttrValueEntity::getAttrValue));

            List<SpuItemAttrGroupVo> spuAttrGroups = new ArrayList<>();
            for (AttrGroupWithAttrsVo group : groups) {
                List<SpuItemAttrValueVo> attrVals = new ArrayList<>();
                for (AttrEntity attr : group.getAttrs()) {
                    String value = attrValueMap.get(attr.getAttrId());
                    if (value == null) continue;
                    SpuItemAttrValueVo attrVal = new SpuItemAttrValueVo();
                    attrVal.setAttrName(attr.getAttrName());
                    attrVal.setAttrValue(value);
                    attrVals.add(attrVal);
                }
                if (attrVals.isEmpty()) continue;
                SpuItemAttrGroupVo itemGroup = new SpuItemAttrGroupVo();
                itemGroup.setGroupName(group.getAttrGroupName());
                itemGroup.setAttrs(attrVals);
                spuAttrGroups.add(itemGroup);
            }
            item.setAttrGroups(spuAttrGroups);
        }, executor);

        CompletableFuture.allOf(basicFuture, catelogFuture, saleFuture, specFuture).join();

        return item;
    }

    private String resolveImageUrl(String key) {
        if (!StringUtils.hasText(key)) return "";
        try {
            R r = thirdPartyFeignService.access(key);
            Object code = r.get("code");
            if (code instanceof Integer && (Integer) code == 0) {
                Object data = r.get("data");
                if (data instanceof String) return (String) data;
            }
        } catch (Exception e) {
            log.warn("图片URL解析失败: key={}", key, e);
        }
        return key;
    }

}
