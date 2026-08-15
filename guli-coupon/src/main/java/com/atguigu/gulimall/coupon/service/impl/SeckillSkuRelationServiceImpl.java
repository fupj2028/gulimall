package com.atguigu.gulimall.coupon.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.alibaba.cloud.commons.lang.StringUtils;
import com.atguigu.gulimall.common.utils.PageUtils;
import com.atguigu.gulimall.common.utils.Query;

import com.atguigu.gulimall.coupon.dao.SeckillSkuRelationDao;
import com.atguigu.gulimall.coupon.entity.SeckillSkuRelationEntity;
import com.atguigu.gulimall.coupon.service.SeckillSkuRelationService;


@Service("seckillSkuRelationService")
public class SeckillSkuRelationServiceImpl extends ServiceImpl<SeckillSkuRelationDao, SeckillSkuRelationEntity> implements SeckillSkuRelationService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<SeckillSkuRelationEntity> queryWrapper = new QueryWrapper<>();
        String promotionId = (String)params.get("promotionSessionId");
        if(StringUtils.isNotEmpty(promotionId)){
            queryWrapper.eq("promotion_session_id", promotionId);
        }
        IPage<SeckillSkuRelationEntity> page = this.page(
                new Query<SeckillSkuRelationEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    @Override
    public boolean existsDuplicated(SeckillSkuRelationEntity relation) {
        QueryWrapper<SeckillSkuRelationEntity> wrapper = new QueryWrapper<SeckillSkuRelationEntity>()
                .eq("promotion_session_id", relation.getPromotionSessionId())
                .eq("sku_id", relation.getSkuId());
        if (relation.getId() != null) {
            wrapper.ne("id", relation.getId());
        }
        return this.count(wrapper) > 0;
    }

}