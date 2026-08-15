package com.atguigu.gulimall.product.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// import static org.junit.jupiter.api.DynamicTest.stream;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gulimall.common.constant.ProductConstant;
import com.atguigu.gulimall.common.utils.PageUtils;
import com.atguigu.gulimall.common.utils.Query;

import com.atguigu.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.atguigu.gulimall.product.dao.AttrDao;
import com.atguigu.gulimall.product.dao.AttrGroupDao;
import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.atguigu.gulimall.product.entity.AttrEntity;
import com.atguigu.gulimall.product.entity.AttrGroupEntity;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.entity.ProductAttrValueEntity;
import com.atguigu.gulimall.product.service.AttrService;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.service.ProductAttrValueService;
import com.atguigu.gulimall.product.vo.AttrRespVo;
import com.atguigu.gulimall.product.vo.AttrVo;

import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;


@Service("attrService")
@RequiredArgsConstructor
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {

    private final AttrAttrgroupRelationDao relationDao;
    private final CategoryService categoryService;
    private final CategoryDao categoryDao;
    private final AttrGroupDao attrGroupDao;
    private final ProductAttrValueService productAttrValueService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<AttrEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    @Transactional
    public void saveAttr(AttrVo vo) {
        AttrEntity entity = new AttrEntity();
        org.springframework.beans.BeanUtils.copyProperties(vo, entity);
        this.save(entity);
        if (vo.getAttrGroupId() != null) {
            AttrAttrgroupRelationEntity relation = new AttrAttrgroupRelationEntity();
            relation.setAttrId(entity.getAttrId());
            relation.setAttrGroupId(vo.getAttrGroupId());
            relationDao.insert(relation);
        }
    }

    @Override
    @Transactional
    public void updateAttr(AttrVo vo) {
        AttrEntity entity = new AttrEntity();
        org.springframework.beans.BeanUtils.copyProperties(vo, entity);
        this.updateById(entity);
        relationDao.delete(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", vo.getAttrId()));
        if (vo.getAttrGroupId() != null) {
            AttrAttrgroupRelationEntity relation = new AttrAttrgroupRelationEntity();
            relation.setAttrId(vo.getAttrId());
            relation.setAttrGroupId(vo.getAttrGroupId());
            relationDao.insert(relation);
        }
    }

    @Override
    public AttrVo getAttrInfo(Long attrId) {
        AttrEntity entity = this.getById(attrId);
        AttrVo vo = new AttrVo();
        org.springframework.beans.BeanUtils.copyProperties(entity, vo);

        AttrAttrgroupRelationEntity relation = relationDao.selectOne(
                new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrId));
        if (relation != null) {
            vo.setAttrGroupId(relation.getAttrGroupId());
        }
        vo.setCatelogPath(categoryService.findCatelogPath(entity.getCatelogId()));
        return vo;
    }

    @Override
    public PageUtils queryPageByType(Map<String, Object> params, Long catelogId, String type) {
        QueryWrapper<AttrEntity> wrapper = new QueryWrapper<>();
        if (catelogId > 0) {
            wrapper.eq("catelog_id", catelogId);
        }
        wrapper.eq("attr_type", "base".equals(type)
                ? ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()
                : ProductConstant.AttrEnum.ATTR_TYPE_SALE.getCode());

        String key = (String) params.get("key");
        if (StringUtils.hasText(key)) {
            wrapper.and(w -> w.eq("attr_id", key).or().like("attr_name", key));
        }

        IPage<AttrEntity> page = this.page(new Query<AttrEntity>().getPage(params), wrapper);
        PageUtils pageUtils = new PageUtils(page);

        List<AttrRespVo> voList = page.getRecords().stream().map(entity -> {
            AttrRespVo vo = new AttrRespVo();
            org.springframework.beans.BeanUtils.copyProperties(entity, vo);
            CategoryEntity category = categoryDao.selectById(entity.getCatelogId());
            if (category != null) vo.setCatelogName(category.getName());
            AttrAttrgroupRelationEntity relation = relationDao.selectOne(
                    new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", entity.getAttrId()));
            if (relation != null) {
                AttrGroupEntity group = attrGroupDao.selectById(relation.getAttrGroupId());
                if (group != null) vo.setGroupName(group.getAttrGroupName());
            }
            return vo;
        }).collect(Collectors.toList());

        pageUtils.setList(voList);
        return pageUtils;
    }

    @Override
    public List<ProductAttrValueEntity> getProductAttrListBySpuId(Long spuId) {

        return productAttrValueService.list(new QueryWrapper<ProductAttrValueEntity>().eq("spu_id", spuId));
    }

    @Override
    @Transactional
    public void updateAttrBySpuId(List<ProductAttrValueEntity> entities, Long spuId) {
        productAttrValueService.remove(new QueryWrapper<ProductAttrValueEntity>().eq("spu_id",spuId));

        List<ProductAttrValueEntity> collect = entities.stream().map(e->{
            e.setSpuId(spuId);
            return e;
        }).collect(Collectors.toList());
        productAttrValueService.saveBatch(collect);
        
    }

    @Override
    public List<Long> getSearchAttrIds(List<Long> attrIds) {
        if (attrIds == null || attrIds.isEmpty()) {
            return Collections.emptyList();
        }
        return this.baseMapper.selectList(
                new QueryWrapper<AttrEntity>()
                        .select("attr_id")
                        .in("attr_id", attrIds)
                        .eq("search_type", 1)
        ).stream().map(AttrEntity::getAttrId).collect(Collectors.toList());
    }

}