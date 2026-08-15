package com.atguigu.gulimall.common.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class MemberOrderItemVo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String skuName;
    private String skuPic;
    private Integer skuQuantity;
}
