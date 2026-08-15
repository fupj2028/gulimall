package com.atguigu.gulimall.product.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.atguigu.gulimall.product.service.SkuItemService;
import com.atguigu.gulimall.product.vo.SkuItemVo;


@Controller
public class ItemController {

    @Autowired
    private SkuItemService skuItemService;

    @GetMapping("/item/{id}.html")
    public String item(@PathVariable("id") Long skuId, Model model) {

        SkuItemVo item = skuItemService.buildSkuItem(skuId);

        model.addAttribute("skuInfo", item);
        model.addAttribute("spuSaleAttrListCheckBySku", item.getSaleAttrs());
        model.addAttribute("spuInfoDesc", item.getSpuDesc());
        model.addAttribute("spuInfoDecriptImages", item.getDecriptImages());
        model.addAttribute("valuesSku", item.getValuesSku());
        model.addAttribute("spuAttrGroups", item.getAttrGroups());

        return "item";
    }
    
}
