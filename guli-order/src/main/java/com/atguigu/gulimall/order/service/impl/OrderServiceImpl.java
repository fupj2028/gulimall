package com.atguigu.gulimall.order.service.impl;

import com.atguigu.gulimall.common.Exception.NoStockException;
import com.atguigu.gulimall.common.Exception.OrderExpireException;
import com.atguigu.gulimall.common.Exception.PriceMismatchException;
import com.atguigu.gulimall.common.constant.MqConstant;
import com.atguigu.gulimall.common.constant.OrderConstant;
import com.atguigu.gulimall.common.to.SkuHasStockVo;
import com.atguigu.gulimall.common.to.SkuSeckillInfoVo;
import com.atguigu.gulimall.common.to.StockLockedTo;
import com.atguigu.gulimall.common.to.WareSkuLockItem;
import com.atguigu.gulimall.common.to.WareSkuLockVo;
import com.atguigu.gulimall.common.utils.PageUtils;
import com.atguigu.gulimall.common.utils.Query;
import com.atguigu.gulimall.common.vo.MemberLoginVo;
import com.atguigu.gulimall.common.vo.MemberOrderItemVo;
import com.atguigu.gulimall.common.vo.MemberOrderVo;
import com.atguigu.gulimall.order.dao.OrderDao;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.entity.OrderItemEntity;
import com.atguigu.gulimall.order.entity.PaymentInfoEntity;
import com.atguigu.gulimall.order.feign.CartFeignService;
import com.atguigu.gulimall.order.feign.MemberFeignService;
import com.atguigu.gulimall.order.feign.ProductFeignService;
import com.atguigu.gulimall.order.feign.WareFeignService;
import com.atguigu.gulimall.order.intercepter.OrderIntercepter;
import com.atguigu.gulimall.order.config.ReliableRabbitTemplate;
import com.atguigu.gulimall.order.service.OrderItemService;
import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.service.PaymentInfoService;
import com.atguigu.gulimall.order.vo.ConfirmItemVo;
import com.atguigu.gulimall.order.vo.FareVo;
import com.atguigu.gulimall.order.vo.MemberAddressVo;
import com.atguigu.gulimall.order.vo.OrderCreateTo;
import com.atguigu.gulimall.order.vo.OrderItemVo;
import com.atguigu.gulimall.order.vo.OrderSubmitVo;
import com.atguigu.gulimall.order.vo.PayAsyncVo;
import com.atguigu.gulimall.order.vo.PayVo;
import com.atguigu.gulimall.order.vo.SkuOrderInfoVo;
import com.atguigu.gulimall.order.vo.SubmitOrderResponseVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
// Seata 已移出：库存锁定改走本地事务 + MQ 延时解锁（见 RabbitMqConfig.stock.delay.queue）
// import org.apache.seata.spring.annotation.GlobalTransactional;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.UUID;

@Slf4j
@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    private static final String SECKILL_SKU_PREFIX = "seckill:skus:";
    private static final String SECKILL_OK_PREFIX = "seckill:ok:";
    private static final String SECKILL_RESERVE_DEADLINE = "seckill:reserve:deadline";

    @Autowired
    private CartFeignService cartFeignService;

    @Autowired
    private MemberFeignService memberFeignService;

    @Autowired
    private WareFeignService wareFeignService;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private ThreadPoolTaskExecutor executor;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ReliableRabbitTemplate rabbitTemplate;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private PaymentInfoService paymentInfoService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>());
        return new PageUtils(page);
    }

    @Override
    public ConfirmItemVo confirmOrder() {
        ConfirmItemVo confirmVo = new ConfirmItemVo();
        MemberLoginVo loginUser = OrderIntercepter.threadLocal.get();

        CompletableFuture<Void> cartFuture = CompletableFuture.runAsync(() -> {
            List<OrderItemVo> items = cartFeignService.checkedItems();
            List<Long> skuIds = items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
            Map<Long, BigDecimal> prices = productFeignService.getPrices(skuIds);
            items.forEach(item -> item.setPrice(prices.get(item.getSkuId())));
            confirmVo.setItems(items);
            BigDecimal total = items.stream()
                    .map(i -> i.getPrice().multiply(new BigDecimal(i.getCount())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            confirmVo.setTotal(total);
            confirmVo.setPayPrice(total);

            List<SkuHasStockVo> stockList = wareFeignService.getSkuHasStock(skuIds);
            Map<Long, Boolean> stockMap = stockList.stream()
                    .collect(Collectors.toMap(SkuHasStockVo::getSkuId, SkuHasStockVo::getHasStock));
            confirmVo.setStockMap(stockMap);
        }, executor);

        CompletableFuture<Void> memberFuture = CompletableFuture.runAsync(() -> {
            List<MemberAddressVo> addresses = memberFeignService.getAddressesByMemberId(loginUser.getMemberId());

            confirmVo.setMemberAddresses(addresses);
            confirmVo.setIntegration(loginUser.getIntegration());

            MemberAddressVo defaultAddr = addresses.stream()
                    .filter(a -> a.getDefaultStatus() == 1)
                    .findFirst().orElse(addresses.isEmpty() ? null : addresses.get(0));
            if (defaultAddr != null) {
                FareVo fareVo = new FareVo();
                fareVo.setAddress(defaultAddr);
                String phone = defaultAddr.getPhone();
                BigDecimal fare = new BigDecimal(phone.charAt(phone.length() - 1) - '0');
                fareVo.setFare(fare);
                confirmVo.setFareVo(fareVo);
            }
        }, executor);

        CompletableFuture.allOf(memberFuture,cartFuture).join();

        BigDecimal payPrice = confirmVo.getPayPrice();
        if (payPrice == null) {
            payPrice = BigDecimal.ZERO;
        }
        if (confirmVo.getFareVo() != null && confirmVo.getFareVo().getFare() != null) {
            payPrice = payPrice.add(confirmVo.getFareVo().getFare());
        }
        confirmVo.setPayPrice(payPrice);

        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX + loginUser.getMemberId(), token, 30, TimeUnit.MINUTES);
        confirmVo.setOrderToken(token);
        return confirmVo;
    }

    @Override
    public ConfirmItemVo confirmSeckillOrder(Long sessionId, Long skuId, Integer num) {
        MemberLoginVo loginUser = OrderIntercepter.threadLocal.get();

        //校验抢购资格：秒杀预留标记必须存在（kill 成功时写入）
        String okKey = SECKILL_OK_PREFIX + loginUser.getMemberId() + ":" + sessionId + "_" + skuId;
        if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(okKey))) {
            throw new OrderExpireException("请先抢购秒杀商品");
        }

        JsonNode skuNode = getSeckillSku(sessionId, skuId);
        if (skuNode == null) {
            throw new OrderExpireException("秒杀活动不存在或已结束");
        }
        long end = parseEpoch(skuNode.path("endTime").asText(null));
        if (end > 0 && System.currentTimeMillis() > end) {
            throw new OrderExpireException("秒杀已结束");
        }

        int count = (num == null || num <= 0) ? 1 : num;
        BigDecimal seckillPrice = new BigDecimal(skuNode.path("seckillPrice").asText("0"));
        Map<Long, SkuSeckillInfoVo> infoMap = productFeignService.getSeckillInfo(Collections.singletonList(skuId));
        SkuSeckillInfoVo info = infoMap == null ? null : infoMap.get(skuId);

        ConfirmItemVo confirmVo = new ConfirmItemVo();
        List<OrderItemVo> items = new ArrayList<>();
        OrderItemVo item = new OrderItemVo();
        item.setSkuId(skuId);
        item.setTitle(info != null ? info.getSkuName() : "秒杀商品");
        item.setImage(info != null ? info.getSkuDefaultImg() : null);
        item.setPrice(seckillPrice);
        item.setCount(count);
        items.add(item);
        confirmVo.setItems(items);
        confirmVo.setStockMap(Collections.singletonMap(skuId, true));
        confirmVo.setTotal(seckillPrice.multiply(new BigDecimal(count)));
        confirmVo.setPayPrice(seckillPrice.multiply(new BigDecimal(count)));

        CompletableFuture<Void> memberFuture = CompletableFuture.runAsync(() -> {
            List<MemberAddressVo> addresses = memberFeignService.getAddressesByMemberId(loginUser.getMemberId());
            confirmVo.setMemberAddresses(addresses);
            confirmVo.setIntegration(loginUser.getIntegration());
            MemberAddressVo defaultAddr = addresses.stream()
                    .filter(a -> a.getDefaultStatus() == 1)
                    .findFirst().orElse(addresses.isEmpty() ? null : addresses.get(0));
            if (defaultAddr != null) {
                FareVo fareVo = new FareVo();
                fareVo.setAddress(defaultAddr);
                String phone = defaultAddr.getPhone();
                BigDecimal fare = new BigDecimal(phone.charAt(phone.length() - 1) - '0');
                fareVo.setFare(fare);
                confirmVo.setFareVo(fareVo);
            }
        }, executor);
        memberFuture.join();

        BigDecimal payPrice = confirmVo.getPayPrice();
        if (payPrice == null) {
            payPrice = BigDecimal.ZERO;
        }
        if (confirmVo.getFareVo() != null && confirmVo.getFareVo().getFare() != null) {
            payPrice = payPrice.add(confirmVo.getFareVo().getFare());
        }
        confirmVo.setPayPrice(payPrice);

        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX + loginUser.getMemberId(),
                token, 30, TimeUnit.MINUTES);
        confirmVo.setOrderToken(token);
        return confirmVo;
    }

    /**
     * 从 Redis 读取秒杀商品信息（seckill:skus:{sessionId}_{skuId}），返回原始 JSON 节点
     */
    private JsonNode getSeckillSku(Long sessionId, Long skuId) {
        String json = stringRedisTemplate.opsForValue().get(SECKILL_SKU_PREFIX + sessionId + "_" + skuId);
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            log.error("解析秒杀商品信息失败, sessionId={}, skuId={}", sessionId, skuId, e);
            return null;
        }
    }

    /**
     * Redis 中秒杀时间为 +00:00 时区的 ISO 字符串，转成毫秒时间戳与本地时间比较
     */
    private long parseEpoch(String text) {
        if (text == null || text.isEmpty()) {
            return 0L;
        }
        try {
            return OffsetDateTime.parse(text).toInstant().toEpochMilli();
        } catch (Exception e) {
            return 0L;
        }
    }

    @Override
    public FareVo getFare(Long addressId, Long memberId) {
        List<MemberAddressVo> addresses = memberFeignService.getAddressesByMemberId(memberId);
        MemberAddressVo addr = null;
        if (addresses != null) {
            addr = addresses.stream()
                    .filter(a -> a.getId().equals(addressId))
                    .findFirst().orElse(null);
        }

        BigDecimal fare = BigDecimal.ZERO;
        if (addr != null && addr.getPhone() != null && !addr.getPhone().isEmpty()) {
            char last = addr.getPhone().charAt(addr.getPhone().length() - 1);
            fare = new BigDecimal(last - '0');
        }

        FareVo fareVo = new FareVo();
        fareVo.setAddress(addr);
        fareVo.setFare(fare);
        return fareVo;
    }

    @Override
    // Seata 已移出，改为本地事务 + MQ 补偿：库存锁定失败/超时由 stock.delay.queue 延时解锁兜底
    // @GlobalTransactional(name = "gulimall-create-order", rollbackFor = Exception.class)
    @Transactional(rollbackFor = Exception.class)
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo submitVo) {

        SubmitOrderResponseVo res = new SubmitOrderResponseVo();

        MemberLoginVo loginInfo = OrderIntercepter.threadLocal.get();

        String script = "if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
        String token = submitVo.getOrderToken();
        Long result = (Long)redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class),Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX+loginInfo.getMemberId()),token);
        if(result==0L){
            //令牌失效：可能是重复提交，查已提交订单，命中则直接返回（幂等），否则报令牌失效
            OrderEntity submitted = findSubmittedOrder(loginInfo.getMemberId());
            if (submitted != null) {
                res.setOrder(submitted);
                res.setCode(0);
                return res;
            }
            res.setCode(1);
            return res;
        }else{
            //秒杀下单：sessionId 非空走秒杀分支（商品来自秒杀场次，价格取 Redis 秒杀价）
            boolean seckill = submitVo.getSessionId() != null;
            //构建订单（只算不存）
            OrderCreateTo order = seckill ? createSeckillOrder(submitVo) : createOrder(submitVo);
            //校验前端提交的价格与后端重新计算的价格是否一致，不一致抛异常，触发全局回滚
            if (!checkOrderPrice(order.getOrderEntity(), submitVo.getPayPrice())) {
                throw new PriceMismatchException();
            }
            //锁定库存，任一SKU失败则抛异常，整单失败
            StockLockedTo locked = lockStock(order);
            //保存库存锁定信息，供支付宝超时收单回调时发送关单消息（TTL 40分钟 > 关单延时 30分钟）
            try {
                redisTemplate.opsForValue().set(OrderConstant.ORDER_LOCK_PREFIX + locked.getOrderSn(),
                        objectMapper.writeValueAsString(locked), 40, TimeUnit.MINUTES);
            } catch (JsonProcessingException e) {
                log.error("保存库存锁定信息失败，orderSn={}", locked.getOrderSn(), e);
            }
            //发送解锁库存延时消息（订单尚未创建，作为下单失败/超时未支付的兜底补偿）
            rabbitTemplate.convertAndSend(MqConstant.STOCK_EVENT_EXCHANGE,
                    MqConstant.STOCK_LOCK_DELAY_ROUTING, locked);
            OrderEntity saved = saveOrder(order);
            //购物车下单成功：从购物车删除本次下单的商品（秒杀单商品不在购物车，不处理）
            if (!seckill) {
                clearCartItems(order);
            }
            //发送延时关单消息（超时未支付自动关单）
            rabbitTemplate.convertAndSend(MqConstant.ORDER_EVENT_EXCHANGE,
                    MqConstant.ORDER_CREATE_ORDER_ROUTING, locked);
            //秒杀下单成功：删除抢购资格，并从预约表移除成员，防止定时任务误回补库存
            if (seckill) {
                releaseSeckillReserve(loginInfo.getMemberId(), submitVo.getSessionId(), submitVo.getSkuId());
            }
            res.setOrder(saved);
            res.setCode(0);
        }

        return res;
    }

    /**
     * 秒杀下单：商品来自秒杀场次，价格重新从 Redis 读取秒杀价，校验资格与结束时间
     */
    private OrderCreateTo createSeckillOrder(OrderSubmitVo submitVo) {
        MemberLoginVo loginUser = OrderIntercepter.threadLocal.get();
        Long sessionId = submitVo.getSessionId();
        Long skuId = submitVo.getSkuId();
        int count = (submitVo.getNum() == null || submitVo.getNum() <= 0) ? 1 : submitVo.getNum();

        //重校验抢购资格（确认页与提交之间可能超时）
        String okKey = SECKILL_OK_PREFIX + loginUser.getMemberId() + ":" + sessionId + "_" + skuId;
        if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(okKey))) {
            throw new OrderExpireException("秒杀资格已失效，请重新抢购");
        }
        JsonNode skuNode = getSeckillSku(sessionId, skuId);
        if (skuNode == null) {
            throw new OrderExpireException("秒杀活动不存在或已结束");
        }
        long end = parseEpoch(skuNode.path("endTime").asText(null));
        if (end > 0 && System.currentTimeMillis() > end) {
            throw new OrderExpireException("秒杀已结束");
        }
        //价格以 Redis 秒杀价为准，不信任前端提交的价格
        BigDecimal seckillPrice = new BigDecimal(skuNode.path("seckillPrice").asText("0"));

        String orderSn = IdWorker.getTimeId();
        OrderCreateTo order = new OrderCreateTo();
        order.setOrderSn(orderSn);

        FareVo fareVo = getFare(submitVo.getAddrId(), loginUser.getMemberId());
        order.setFare(fareVo.getFare().intValue());

        Map<Long, SkuSeckillInfoVo> infoMap = productFeignService.getSeckillInfo(Collections.singletonList(skuId));
        SkuSeckillInfoVo info = infoMap == null ? null : infoMap.get(skuId);

        List<OrderItemVo> items = new ArrayList<>();
        OrderItemVo item = new OrderItemVo();
        item.setSkuId(skuId);
        item.setTitle(info != null ? info.getSkuName() : "秒杀商品");
        item.setImage(info != null ? info.getSkuDefaultImg() : null);
        item.setPrice(seckillPrice);
        item.setCount(count);
        items.add(item);

        List<OrderItemEntity> orderItems = buildOrderItems(items, orderSn);
        order.setOrders(orderItems);

        BigDecimal total = sumItemAmount(items);
        BigDecimal payAmount = total.add(fareVo.getFare());
        order.setPayload(payAmount);

        OrderEntity orderEntity = buildOrderEntity(loginUser, submitVo, orderSn, fareVo, total, payAmount, orderItems);
        order.setOrderEntity(orderEntity);
        return order;
    }

    /**
     * 秒杀下单成功：删除抢购资格，并从预约表移除成员，防止 SeckillReserveReleaseScheduled 误回补库存
     */
    private void releaseSeckillReserve(Long memberId, Long sessionId, Long skuId) {
        String okKey = SECKILL_OK_PREFIX + memberId + ":" + sessionId + "_" + skuId;
        stringRedisTemplate.delete(okKey);
        stringRedisTemplate.opsForZSet().remove(SECKILL_RESERVE_DEADLINE, memberId + ":" + sessionId + "_" + skuId);
    }

    private StockLockedTo lockStock(OrderCreateTo order) {
        WareSkuLockVo lockVo = new WareSkuLockVo();
        lockVo.setOrderSn(order.getOrderSn());
        List<WareSkuLockItem> locks = order.getOrders().stream().map(item -> {
            WareSkuLockItem lockItem = new WareSkuLockItem();
            lockItem.setSkuId(item.getSkuId());
            lockItem.setNum(item.getSkuQuantity());
            return lockItem;
        }).collect(Collectors.toList());
        lockVo.setLocks(locks);
        try {
            //ware 端返回带 taskId + 锁定明细的解锁消息体，后续关单时原样转发
            return wareFeignService.lockStock(lockVo);
        } catch (Exception e) {
            //Feign 不会透传服务端异常类型，任何锁定失败（真缺货/超时/解码失败）都会走到这里。
            //必须记录原始异常，否则所有原因都被伪装成"库存不足"，无法排查。
            log.error("锁定库存失败, orderSn={}", order.getOrderSn(), e);
            throw new NoStockException(null);
        }
    }

    private boolean checkOrderPrice(OrderEntity order, BigDecimal payPrice) {
        if (payPrice == null || order.getPayAmount() == null) {
            return false;
        }
        return order.getPayAmount().subtract(payPrice).abs().compareTo(new BigDecimal("0.01")) <= 0;
    }

    private OrderCreateTo createOrder(OrderSubmitVo submitVo) {
        MemberLoginVo loginUser = OrderIntercepter.threadLocal.get();

        //创建订单号
        String orderSn = IdWorker.getTimeId();

        OrderCreateTo order = new OrderCreateTo();
        order.setOrderSn(orderSn);

        //远程查询收货地址信息并计算运费
        FareVo fareVo = getFare(submitVo.getAddrId(), loginUser.getMemberId());
        order.setFare(fareVo.getFare().intValue());

        //远程查询购物车勾选的商品，并更新实时价格
        List<OrderItemVo> items = getCheckedItems();

        //构建订单项
        List<OrderItemEntity> orderItems = buildOrderItems(items, orderSn);
        order.setOrders(orderItems);

        //计算应付金额
        BigDecimal total = sumItemAmount(items);
        BigDecimal payAmount = total.add(fareVo.getFare());
        order.setPayload(payAmount);

        //构建订单主表
        OrderEntity orderEntity = buildOrderEntity(loginUser, submitVo, orderSn, fareVo, total, payAmount, orderItems);
        order.setOrderEntity(orderEntity);

        return order;
    }

    private List<OrderItemVo> getCheckedItems() {
        List<OrderItemVo> items = cartFeignService.checkedItems();
        List<Long> skuIds = items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
        Map<Long, BigDecimal> prices = productFeignService.getPrices(skuIds);
        items.forEach(item -> item.setPrice(prices.get(item.getSkuId())));
        return items;
    }

    private List<OrderItemEntity> buildOrderItems(List<OrderItemVo> items, String orderSn) {
        List<Long> skuIds = items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
        Map<Long, SkuOrderInfoVo> skuOrderMap = productFeignService.getSkuOrderInfos(skuIds);
        return items.stream()
                .map(item -> buildOrderItem(item, orderSn, skuOrderMap))
                .collect(Collectors.toList());
    }

    private OrderItemEntity buildOrderItem(OrderItemVo item, String orderSn, Map<Long, SkuOrderInfoVo> skuOrderMap) {
        OrderItemEntity entity = new OrderItemEntity();
        entity.setOrderSn(orderSn);
        entity.setSkuId(item.getSkuId());
        entity.setSkuName(item.getTitle());
        entity.setSkuPic(item.getImage());
        entity.setSkuPrice(item.getPrice());
        entity.setSkuQuantity(item.getCount());
        entity.setSkuAttrsVals(item.getAttrs() != null ? String.join(";", item.getAttrs()) : null);

        SkuOrderInfoVo skuOrderInfo = skuOrderMap.get(item.getSkuId());
        if (skuOrderInfo != null) {
            entity.setSpuId(skuOrderInfo.getSpuId());
            entity.setSpuName(skuOrderInfo.getSpuName());
            entity.setSpuBrand(skuOrderInfo.getSpuBrand());
            entity.setCategoryId(skuOrderInfo.getCategoryId());
        }
        entity.setSpuPic(item.getImage());

        BigDecimal realAmount = item.getPrice().multiply(new BigDecimal(item.getCount()));
        entity.setRealAmount(realAmount);
        entity.setGiftIntegration(realAmount.intValue());
        entity.setGiftGrowth(realAmount.intValue());
        return entity;
    }

    private BigDecimal sumItemAmount(List<OrderItemVo> items) {
        return items.stream()
                .map(i -> i.getPrice().multiply(new BigDecimal(i.getCount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private OrderEntity buildOrderEntity(MemberLoginVo loginUser, OrderSubmitVo submitVo, String orderSn,
                                         FareVo fareVo, BigDecimal total, BigDecimal payAmount,
                                         List<OrderItemEntity> orderItems) {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(orderSn);
        orderEntity.setMemberId(loginUser.getMemberId());
        orderEntity.setMemberUsername(loginUser.getUsername());
        orderEntity.setTotalAmount(total);
        orderEntity.setPayAmount(payAmount);
        orderEntity.setFreightAmount(fareVo.getFare());
        orderEntity.setPromotionAmount(BigDecimal.ZERO);
        orderEntity.setIntegrationAmount(BigDecimal.ZERO);
        orderEntity.setCouponAmount(BigDecimal.ZERO);
        orderEntity.setDiscountAmount(BigDecimal.ZERO);
        orderEntity.setPayType("货到付款".equals(submitVo.getPayType()) ? 4 : 1);
        orderEntity.setStatus(OrderConstant.ORDER_STATUS_PENDING_PAY);
        orderEntity.setCreateTime(new Date());
        orderEntity.setIntegration(sumIntegration(orderItems));
        orderEntity.setGrowth(sumGrowth(orderItems));
        fillReceiverInfo(orderEntity, fareVo.getAddress());
        orderEntity.setConfirmStatus(0);
        orderEntity.setDeleteStatus(0);
        return orderEntity;
    }

    private int sumIntegration(List<OrderItemEntity> orderItems) {
        return orderItems.stream().mapToInt(OrderItemEntity::getGiftIntegration).sum();
    }

    private int sumGrowth(List<OrderItemEntity> orderItems) {
        return orderItems.stream().mapToInt(OrderItemEntity::getGiftGrowth).sum();
    }

    private void fillReceiverInfo(OrderEntity orderEntity, MemberAddressVo addr) {
        if (addr != null) {
            orderEntity.setReceiverName(addr.getName());
            orderEntity.setReceiverPhone(addr.getPhone());
            orderEntity.setReceiverPostCode(addr.getPostCode());
            orderEntity.setReceiverProvince(addr.getProvince());
            orderEntity.setReceiverCity(addr.getCity());
            orderEntity.setReceiverRegion(addr.getRegion());
            orderEntity.setReceiverDetailAddress(addr.getDetailAddress());
        }
    }

    private OrderEntity saveOrder(OrderCreateTo order) {
        OrderEntity orderEntity = order.getOrderEntity();
        this.save(orderEntity);
        order.getOrders().forEach(item -> item.setOrderId(orderEntity.getId()));
        orderItemService.saveBatch(order.getOrders());

        //保存成功后记录订单号，用于重复提交时的幂等返回
        MemberLoginVo loginUser = OrderIntercepter.threadLocal.get();
        if (loginUser != null) {
            redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_SN_PREFIX + loginUser.getMemberId(),
                    orderEntity.getOrderSn(), 30, TimeUnit.MINUTES);
        }
        return orderEntity;
    }

    private void clearCartItems(OrderCreateTo order) {
        order.getOrders().forEach(item -> {
            try {
                cartFeignService.deleteCartItem(item.getSkuId());
            } catch (Exception e) {
                log.warn("删除购物车商品失败, skuId={}, orderSn={}", item.getSkuId(), order.getOrderSn(), e);
            }
        });
    }

    private OrderEntity findSubmittedOrder(Long memberId) {
        Object orderSn = redisTemplate.opsForValue().get(OrderConstant.USER_ORDER_SN_PREFIX + memberId);
        if (orderSn == null) {
            return null;
        }
        return this.getOne(new QueryWrapper<OrderEntity>()
                .eq("order_sn", orderSn.toString())
                .last("limit 1"));
    }

    @Override
    public void closeOrder(StockLockedTo to) {
        String orderSn = to.getOrderSn();
        OrderEntity order = this.getOne(new QueryWrapper<OrderEntity>()
                .eq("order_sn", orderSn)
                .last("limit 1"));
        if (order == null || order.getStatus() != OrderConstant.ORDER_STATUS_PENDING_PAY) {
            return;
        }
        order.setStatus(OrderConstant.ORDER_STATUS_CLOSED);
        this.updateById(order);
        log.info("订单超时未支付，已关闭，orderSn={}", orderSn);

        //转发库存解锁消息（原样携带锁定明细）
        rabbitTemplate.convertAndSend(MqConstant.STOCK_EVENT_EXCHANGE,
                MqConstant.STOCK_RELEASE_ROUTING, to);
    }

    @Override
    public Integer getOrderStatus(String orderSn) {
        OrderEntity order = this.getOne(new QueryWrapper<OrderEntity>()
                .select("status")
                .eq("order_sn", orderSn)
                .last("limit 1"));
        return order == null ? null : order.getStatus();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePayNotify(PayAsyncVo payAsyncVo) {
        String orderSn = payAsyncVo.getOutTradeNo();
        OrderEntity order = this.getOne(new QueryWrapper<OrderEntity>()
                .eq("order_sn", orderSn)
                .last("limit 1"));
        //订单不存在或非待付款状态，直接返回（幂等，防止重复回调重复处理）
        if (order == null || order.getStatus() != OrderConstant.ORDER_STATUS_PENDING_PAY) {
            log.info("支付回调忽略，orderSn={}, 状态={}", orderSn, order == null ? null : order.getStatus());
            return;
        }

        //记录支付信息
        PaymentInfoEntity paymentInfo = new PaymentInfoEntity();
        paymentInfo.setOrderSn(orderSn);
        paymentInfo.setOrderId(order.getId());
        paymentInfo.setAlipayTradeNo(payAsyncVo.getTradeNo());
        BigDecimal totalAmount = payAsyncVo.getTotalAmount() == null
                ? null : new BigDecimal(payAsyncVo.getTotalAmount());
        paymentInfo.setTotalAmount(totalAmount == null ? order.getPayAmount() : totalAmount);
        paymentInfo.setSubject(payAsyncVo.getSubject() == null ? "谷粒商城订单-" + orderSn : payAsyncVo.getSubject());
        paymentInfo.setPaymentStatus(payAsyncVo.getTradeStatus());
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setConfirmTime(new Date());
        paymentInfo.setCallbackContent(payAsyncVo.toString());
        paymentInfo.setCallbackTime(new Date());
        paymentInfoService.save(paymentInfo);

        //订单改为待发货
        order.setStatus(OrderConstant.ORDER_STATUS_TO_DELIVER);
        order.setPaymentTime(new Date());
        this.updateById(order);
        log.info("订单支付成功，orderSn={}, 支付金额={}", orderSn, paymentInfo.getTotalAmount());

        //支付成功：通知 ware 将锁定库存转为实销（stock 与 stock_locked 同时扣减）
        sendStockDeductMessage(orderSn);
    }

    private void sendStockDeductMessage(String orderSn) {
        Object cached = redisTemplate.opsForValue().get(OrderConstant.ORDER_LOCK_PREFIX + orderSn);
        if (cached == null) {
            log.warn("支付成功但库存锁定信息不存在，orderSn={}，暂不扣减库存", orderSn);
            return;
        }
        try {
            StockLockedTo to = objectMapper.readValue(cached.toString(), StockLockedTo.class);
            rabbitTemplate.convertAndSend(MqConstant.STOCK_EVENT_EXCHANGE,
                    MqConstant.STOCK_DEDUCT_ROUTING, to);
            log.info("支付成功，已发送库存扣减消息，orderSn={}", orderSn);
        } catch (JsonProcessingException e) {
            log.error("解析库存锁定信息失败，orderSn={}", orderSn, e);
        }
    }

    @Override
    public void closeOrderByAlipay(String orderSn) {
        Object cached = redisTemplate.opsForValue().get(OrderConstant.ORDER_LOCK_PREFIX + orderSn);
        if (cached == null) {
            log.warn("支付宝超时收单回调，但库存锁定信息不存在，orderSn={}，交由商家延时关单兜底", orderSn);
            return;
        }
        try {
            StockLockedTo to = objectMapper.readValue(cached.toString(), StockLockedTo.class);
            //复用关单管线：OrderCloseListener 关单 + 解锁库存 + 重试/最终死信
            rabbitTemplate.convertAndSend(MqConstant.ORDER_EVENT_EXCHANGE,
                    MqConstant.ORDER_RELEASE_ORDER_ROUTING, to);
            log.info("支付宝超时收单，已发送关单消息，orderSn={}", orderSn);
        } catch (JsonProcessingException e) {
            log.error("解析库存锁定信息失败，orderSn={}", orderSn, e);
        }
    }

    @Override
    public List<MemberOrderVo> listOrdersByMember(Long memberId) {
        List<OrderEntity> orders = this.list(new QueryWrapper<OrderEntity>()
                .eq("member_id", memberId)
                .orderByDesc("create_time"));
        return orders.stream().map(order -> {
            MemberOrderVo vo = new MemberOrderVo();
            vo.setId(order.getId());
            vo.setOrderSn(order.getOrderSn());
            vo.setPayAmount(order.getPayAmount());
            vo.setStatus(order.getStatus());
            vo.setReceiverName(order.getReceiverName());
            vo.setCreateTime(order.getCreateTime());
            List<OrderItemEntity> items = orderItemService.list(new QueryWrapper<OrderItemEntity>()
                    .eq("order_sn", order.getOrderSn()));
            vo.setItems(items.stream().map(item -> {
                MemberOrderItemVo itemVo = new MemberOrderItemVo();
                itemVo.setSkuName(item.getSkuName());
                itemVo.setSkuPic(item.getSkuPic());
                itemVo.setSkuQuantity(item.getSkuQuantity());
                return itemVo;
            }).collect(Collectors.toList()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public PayVo getPayVo(String orderSn) {
        OrderEntity order = this.getOne(new QueryWrapper<OrderEntity>()
                .eq("order_sn", orderSn)
                .last("limit 1"));
        if (order == null) {
            return null;
        }
        PayVo payVo = new PayVo();
        payVo.setOutTradeNo(order.getOrderSn());
        payVo.setTotalAmount(order.getPayAmount().toPlainString());
        payVo.setSubject("谷粒商城订单-" + order.getOrderSn());
        return payVo;
    }
}