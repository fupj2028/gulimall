package com.atguigu.gulimall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class Catalog2Vo {
    private String name;
    private List<Catalog3Vo> catalog3List;
}
