package com.atguigu.gulimall.coupon.exception;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.atguigu.gulimall.common.utils.R;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice(basePackages = "com.atguigu.gulimall.coupon.controller")
public class CouponExceptionControllerAdvice {

    @ExceptionHandler(DuplicateKeyException.class)
    public R handleDuplicateKeyException(DuplicateKeyException e) {
        log.error("数据重复:", e);
        return R.error("该场次已存在相同的秒杀商品，请勿重复添加");
    }
}
