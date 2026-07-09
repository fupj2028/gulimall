package com.atguigu.gulimall.product.exception;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.atguigu.gulimall.common.Exception.ErrorCode;
import com.atguigu.gulimall.common.utils.R;

@RestControllerAdvice(basePackages = "com.atguigu.gulimall.product.controller")
public class GulimallExceptionControllerAdvice {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R handleValidException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        // String message = bindingResult.getFieldErrors().stream()
        //         .map(fieldError -> fieldError.getDefaultMessage())
        //         .collect(Collectors.joining("; "));
        Map<String, String> errorMap = bindingResult.getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage()
                ));
        return R.error(ErrorCode.InvalidParameter.getCode(),ErrorCode.InvalidParameter.getMessage())
                .put("msg", errorMap);
    }

    @ExceptionHandler(Throwable.class)
    public R handleException(Throwable e) {
        return R.error(ErrorCode.UnknownError.getCode(),ErrorCode.UnknownError.getMessage());
    }
}
