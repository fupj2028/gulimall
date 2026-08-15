package com.atguigu.gulimall.search.controller;

import com.atguigu.gulimall.search.service.SearchService;
import com.atguigu.gulimall.search.vo.SearchParam;
import com.atguigu.gulimall.search.vo.SearchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class IndexController {

    private final SearchService searchService;

    @GetMapping({"/search", "/list"})
    public String search(SearchParam param, Model model) {
        SearchResult result = searchService.search(param);
        model.addAttribute("result", result);
        return "list";
    }

    @GetMapping({"/search/debug"})
    @org.springframework.web.bind.annotation.ResponseBody
    public SearchResult debug(SearchParam param) {
        return searchService.search(param);
    }
}
