package com.atguigu.gulimall.product.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.beans.BeanUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gulimall.product.vo.AttrGroupWithAttrsVo;
import com.atguigu.gulimall.common.constant.ProductConstant;
import com.atguigu.gulimall.common.utils.PageUtils;
import com.atguigu.gulimall.common.utils.Query;
import com.atguigu.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.atguigu.gulimall.product.dao.AttrDao;
import com.atguigu.gulimall.product.dao.AttrGroupDao;
import com.atguigu.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.atguigu.gulimall.product.entity.AttrEntity;
import com.atguigu.gulimall.product.entity.AttrGroupEntity;
import com.atguigu.gulimall.product.service.AttrGroupService;

import lombok.RequiredArgsConstructor;

@Service("attrGroupService")
@RequiredArgsConstructor
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {

    private final AttrAttrgroupRelationDao relationDao;
    private final AttrDao attrDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<AttrGroupEntity>());

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params, Long catelogId) {
        QueryWrapper<AttrGroupEntity> wrapper = new QueryWrapper<AttrGroupEntity>();
        String key = (String) params.get("key");
        if (key != null && !key.isEmpty()) {
            wrapper.and((w) -> w.eq("attr_group_id", key).or().like("attr_group_name", key));
        }
        if (catelogId == 0) {
            IPage<AttrGroupEntity> page = this.page(
                    new Query<AttrGroupEntity>().getPage(params),
                    wrapper);
            return new PageUtils(page);
        } else {
            wrapper.eq("catelog_id", catelogId);
            IPage<AttrGroupEntity> page = this.page(
                    new Query<AttrGroupEntity>().getPage(params),
                    wrapper);
            return new PageUtils(page);
        }
    }

    @Override
    @Transactional
    public boolean updateById(AttrGroupEntity entity) {
        AttrGroupEntity old = this.getById(entity.getAttrGroupId());
        boolean updated = super.updateById(entity);
        if (updated && old != null && !Objects.equals(old.getCatelogId(), entity.getCatelogId())) {
            List<AttrAttrgroupRelationEntity> relations = relationDao.selectList(
                    new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", entity.getAttrGroupId()));
            List<Long> attrIds = relations.stream()
                    .map(AttrAttrgroupRelationEntity::getAttrId)
                    .collect(Collectors.toList());
            if (!attrIds.isEmpty()) {
                AttrEntity attrUpdate = new AttrEntity();
                attrUpdate.setCatelogId(entity.getCatelogId());
                attrDao.update(attrUpdate, new QueryWrapper<AttrEntity>().in("attr_id", attrIds));
            }
        }
        return updated;
    }

    @Override
    public List<AttrEntity> getRelationAttrs(Long attrGroupId) {
        List<AttrAttrgroupRelationEntity> relations = relationDao.selectList(
                new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", attrGroupId));
        if (relations.isEmpty()) {
            return List.of();
        }
        List<Long> attrIds = relations.stream()
                .map(AttrAttrgroupRelationEntity::getAttrId)
                .collect(Collectors.toList());
        return attrDao.selectBatchIds(attrIds);
    }

    @Override
    public PageUtils getNoRelationPage(Map<String, Object> params, Long attrGroupId) {
        AttrGroupEntity attrGroup = this.getById(attrGroupId);
        if (attrGroup == null) {
            return new PageUtils(new Page<>(1, 10, 0));
        }
        Long catelogId = attrGroup.getCatelogId();

        List<AttrAttrgroupRelationEntity> relations = relationDao.selectList(
                new QueryWrapper<AttrAttrgroupRelationEntity>()
                        .inSql("attr_group_id",
                                "SELECT attr_group_id FROM pms_attr_group WHERE catelog_id = " + catelogId));
        List<Long> associatedAttrIds = relations.stream()
                .map(AttrAttrgroupRelationEntity::getAttrId)
                .distinct()
                .collect(Collectors.toList());

        QueryWrapper<AttrEntity> wrapper = new QueryWrapper<AttrEntity>()
                .eq("catelog_id", catelogId)
                .eq("attr_type", ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode());
        if (!associatedAttrIds.isEmpty()) {
            wrapper.notIn("attr_id", associatedAttrIds);
        }

        String key = (String) params.get("key");
        if (StringUtils.hasText(key)) {
            wrapper.and(w -> w.eq("attr_id", key).or().like("attr_name", key));
        }

        IPage<AttrEntity> page = attrDao.selectPage(
                new Query<AttrEntity>().getPage(params), wrapper);
        return new PageUtils(page);
    }

    @Override
    @Transactional
    public void saveRelations(Collection<AttrAttrgroupRelationEntity> relations) {
        if (relations.isEmpty()) return;

        Long attrGroupId = relations.iterator().next().getAttrGroupId();
        List<Long> attrIds = relations.stream()
                .map(AttrAttrgroupRelationEntity::getAttrId)
                .collect(Collectors.toList());

        List<Long> existingIds = relationDao.selectList(
                        new QueryWrapper<AttrAttrgroupRelationEntity>()
                                .select("attr_id")
                                .eq("attr_group_id", attrGroupId)
                                .in("attr_id", attrIds))
                .stream()
                .map(AttrAttrgroupRelationEntity::getAttrId)
                .collect(Collectors.toList());

        for (AttrAttrgroupRelationEntity r : relations) {
            if (!existingIds.contains(r.getAttrId())) {
                relationDao.insert(r);
            }
        }
    }

    @Override
    @Transactional
    public void removeRelations(Collection<AttrAttrgroupRelationEntity> relations) {
        if (relations.isEmpty()) return;

        Long attrGroupId = relations.iterator().next().getAttrGroupId();
        List<Long> attrIds = relations.stream()
                .map(AttrAttrgroupRelationEntity::getAttrId)
                .collect(Collectors.toList());

        relationDao.delete(new QueryWrapper<AttrAttrgroupRelationEntity>()
                .eq("attr_group_id", attrGroupId)
                .in("attr_id", attrIds));
    }

    @Override
    public List<AttrGroupWithAttrsVo> getAttrGroupWithAttrsByCatelogId(Long catelogId) {
        List<AttrGroupEntity> groups = this.list(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId));
        if (groups.isEmpty()) {
            return List.of();
        }

        List<Long> groupIds = groups.stream().map(AttrGroupEntity::getAttrGroupId).collect(Collectors.toList());

        List<AttrAttrgroupRelationEntity> relations = relationDao.selectList(
                new QueryWrapper<AttrAttrgroupRelationEntity>().in("attr_group_id", groupIds));

        Map<Long, List<Long>> groupAttrIdsMap = relations.stream()
                .collect(Collectors.groupingBy(
                        AttrAttrgroupRelationEntity::getAttrGroupId,
                        Collectors.mapping(AttrAttrgroupRelationEntity::getAttrId, Collectors.toList())));

        List<Long> allAttrIds = relations.stream()
                .map(AttrAttrgroupRelationEntity::getAttrId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, AttrEntity> attrMap = allAttrIds.isEmpty() ? Map.of()
                : attrDao.selectBatchIds(allAttrIds).stream()
                        .collect(Collectors.toMap(AttrEntity::getAttrId, a -> a));

        return groups.stream().map(g -> {
            AttrGroupWithAttrsVo vo = new AttrGroupWithAttrsVo();
            BeanUtils.copyProperties(g, vo);
            List<Long> attrIds = groupAttrIdsMap.getOrDefault(g.getAttrGroupId(), List.of());
            vo.setAttrs(attrIds.stream().map(attrMap::get).filter(Objects::nonNull).collect(Collectors.toList()));
            return vo;
        }).collect(Collectors.toList());
    }

}