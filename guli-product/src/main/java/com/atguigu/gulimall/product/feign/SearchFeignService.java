package com.atguigu.gulimall.product.feign;

import com.atguigu.gulimall.common.to.es.SkuEsModel;
import com.atguigu.gulimall.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("guli-search")
public interface SearchFeignService {

    @PostMapping("/search/save/product")
    R saveProduct(@RequestBody List<SkuEsModel> skuEsModels);
}
