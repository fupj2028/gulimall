package com.atguigu.gulimall.product.web;

import com.atguigu.gulimall.product.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HelloController {

    private final CategoryService categoryService;

    @GetMapping({"/","/index"})
    public String index(Model model) {
        model.addAttribute("level1Categories", categoryService.listLevel1());
        return "index";
    }
    
}