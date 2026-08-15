package com.atguigu.gulimall.seckill.scheduled;

import com.atguigu.gulimall.common.to.SeckillSessionWithSkusTo;
import com.atguigu.gulimall.common.to.SeckillSkuRelationTo;
import com.atguigu.gulimall.seckill.config.SeckillConstant;
import com.atguigu.gulimall.seckill.feign.CouponFeignService;
import com.atguigu.gulimall.seckill.to.SeckillSkuRedisTo;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class SeckillSkuScheduled {

    private static final String UNLOCK_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    @Autowired
    private CouponFeignService couponFeignService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Scheduled(initialDelay = 10000, fixedRate = 5 * 60 * 1000L)
    public void uploadSeckillSkuLatest3Days() {
        String token = UUID.randomUUID().toString();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(
                SeckillConstant.SECKILL_UPLOAD_LOCK, token, 5, TimeUnit.MINUTES);
        if (locked == null || !locked) {
            log.info("秒杀上架任务已在其他实例执行，本次跳过");
            return;
        }
        try {
            doUpload();
        } finally {
            redisTemplate.execute(new DefaultRedisScript<>(UNLOCK_LUA, Long.class),
                    List.of(SeckillConstant.SECKILL_UPLOAD_LOCK), token);
        }
    }

    private void doUpload() {
        log.info("秒杀商品上架任务开始");
        List<SeckillSessionWithSkusTo> sessions;
        try {
            sessions = couponFeignService.getSeckillSessionsIn3Days();
        } catch (Exception e) {
            log.error("获取最近3天秒杀场次失败", e);
            return;
        }
        if (sessions == null || sessions.isEmpty()) {
            log.info("最近3天没有待上架的秒杀场次");
            return;
        }

        int uploaded = 0;
        for (SeckillSessionWithSkusTo session : sessions) {
            long end = session.getEndTime().getTime();
            if (end <= System.currentTimeMillis()) {
                continue;
            }
            List<SeckillSkuRelationTo> relations = session.getRelations();
            if (relations == null || relations.isEmpty()) {
                continue;
            }
            long ttl = end - System.currentTimeMillis();

            String sessionKey = SeckillConstant.SECKILL_SESSION_PREFIX
                    + session.getStartTime().getTime() + "_" + end;
            List<String> skuKeys = new ArrayList<>();
            for (SeckillSkuRelationTo relation : relations) {
                try {
                    SeckillSkuRedisTo redisTo = new SeckillSkuRedisTo();
                    redisTo.setId(relation.getId());
                    redisTo.setSessionId(session.getId());
                    redisTo.setSkuId(relation.getSkuId());
                    redisTo.setSeckillPrice(relation.getSeckillPrice());
                    redisTo.setSeckillCount(relation.getSeckillCount());
                    redisTo.setSeckillLimit(relation.getSeckillLimit());
                    redisTo.setSeckillSort(relation.getSeckillSort());
                    redisTo.setRandomCode(UUID.randomUUID().toString().replace("-", ""));
                    redisTo.setStartTime(session.getStartTime());
                    redisTo.setEndTime(session.getEndTime());

                    String skuKey = SeckillConstant.SECKILL_SKU_PREFIX
                            + session.getId() + "_" + relation.getSkuId();
                    redisTemplate.opsForValue().setIfAbsent(skuKey,
                            objectMapper.writeValueAsString(redisTo), ttl, TimeUnit.MILLISECONDS);

                    String stockKey = SeckillConstant.SECKILL_STOCK_PREFIX
                            + session.getId() + "_" + relation.getSkuId();
                    redisTemplate.opsForValue().setIfAbsent(stockKey,
                            String.valueOf(relation.getSeckillCount().intValue()),
                            ttl, TimeUnit.MILLISECONDS);

                    skuKeys.add(skuKey);
                } catch (Exception e) {
                    log.error("上架商品失败, skuId={}", relation.getSkuId(), e);
                }
            }
            Boolean sessionSet = redisTemplate.opsForValue().setIfAbsent(sessionKey,
                    String.join(",", skuKeys), ttl, TimeUnit.MILLISECONDS);
            if (sessionSet != null && sessionSet) {
                uploaded++;
            }
        }
        log.info("秒杀商品上架完成，本次新上架{}个场次", uploaded);
    }
}
