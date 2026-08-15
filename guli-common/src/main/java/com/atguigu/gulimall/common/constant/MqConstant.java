package com.atguigu.gulimall.common.constant;

public class MqConstant {

    //订单事件交换机
    public static final String ORDER_EVENT_EXCHANGE = "order-event-exchange";
    public static final String ORDER_CREATE_ORDER_ROUTING = "order.create.order";
    public static final String ORDER_RELEASE_ORDER_ROUTING = "order.release.order";

    //延时队列（TTL 到期后死信投递到关单队列）
    public static final String ORDER_DELAY_QUEUE = "order.delay.queue";
    //关单队列
    public static final String ORDER_RELEASE_ORDER_QUEUE = "order.release.order.queue";
    //关单延时，毫秒（30分钟）
    public static final int ORDER_RELEASE_TTL = 30 * 60 * 1000;

    //库存事件交换机
    public static final String STOCK_EVENT_EXCHANGE = "stock-event-exchange";
    public static final String STOCK_RELEASE_ROUTING = "stock.release";
    public static final String STOCK_RELEASE_STOCK_QUEUE = "stock.release.stock.queue";

    //库存解锁延时队列（下单即入队，作为下单失败/超时未支付的兜底解锁）
    public static final String STOCK_DELAY_QUEUE = "stock.delay.queue";
    public static final String STOCK_LOCK_DELAY_ROUTING = "stock.lock.delay";

    //库存扣减：支付成功后将锁定库存转为实销（stock 与 stock_locked 同时扣减）
    public static final String STOCK_DEDUCT_ROUTING = "stock.deduct";
    public static final String STOCK_DEDUCT_STOCK_QUEUE = "stock.deduct.stock.queue";

    //库存扣减重试队列（TTL 到期后死信回库存扣减队列继续重试）
    public static final String STOCK_DEDUCT_RETRY_QUEUE = "stock.deduct.retry.queue";
    public static final String STOCK_DEDUCT_RETRY_ROUTING = "stock.deduct.retry";
    //库存扣减最终死信队列（超过重试上限，留给人/补偿程序处理）
    public static final String STOCK_DEDUCT_FINAL_QUEUE = "stock.deduct.final.queue";
    public static final String STOCK_DEDUCT_FINAL_ROUTING = "stock.deduct.final";
    public static final int STOCK_DEDUCT_RETRY_TTL = 60 * 1000;
    public static final int STOCK_DEDUCT_MAX_RETRY = 3;

    //通用：重试计数消息头
    public static final String RETRY_COUNT_HEADER = "x-retry-count";

    //关单重试队列（TTL 到期后死信回关单队列继续重试）
    public static final String ORDER_RELEASE_RETRY_QUEUE = "order.release.retry.queue";
    public static final String ORDER_RELEASE_RETRY_ROUTING = "order.release.retry";
    //关单最终死信队列（超过重试上限，留给人/补偿程序处理）
    public static final String ORDER_RELEASE_FINAL_QUEUE = "order.release.final.queue";
    public static final String ORDER_RELEASE_FINAL_ROUTING = "order.release.final";
    public static final int ORDER_RELEASE_RETRY_TTL = 60 * 1000;
    public static final int ORDER_RELEASE_MAX_RETRY = 3;

    //库存解锁重试队列（TTL 到期后死信回库存解锁队列继续重试）
    public static final String STOCK_RELEASE_RETRY_QUEUE = "stock.release.retry.queue";
    public static final String STOCK_RELEASE_RETRY_ROUTING = "stock.release.retry";
    //库存解锁最终死信队列（超过重试上限，留给人/补偿程序处理）
    public static final String STOCK_RELEASE_FINAL_QUEUE = "stock.release.final.queue";
    public static final String STOCK_RELEASE_FINAL_ROUTING = "stock.release.final";
    public static final int STOCK_RELEASE_RETRY_TTL = 60 * 1000;
    public static final int STOCK_RELEASE_MAX_RETRY = 3;
}
