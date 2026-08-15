package com.atguigu.gulimall.common.to;

import java.util.List;

import lombok.Data;

@Data
public class StockLockedTo {

    private String orderSn;

    private Long taskId;

    private List<StockLockedDetailTo> details;
}
