package com.atguigu.gulimall.common.to;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class SeckillSessionWithSkusTo {
    private Long id;
    private String name;
    private Date startTime;
    private Date endTime;
    private Integer status;
    private List<SeckillSkuRelationTo> relations;
}
