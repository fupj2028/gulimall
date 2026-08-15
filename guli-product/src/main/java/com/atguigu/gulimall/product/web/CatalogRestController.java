package com.atguigu.gulimall.product.web;

import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.Catalog2Vo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class CatalogRestController {

    private final CategoryService categoryService;

    @GetMapping("/index/catalog.json")
    public Map<String, List<Catalog2Vo>> catalog() {
        return categoryService.getCatalogJson();
    }
}
