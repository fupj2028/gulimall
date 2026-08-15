package com.atguigu.gulimall.product.service.impl;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gulimall.common.utils.PageUtils;
import com.atguigu.gulimall.common.utils.Query;

import com.atguigu.gulimall.product.dao.SkuSaleAttrValueDao;
import com.atguigu.gulimall.product.entity.SkuSaleAttrValueEntity;
import com.atguigu.gulimall.product.service.SkuSaleAttrValueService;
import com.atguigu.gulimall.product.vo.SpuSaleAttrVo;


@Service("skuSaleAttrValueService")
public class SkuSaleAttrValueServiceImpl extends ServiceImpl<SkuSaleAttrValueDao, SkuSaleAttrValueEntity> implements SkuSaleAttrValueService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuSaleAttrValueEntity> page = this.page(
                new Query<SkuSaleAttrValueEntity>().getPage(params),
                new QueryWrapper<SkuSaleAttrValueEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<SpuSaleAttrVo> getSpuSaleAttrVoListBySpuId(Long spuId) {
        List<SkuSaleAttrValueEntity> entities = this.baseMapper.selectSaleAttrValuesBySpuId(spuId);

        Map<Long, List<SkuSaleAttrValueEntity>> groupMap = entities.stream()
                .collect(Collectors.groupingBy(
                        SkuSaleAttrValueEntity::getAttrId,
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<SpuSaleAttrVo> result = new ArrayList<>();
        groupMap.forEach((attrId, list) -> {
            SpuSaleAttrVo vo = new SpuSaleAttrVo();
            vo.setAttrId(attrId);
            vo.setSaleAttrName(list.get(0).getAttrName());
            vo.setSpuSaleAttrValueList(list.stream()
                    .map(SkuSaleAttrValueEntity::getAttrValue)
                    .distinct()
                    .collect(Collectors.toList()));
            result.add(vo);
        });
        return result;
    }

    @Override
    public List<String> querySaleAttrAsString(Long skuId) {
        List<SkuSaleAttrValueEntity> saleAttrs = this.baseMapper.selectList(new QueryWrapper<SkuSaleAttrValueEntity>().eq("sku_id", skuId));

        return saleAttrs.stream().map((saleAttr)->{
            return saleAttr.getAttrName() + ":" +saleAttr.getAttrValue();
        }).collect(Collectors.toList());
    }

}