package com.atguigu.gulimall.ware.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gulimall.common.constant.WareConstant.PurchaseDetailStatusEnum;
import com.atguigu.gulimall.common.constant.WareConstant.PurchaseStatusEnum;
import com.atguigu.gulimall.common.utils.PageUtils;
import com.atguigu.gulimall.common.utils.Query;

import com.atguigu.gulimall.ware.dao.PurchaseDao;
import com.atguigu.gulimall.ware.entity.PurchaseDetailEntity;
import com.atguigu.gulimall.ware.entity.PurchaseEntity;
import com.atguigu.gulimall.ware.service.PurchaseDetailService;
import com.atguigu.gulimall.ware.service.PurchaseService;
import com.atguigu.gulimall.ware.service.WareSkuService;
import com.atguigu.gulimall.ware.vo.MergeVo;
import com.atguigu.gulimall.ware.vo.PurchaseDetailDoneVo;
import com.atguigu.gulimall.ware.vo.PurchaseDoneVo;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {


    @Autowired
    private PurchaseDetailService purchaseDetailService;

    @Autowired
    private WareSkuService wareSkuService;
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryUnreceiveList(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>().eq("status", 0).or().eq("status", 1)
        );

        return new PageUtils(page);
    }

    @Override
    @Transactional
    public void received(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("采购单ID列表不能为空");
        }

        List<PurchaseEntity> purchases = this.listByIds(ids);
        if (purchases.isEmpty()) {
            throw new IllegalArgumentException("采购单不存在");
        }
        if (purchases.size() != ids.size()) {
            throw new IllegalArgumentException("部分采购单不存在");
        }
        for (PurchaseEntity purchase : purchases) {
            int status = purchase.getStatus();
            if (status != PurchaseStatusEnum.CREATED.getCode()
                    && status != PurchaseStatusEnum.ASSIGNED.getCode()) {
                throw new IllegalArgumentException("采购单 " + purchase.getId()
                        + " 状态不是新建或已分配，无法领取");
            }
        }

        List<PurchaseDetailEntity> details = purchaseDetailService.list(
                new QueryWrapper<PurchaseDetailEntity>().in("purchase_id", ids));
        List<PurchaseDetailEntity> updatedDetails = details.stream().map(d -> {
            d.setStatus(PurchaseDetailStatusEnum.BUYING.getCode());
            return d;
        }).collect(Collectors.toList());
        purchaseDetailService.updateBatchById(updatedDetails);

        List<PurchaseEntity> updatedPurchases = purchases.stream().map(p -> {
            p.setStatus(PurchaseStatusEnum.RECEIVED.getCode());
            p.setUpdateTime(new java.util.Date());
            return p;
        }).collect(Collectors.toList());
        this.updateBatchById(updatedPurchases);
    }

    @Override
    public void merge(MergeVo mergeVo) {
        Long purchaseId = mergeVo.getPurchaseId();
        if (purchaseId == null) {
            // 新建采购单
            PurchaseEntity purchase = new PurchaseEntity();
            purchase.setStatus(PurchaseStatusEnum.CREATED.getCode()); // 新建状态
            purchase.setCreateTime(new java.util.Date());
            purchase.setUpdateTime(new java.util.Date());
            this.save(purchase);
            purchaseId = purchase.getId();
        }
        Long finalPurchaseId = purchaseId;
        
        List<Long> items = mergeVo.getItems();
        List<PurchaseDetailEntity> purchaseDetails = items.stream().map(i->{
            PurchaseDetailEntity purchase = new PurchaseDetailEntity();
            purchase.setId(i);
            purchase.setPurchaseId(finalPurchaseId);
            purchase.setStatus(PurchaseDetailStatusEnum.ASSIGNED.getCode()); // 已分配状态
            return purchase;
        }).collect(Collectors.toList());
        purchaseDetailService.updateBatchById(purchaseDetails);

    }

    @Override
    @Transactional
    public void done(PurchaseDoneVo purchaseDoneVo) {
        Long purchaseId = purchaseDoneVo.getId();
        List<PurchaseDetailDoneVo> items = purchaseDoneVo.getItems();
        if (purchaseId == null) {
            throw new IllegalArgumentException("采购单ID不能为空");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("采购需求列表不能为空");
        }

        // 1. 校验采购单存在且状态为已领取
        PurchaseEntity purchase = this.getById(purchaseId);
        if (purchase == null) {
            throw new IllegalArgumentException("采购单不存在");
        }
        if (purchase.getStatus() != PurchaseStatusEnum.RECEIVED.getCode()) {
            throw new IllegalArgumentException("采购单状态不是已领取，无法完成");
        }

        // 2. 查询该采购单所有原始 detail，建立 id→entity 映射
        List<PurchaseDetailEntity> dbDetails = purchaseDetailService.list(
                new QueryWrapper<PurchaseDetailEntity>().eq("purchase_id", purchaseId));
        Map<Long, PurchaseDetailEntity> dbDetailMap = dbDetails.stream()
                .collect(Collectors.toMap(PurchaseDetailEntity::getId, d -> d));

        // 3. 校验每项 detail 的归属关系和当前状态
        boolean hasError = false;
        List<PurchaseDetailEntity> updatedDetails = new ArrayList<>();
        for (PurchaseDetailDoneVo item : items) {
            if (item.getId() == null) {
                throw new IllegalArgumentException("采购需求ID不能为空");
            }
            if (item.getStatus() == null) {
                throw new IllegalArgumentException("采购需求状态不能为空");
            }
            PurchaseDetailEntity detail = dbDetailMap.get(item.getId());
            if (detail == null) {
                throw new IllegalArgumentException("采购需求 " + item.getId() + " 不属于该采购单");
            }
            if (detail.getStatus() != PurchaseDetailStatusEnum.BUYING.getCode()) {
                throw new IllegalArgumentException("采购需求 " + item.getId() + " 状态不是正在采购");
            }

            detail.setStatus(item.getStatus());
            detail.setSkuNum(item.getTotal());
            detail.setSkuPrice(item.getPrice());
            updatedDetails.add(detail);

            if (item.getStatus() == PurchaseDetailStatusEnum.ERROR.getCode()) {
                hasError = true;
            } else {
                wareSkuService.addStock(detail.getSkuId(), detail.getWareId(), detail.getSkuNum());
            }
        }

        // 4. 批量更新 detail
        purchaseDetailService.updateBatchById(updatedDetails);

        // 5. 更新采购单
        purchase.setStatus(hasError ? PurchaseStatusEnum.ERROR.getCode()
                                    : PurchaseStatusEnum.FINISHED.getCode());
        purchase.setUpdateTime(new java.util.Date());
        this.updateById(purchase);
    }
       

}