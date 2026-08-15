package com.atguigu.gulimall.coupon.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gulimall.common.utils.PageUtils;
import com.atguigu.gulimall.common.utils.Query;
import com.atguigu.gulimall.common.to.SeckillSessionWithSkusTo;
import com.atguigu.gulimall.common.to.SeckillSkuRelationTo;

import com.atguigu.gulimall.coupon.dao.SeckillSessionDao;
import com.atguigu.gulimall.coupon.entity.SeckillSessionEntity;
import com.atguigu.gulimall.coupon.entity.SeckillSkuRelationEntity;
import com.atguigu.gulimall.coupon.service.SeckillSessionService;
import com.atguigu.gulimall.coupon.service.SeckillSkuRelationService;


@Service("seckillSessionService")
public class SeckillSessionServiceImpl extends ServiceImpl<SeckillSessionDao, SeckillSessionEntity> implements SeckillSessionService {

    @Autowired
    private SeckillSkuRelationService seckillSkuRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SeckillSessionEntity> page = this.page(
                new Query<SeckillSessionEntity>().getPage(params),
                new QueryWrapper<SeckillSessionEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<SeckillSessionWithSkusTo> listSessionsWithSkusIn3Days() {
        Date now = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        calendar.add(Calendar.DAY_OF_MONTH, 3);
        Date threeDaysLater = calendar.getTime();

        List<SeckillSessionEntity> sessions = this.list(
                new QueryWrapper<SeckillSessionEntity>()
                        .ge("start_time", now)
                        .le("start_time", threeDaysLater)
                        .eq("status", 1));

        if (sessions == null || sessions.isEmpty()) {
            return Collections.emptyList();
        }

        List<SeckillSessionWithSkusTo> result = new ArrayList<>();
        for (SeckillSessionEntity session : sessions) {
            List<SeckillSkuRelationEntity> relations = seckillSkuRelationService.list(
                    new QueryWrapper<SeckillSkuRelationEntity>()
                            .eq("promotion_session_id", session.getId()));

            SeckillSessionWithSkusTo to = new SeckillSessionWithSkusTo();
            BeanUtils.copyProperties(session, to);
            to.setRelations(relations.stream().map(relation -> {
                SeckillSkuRelationTo relationTo = new SeckillSkuRelationTo();
                BeanUtils.copyProperties(relation, relationTo);
                return relationTo;
            }).collect(Collectors.toList()));
            result.add(to);
        }
        return result;
    }

}
