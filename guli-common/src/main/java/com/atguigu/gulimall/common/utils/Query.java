package com.atguigu.gulimall.common.utils;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.LinkedHashMap;
import java.util.Map;

public class Query<T> {

    public IPage<T> getPage(Map<String, Object> params) {
        long currPage = 1;
        long limit = 10;

        if (params.get("page") != null) {
            currPage = Long.parseLong(params.get("page").toString());
        }
        if (params.get("limit") != null) {
            limit = Long.parseLong(params.get("limit").toString());
        }

        return new Page<>(currPage, limit);
    }
}
