package com.atguigu.gulimall.common.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class MemberOrderVo implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String orderSn;
    private BigDecimal payAmount;
    private Integer status;
    private String receiverName;
    private Date createTime;
    private List<MemberOrderItemVo> items;
}
