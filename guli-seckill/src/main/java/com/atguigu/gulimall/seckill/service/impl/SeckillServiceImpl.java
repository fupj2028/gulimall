package com.atguigu.gulimall.seckill.service.impl;

import com.atguigu.gulimall.common.to.SkuSeckillInfoVo;
import com.atguigu.gulimall.common.vo.MemberLoginVo;
import com.atguigu.gulimall.seckill.config.SeckillConstant;
import com.atguigu.gulimall.seckill.exception.SeckillException;
import com.atguigu.gulimall.seckill.feign.ProductFeignService;
import com.atguigu.gulimall.seckill.intercepter.SeckillLoginInterceptor;
import com.atguigu.gulimall.seckill.service.SeckillService;
import com.atguigu.gulimall.seckill.to.SeckillSkuRedisTo;
import com.atguigu.gulimall.seckill.vo.SeckillKillVo;
import com.atguigu.gulimall.seckill.vo.SeckillSkuVo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.math.BigDecimal;

@Service
public class SeckillServiceImpl implements SeckillService {

    private static final String KILL_LUA =
            "if redis.call('exists', KEYS[1]) == 1 then return -2 end " +
            "local stock = tonumber(redis.call('get', KEYS[2]) or '0') " +
            "if stock < tonumber(ARGV[1]) then return -1 end " +
            "redis.call('decrby', KEYS[2], ARGV[1]) " +
            "redis.call('set', KEYS[1], ARGV[1]) " +
            "redis.call('expire', KEYS[1], ARGV[2]) " +
            "redis.call('zadd', KEYS[3], ARGV[3], ARGV[4]) " +
            "return 1";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductFeignService productFeignService;

    @Override
    public List<SeckillSkuVo> currentSeckillSkus() {
        long now = System.currentTimeMillis();
        List<Session> activeSessions = findSessions(now, false);
        if (activeSessions.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> skuKeys = new ArrayList<>();
        for (Session s : activeSessions) {
            skuKeys.addAll(s.skuKeys);
        }
        List<String> jsonList = redisTemplate.opsForValue().multiGet(skuKeys);
        if (jsonList == null) {
            return Collections.emptyList();
        }

        List<SeckillSkuRedisTo> tos = new ArrayList<>();
        for (String json : jsonList) {
            if (json == null) {
                continue;
            }
            try {
                tos.add(objectMapper.readValue(json, SeckillSkuRedisTo.class));
            } catch (Exception e) {
                // 单条数据损坏不影响整体
            }
        }
        if (tos.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, SkuSeckillInfoVo> infoMap = fetchSkuInfo(tos);
        return tos.stream()
                .sorted(Comparator
                        .comparing((SeckillSkuRedisTo t) -> {
                            long start = t.getStartTime() == null
                                    ? Long.MIN_VALUE : t.getStartTime().getTime();
                            return start <= now ? 0L : start;
                        })
                        .thenComparing(SeckillSkuRedisTo::getSeckillSort,
                                Comparator.nullsLast(Integer::compareTo)))
                .map(t -> {
                    SeckillSkuVo vo = toVo(t, infoMap.get(t.getSkuId()), now);
                    if (vo.getStartTime() != null && vo.getStartTime() > now) {
                        vo.setRandomCode(null);
                    }
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public SeckillSkuVo seckillSkuInfo(Long skuId, Long sessionId) {
        long now = System.currentTimeMillis();
        if (sessionId != null) {
            String key = SeckillConstant.SECKILL_SKU_PREFIX + sessionId + "_" + skuId;
            String json = redisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(json)) {
                return null;
            }
            try {
                SeckillSkuRedisTo t = objectMapper.readValue(json, SeckillSkuRedisTo.class);
                if (t.getEndTime() != null && t.getEndTime().getTime() < now) {
                    return null;
                }
                Map<Long, SkuSeckillInfoVo> infoMap = fetchSkuInfo(Collections.singletonList(t));
                SkuSeckillInfoVo info = infoMap == null ? null : infoMap.get(skuId);
                SeckillSkuVo vo = toVo(t, info, now);
                if (vo.getStartTime() != null && vo.getStartTime() > now) {
                    vo.setRandomCode(null);
                }
                return vo;
            } catch (Exception e) {
                return null;
            }
        }
        List<Session> liveSessions = findSessions(now, false);
        for (Session s : liveSessions) {
            String targetKey = null;
            for (String key : s.skuKeys) {
                Long parsed = parseSkuId(key);
                if (parsed != null && parsed.equals(skuId)) {
                    targetKey = key;
                    break;
                }
            }
            if (targetKey == null) {
                continue;
            }
            String json = redisTemplate.opsForValue().get(targetKey);
            if (json == null) {
                continue;
            }
            SeckillSkuRedisTo t;
            try {
                t = objectMapper.readValue(json, SeckillSkuRedisTo.class);
            } catch (Exception e) {
                continue;
            }
            Map<Long, SkuSeckillInfoVo> infoMap = fetchSkuInfo(Collections.singletonList(t));
            SkuSeckillInfoVo info = infoMap == null ? null : infoMap.get(skuId);
            SeckillSkuVo vo = toVo(t, info, now);
            if (vo.getStartTime() != null && vo.getStartTime() > now) {
                vo.setRandomCode(null);
            }
            return vo;
        }
        return null;
    }

    @Override
    public SeckillKillVo kill(Long sessionId, Long skuId, String code, Integer num) {
        MemberLoginVo loginUser = SeckillLoginInterceptor.LOGIN_USER.get();
        if (loginUser == null) {
            throw new SeckillException("请先登录");
        }
        long now = System.currentTimeMillis();
        String skuKey = SeckillConstant.SECKILL_SKU_PREFIX + sessionId + "_" + skuId;
        String json = redisTemplate.opsForValue().get(skuKey);
        if (!StringUtils.hasText(json)) {
            throw new SeckillException("秒杀活动不存在或已结束");
        }
        SeckillSkuRedisTo sku;
        try {
            sku = objectMapper.readValue(json, SeckillSkuRedisTo.class);
        } catch (Exception e) {
            throw new SeckillException("秒杀活动数据异常");
        }
        if (code == null || !code.equals(sku.getRandomCode())) {
            throw new SeckillException("随机码校验失败");
        }
        if (sku.getStartTime() == null || sku.getEndTime() == null) {
            throw new SeckillException("秒杀时间数据异常");
        }
        if (now < sku.getStartTime().getTime()) {
            throw new SeckillException("秒杀还未开始");
        }
        if (now > sku.getEndTime().getTime()) {
            throw new SeckillException("秒杀已结束");
        }
        if (num == null || num <= 0) {
            throw new SeckillException("购买数量不合法");
        }
        if (sku.getSeckillLimit() != null && BigDecimal.valueOf(num).compareTo(sku.getSeckillLimit()) > 0) {
            throw new SeckillException("超过每人限购数量");
        }

        String member = loginUser.getMemberId() + ":" + sessionId + "_" + skuId;
        String okKey = SeckillConstant.SECKILL_OK_PREFIX + member;
        String stockKey = SeckillConstant.SECKILL_STOCK_PREFIX + sessionId + "_" + skuId;
        long deadline = now + SeckillConstant.SECKILL_RESERVE_TIMEOUT_SECONDS * 1000L;

        Long result = redisTemplate.execute(new DefaultRedisScript<>(KILL_LUA, Long.class),
                List.of(okKey, stockKey, SeckillConstant.SECKILL_RESERVE_DEADLINE),
                String.valueOf(num),
                String.valueOf(SeckillConstant.SECKILL_OK_TTL_SECONDS),
                String.valueOf(deadline),
                member);
        if (result == null) {
            throw new SeckillException("抢购失败，请重试");
        }
        if (result == -2L) {
            throw new SeckillException("每人限购，不可重复抢购");
        }
        if (result == -1L) {
            throw new SeckillException("秒杀库存不足");
        }

        SeckillKillVo vo = new SeckillKillVo();
        vo.setSessionId(sessionId);
        vo.setSkuId(skuId);
        vo.setSeckillPrice(sku.getSeckillPrice());
        vo.setNum(num);
        vo.setPayDeadline(deadline);
        return vo;
    }

    private Map<Long, SkuSeckillInfoVo> fetchSkuInfo(List<SeckillSkuRedisTo> tos) {
        List<Long> skuIds = tos.stream().map(SeckillSkuRedisTo::getSkuId)
                .distinct().collect(Collectors.toList());
        if (skuIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return productFeignService.getSeckillInfo(skuIds);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private SeckillSkuVo toVo(SeckillSkuRedisTo t, SkuSeckillInfoVo info, long now) {
        SeckillSkuVo vo = new SeckillSkuVo();
        vo.setSessionId(t.getSessionId());
        vo.setSkuId(t.getSkuId());
        if (info != null) {
            vo.setSkuName(info.getSkuName());
            vo.setSkuDefaultImg(info.getSkuDefaultImg());
            vo.setPrice(info.getPrice());
        }
        vo.setSeckillPrice(t.getSeckillPrice());
        vo.setSeckillCount(t.getSeckillCount());
        vo.setSeckillLimit(t.getSeckillLimit());
        if (t.getStartTime() != null) {
            vo.setStartTime(t.getStartTime().getTime());
        }
        if (t.getEndTime() != null) {
            vo.setEndTime(t.getEndTime().getTime());
        }
        vo.setRandomCode(t.getRandomCode());
        return vo;
    }

    private List<Session> findSessions(long now, boolean activeOnly) {
        Set<String> keys = redisTemplate.keys(SeckillConstant.SECKILL_SESSION_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        List<Session> sessions = new ArrayList<>();
        for (String key : keys) {
            long start;
            long end;
            try {
                String timePart = key.substring(SeckillConstant.SECKILL_SESSION_PREFIX.length());
                int idx = timePart.indexOf('_');
                start = Long.parseLong(timePart.substring(0, idx));
                end = Long.parseLong(timePart.substring(idx + 1));
            } catch (Exception e) {
                continue;
            }
            if (activeOnly && (start > now || end < now)) {
                continue;
            }
            if (!activeOnly && end < now) {
                continue;
            }

            String value = redisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(value)) {
                continue;
            }
            List<String> skuKeys = new ArrayList<>();
            for (String item : value.split(",")) {
                if (StringUtils.hasText(item)) {
                    skuKeys.add(item);
                }
            }
            sessions.add(new Session(start, end, skuKeys));
        }
        sessions.sort(Comparator.comparingLong(s -> s.start));
        return sessions;
    }

    private Long parseSkuId(String skuKey) {
        try {
            String body = skuKey.substring(SeckillConstant.SECKILL_SKU_PREFIX.length());
            int idx = body.lastIndexOf('_');
            return Long.parseLong(body.substring(idx + 1));
        } catch (Exception e) {
            return null;
        }
    }

    private static class Session {
        long start;
        long end;
        List<String> skuKeys;

        Session(long start, long end, List<String> skuKeys) {
            this.start = start;
            this.end = end;
            this.skuKeys = skuKeys;
        }
    }
}
