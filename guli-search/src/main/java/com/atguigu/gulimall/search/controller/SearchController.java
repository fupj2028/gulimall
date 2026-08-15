package com.atguigu.gulimall.search.controller;

import com.atguigu.gulimall.common.to.es.SkuEsModel;
import com.atguigu.gulimall.common.utils.R;
import com.atguigu.gulimall.search.service.ProductUpService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final ProductUpService productUpService;

    @PostMapping("/save/product")
    public R saveProduct(@RequestBody List<SkuEsModel> skuEsModels) {
        if (skuEsModels == null || skuEsModels.isEmpty()) {
            return R.error("没有需要上架的商品");
        }
        return productUpService.productUp(skuEsModels);
    }
}
