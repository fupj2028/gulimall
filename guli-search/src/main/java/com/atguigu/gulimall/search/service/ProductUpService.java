package com.atguigu.gulimall.search.service;

import com.atguigu.gulimall.common.to.es.SkuEsModel;
import com.atguigu.gulimall.common.utils.R;

import java.util.List;

public interface ProductUpService {

    R productUp(List<SkuEsModel> skuEsModels);
}
