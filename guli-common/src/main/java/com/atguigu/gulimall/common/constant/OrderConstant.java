package com.atguigu.gulimall.common.constant;

public class OrderConstant {
    public static final String USER_ORDER_TOKEN_PREFIX = "order:token:";

    public static final String USER_ORDER_SN_PREFIX = "order:sn:";

    //下单时保存的库存锁定信息，支付宝超时收单回调时用于发送关单消息
    public static final String ORDER_LOCK_PREFIX = "order:lock:";

    //订单状态【0->待付款；1->待发货；2->已发货；3->已完成；4->已关闭；5->无效订单】
    public static final int ORDER_STATUS_PENDING_PAY = 0;

    public static final int ORDER_STATUS_TO_DELIVER = 1;

    public static final int ORDER_STATUS_CLOSED = 4;
}
