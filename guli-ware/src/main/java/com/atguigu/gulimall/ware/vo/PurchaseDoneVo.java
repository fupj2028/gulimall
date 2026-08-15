package com.atguigu.gulimall.ware.vo;

import java.util.List;

import lombok.Data;

@Data
public class PurchaseDoneVo {
    private Long id;
    private List<PurchaseDetailDoneVo> items;

}
