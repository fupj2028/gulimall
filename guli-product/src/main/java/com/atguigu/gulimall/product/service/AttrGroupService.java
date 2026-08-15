package com.atguigu.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gulimall.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.atguigu.gulimall.product.entity.AttrEntity;
import com.atguigu.gulimall.product.entity.AttrGroupEntity;
import com.atguigu.gulimall.product.vo.AttrGroupWithAttrsVo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 属性分组
 *
 * @author fupengju
 * @email 3545485659@qq.com
 * @date 2026-07-05 17:59:52
 */
public interface AttrGroupService extends IService<AttrGroupEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryPage(Map<String, Object> params, Long catelogId);

    List<AttrEntity> getRelationAttrs(Long attrGroupId);

    PageUtils getNoRelationPage(Map<String, Object> params, Long attrGroupId);

    void saveRelations(Collection<AttrAttrgroupRelationEntity> relations);

    void removeRelations(Collection<AttrAttrgroupRelationEntity> relations);

    List<AttrGroupWithAttrsVo> getAttrGroupWithAttrsByCatelogId(Long catelogId);
}

