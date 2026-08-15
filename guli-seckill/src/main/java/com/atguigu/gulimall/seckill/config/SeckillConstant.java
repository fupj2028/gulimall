package com.atguigu.gulimall.seckill.config;

public class SeckillConstant {

    public static final String SECKILL_SESSION_PREFIX = "seckill:sessions:";
    public static final String SECKILL_SKU_PREFIX = "seckill:skus:";
    public static final String SECKILL_STOCK_PREFIX = "seckill:stock:";
    public static final String SECKILL_OK_PREFIX = "seckill:ok:";
    public static final String SECKILL_RESERVE_DEADLINE = "seckill:reserve:deadline";
    public static final String SECKILL_UPLOAD_LOCK = "seckill:upload:lock";

    public static final int SECKILL_RESERVE_TIMEOUT_SECONDS = 30 * 60;
    public static final int SECKILL_OK_TTL_SECONDS = 40 * 60;
}
