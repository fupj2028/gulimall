package com.atguigu.gulimall.common.to;

import java.util.List;

import lombok.Data;

@Data
public class WareSkuLockVo {
    private String orderSn;

    private List<WareSkuLockItem> locks;
}
