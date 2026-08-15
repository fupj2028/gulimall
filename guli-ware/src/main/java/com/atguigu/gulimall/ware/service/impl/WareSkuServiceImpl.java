package com.atguigu.gulimall.ware.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.atguigu.gulimall.common.Exception.NoStockException;
import com.atguigu.gulimall.common.constant.OrderConstant;
import com.atguigu.gulimall.common.to.SkuHasStockVo;
import com.atguigu.gulimall.common.to.StockLockedDetailTo;
import com.atguigu.gulimall.common.to.StockLockedTo;
import com.atguigu.gulimall.common.to.WareSkuLockItem;
import com.atguigu.gulimall.common.to.WareSkuLockVo;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gulimall.common.utils.PageUtils;
import com.atguigu.gulimall.common.utils.Query;
import com.atguigu.gulimall.common.utils.R;

import com.atguigu.gulimall.ware.dao.WareSkuDao;
import com.atguigu.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.atguigu.gulimall.ware.entity.WareOrderTaskEntity;
import com.atguigu.gulimall.ware.entity.WareSkuEntity;
import com.atguigu.gulimall.ware.feign.OrderFeignService;
import com.atguigu.gulimall.ware.feign.ProductFeignService;
import com.atguigu.gulimall.ware.service.WareOrderTaskDetailService;
import com.atguigu.gulimall.ware.service.WareOrderTaskService;
import com.atguigu.gulimall.ware.service.WareSkuService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private OrderFeignService orderFeignService;

    @Autowired
    private WareOrderTaskService wareOrderTaskService;

    @Autowired
    private WareOrderTaskDetailService wareOrderTaskDetailService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> queryWrapper = new QueryWrapper<>();

        String skuId = (String) params.get("skuId");
        if (skuId != null && !skuId.isEmpty()) {
            queryWrapper.eq("sku_id", skuId);
        }

        String wareId = (String) params.get("wareId");
        if (wareId != null && !wareId.isEmpty()) {
            queryWrapper.eq("ware_id", wareId);
        }

        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    @Override
    @Transactional
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        WareSkuEntity wareSku = this.getOne(
                new QueryWrapper<WareSkuEntity>()
                        .eq("sku_id", skuId)
                        .eq("ware_id", wareId));
        if (wareSku == null) {
            wareSku = new WareSkuEntity();
            wareSku.setSkuId(skuId);
            wareSku.setWareId(wareId);
            wareSku.setStock(skuNum);
            wareSku.setStockLocked(0);
        } else {
            wareSku.setStock(wareSku.getStock() + skuNum);
        }

        // 远程获取 skuName
        if (wareSku.getSkuName() == null) {
            try {
                R r = productFeignService.info(skuId);
                if ((int) r.get("code") == 0) {
                    Map<String, Object> skuInfo = (Map<String, Object>) r.get("skuInfo");
                    wareSku.setSkuName((String) skuInfo.get("skuName"));
                }
            } catch (Exception e) {
                log.error("远程调用product获取skuName失败, skuId={}", skuId, e);
            }
        }

        if (wareSku.getId() == null) {
            this.save(wareSku);
        } else {
            this.updateById(wareSku);
        }
    }

    @Override
    public List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return List.of();
        }

        List<WareSkuEntity> records = this.list(
                new QueryWrapper<WareSkuEntity>()
                        .select("sku_id, SUM(stock) as stock")
                        .in("sku_id", skuIds)
                        .groupBy("sku_id")
        );

        return records.stream().map(r -> {
            SkuHasStockVo vo = new SkuHasStockVo();
            vo.setSkuId(r.getSkuId());
            vo.setHasStock(r.getStock() != null && r.getStock() > 0);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StockLockedTo lockStock(WareSkuLockVo wareSkuLockVo) {
        //创建工作单
        WareOrderTaskEntity task = new WareOrderTaskEntity();
        task.setOrderSn(wareSkuLockVo.getOrderSn());
        task.setTaskStatus(1);
        task.setCreateTime(new Date());
        wareOrderTaskService.save(task);

        StockLockedTo to = new StockLockedTo();
        to.setOrderSn(wareSkuLockVo.getOrderSn());
        to.setTaskId(task.getId());
        List<StockLockedDetailTo> details = new ArrayList<>();

        for (WareSkuLockItem item : wareSkuLockVo.getLocks()) {
            //记录锁定明细
            WareOrderTaskDetailEntity detail = new WareOrderTaskDetailEntity();
            detail.setTaskId(task.getId());
            detail.setSkuId(item.getSkuId());
            detail.setSkuNum(item.getNum());
            detail.setLockStatus(1);

            //单仓锁定：找一家库存足够的仓库一次锁满，不跨仓拆分
            List<WareSkuEntity> wares = this.list(new QueryWrapper<WareSkuEntity>()
                    .eq("sku_id", item.getSkuId())
                    .orderByDesc("stock"));
            boolean locked = false;
            for (WareSkuEntity ware : wares) {
                //条件更新保证并发下不超锁；只有该仓可用库存足够才生效
                boolean ok = this.update(new LambdaUpdateWrapper<WareSkuEntity>()
                        .eq(WareSkuEntity::getId, ware.getId())
                        .setSql("stock_locked = stock_locked + " + item.getNum())
                        .apply("stock - stock_locked >= {0}", item.getNum()));
                if (ok) {
                    //记录锁定仓库，解锁时据此释放
                    detail.setWareId(ware.getId());
                    locked = true;
                    break;
                }
            }

            //整体原子：任一SKU无法锁满，抛异常回滚整个事务，整单失败
            if (!locked) {
                throw new NoStockException(item.getSkuId());
            }

            wareOrderTaskDetailService.save(detail);

            StockLockedDetailTo d = new StockLockedDetailTo();
            d.setDetailId(detail.getId());
            d.setSkuId(item.getSkuId());
            d.setSkuNum(item.getNum());
            details.add(d);
        }

        to.setDetails(details);
        return to;
    }

    @Override
    @Transactional
    public void unlockStock(StockLockedTo to) {
        //订单状态：已支付则不解锁；订单不存在或已关闭则解锁
        Integer orderStatus = null;
        try {
            R r = orderFeignService.getOrderStatus(to.getOrderSn());
            orderStatus = r.get("status") == null ? null : Integer.parseInt(r.get("status").toString());
        } catch (Exception e) {
            //查询订单状态失败：抛出异常让消息重新入队重试，避免消息被误确认后丢失
            log.error("查询订单状态失败，orderSn={}", to.getOrderSn(), e);
            throw new RuntimeException("查询订单状态失败", e);
        }
        if (orderStatus != null && !orderStatus.equals(OrderConstant.ORDER_STATUS_CLOSED)) {
            return;
        }

        if (to.getDetails() == null) {
            return;
        }
        for (StockLockedDetailTo d : to.getDetails()) {
            WareOrderTaskDetailEntity detail = wareOrderTaskDetailService.getById(d.getDetailId());
            //幂等：已解锁的明细跳过
            if (detail == null
                    || (detail.getLockStatus() != null && detail.getLockStatus() == 2)
                    || detail.getWareId() == null) {
                continue;
            }
            boolean ok = this.update(new LambdaUpdateWrapper<WareSkuEntity>()
                    .eq(WareSkuEntity::getId, detail.getWareId())
                    .eq(WareSkuEntity::getSkuId, detail.getSkuId())
                    .setSql("stock_locked = stock_locked - " + detail.getSkuNum()));
            if (ok) {
                detail.setLockStatus(2);
                wareOrderTaskDetailService.updateById(detail);
                log.info("库存解锁成功，orderSn={}, skuId={}, num={}",
                        to.getOrderSn(), detail.getSkuId(), detail.getSkuNum());
            }
        }
    }

    @Override
    @Transactional
    public void deductStock(StockLockedTo to) {
        //订单已支付（待发货）才允许扣减；未支付/已关闭不扣
        Integer orderStatus = null;
        try {
            R r = orderFeignService.getOrderStatus(to.getOrderSn());
            orderStatus = r.get("status") == null ? null : Integer.parseInt(r.get("status").toString());
        } catch (Exception e) {
            //查询订单状态失败：抛出异常让消息重新入队重试，避免消息被误确认后丢失
            log.error("查询订单状态失败，orderSn={}", to.getOrderSn(), e);
            throw new RuntimeException("查询订单状态失败", e);
        }
        if (orderStatus == null || !orderStatus.equals(OrderConstant.ORDER_STATUS_TO_DELIVER)) {
            log.info("订单非待发货状态，跳过库存扣减，orderSn={}, status={}", to.getOrderSn(), orderStatus);
            return;
        }

        if (to.getDetails() == null) {
            return;
        }
        for (StockLockedDetailTo d : to.getDetails()) {
            WareOrderTaskDetailEntity detail = wareOrderTaskDetailService.getById(d.getDetailId());
            //幂等：非锁定(1)状态的明细跳过（null/2已释放/3已扣减）
            if (detail == null
                    || (detail.getLockStatus() != null && detail.getLockStatus() != 1)
                    || detail.getWareId() == null) {
                continue;
            }
            boolean ok = this.update(new LambdaUpdateWrapper<WareSkuEntity>()
                    .eq(WareSkuEntity::getId, detail.getWareId())
                    .eq(WareSkuEntity::getSkuId, detail.getSkuId())
                    .setSql("stock = stock - " + detail.getSkuNum())
                    .setSql("stock_locked = stock_locked - " + detail.getSkuNum())
                    .apply("stock_locked >= {0}", detail.getSkuNum()));
            if (ok) {
                detail.setLockStatus(3);
                wareOrderTaskDetailService.updateById(detail);
                log.info("库存扣减成功，orderSn={}, skuId={}, num={}",
                        to.getOrderSn(), detail.getSkuId(), detail.getSkuNum());
            } else {
                //条件更新失败（锁定库存不足等数据异常），抛出异常让消息重试
                throw new RuntimeException("库存扣减失败，锁定库存不足，orderSn=" + to.getOrderSn()
                        + ", skuId=" + detail.getSkuId());
            }
        }
    }

}