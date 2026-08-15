package com.atguigu.gulimall.seckill.scheduled;

import com.atguigu.gulimall.seckill.config.SeckillConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class SeckillReserveReleaseScheduled {

    /**
     * KEYS[1] 到期表  KEYS[2] 资格key  KEYS[3] 库存key  ARGV[1] 成员
     */
    private static final String RELEASE_LUA =
            "if redis.call('zrem', KEYS[1], ARGV[1]) == 1 then " +
            "  local num = tonumber(redis.call('get', KEYS[2]) or '0') " +
            "  if num > 0 then redis.call('incrby', KEYS[3], num) end " +
            "  redis.call('del', KEYS[2]) " +
            "  return 1 " +
            "end " +
            "return 0";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Scheduled(cron = "0 */5 * * * ?")
    public void releaseExpiredReserves() {
        long now = System.currentTimeMillis();
        Set<String> members = redisTemplate.opsForZSet().rangeByScore(
                SeckillConstant.SECKILL_RESERVE_DEADLINE, Double.MIN_VALUE, (double) now);
        if (members == null || members.isEmpty()) {
            return;
        }
        for (String member : members) {
            String[] userParts = member.split(":");
            if (userParts.length != 2) {
                continue;
            }
            String userId = userParts[0];
            String skuPart = userParts[1];
            int idx = skuPart.indexOf('_');
            if (idx <= 0) {
                continue;
            }
            String sessionId = skuPart.substring(0, idx);
            String skuId = skuPart.substring(idx + 1);
            String okKey = SeckillConstant.SECKILL_OK_PREFIX + userId + ":" + sessionId + "_" + skuId;
            String stockKey = SeckillConstant.SECKILL_STOCK_PREFIX + sessionId + "_" + skuId;
            Long released = redisTemplate.execute(new DefaultRedisScript<>(RELEASE_LUA, Long.class),
                    List.of(SeckillConstant.SECKILL_RESERVE_DEADLINE, okKey, stockKey), member);
            if (released != null && released == 1L) {
                log.info("超时释放秒杀资格, sessionId={}, skuId={}, userId={}",
                        sessionId, skuId, userId);
            }
        }
    }
}