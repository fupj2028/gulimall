package com.atguigu.gulimall.third_party.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.atguigu.gulimall.common.utils.R;
import com.atguigu.gulimall.third_party.component.SmsComponent;

@RestController
public class SmsController {

    @Autowired
    private SmsComponent smsComponent;

    @PostMapping("/sms/send")
    public R sendSms(@RequestParam("phone") String phone, @RequestParam("code") String code) {
        smsComponent.sendCode(phone, code);
        return R.ok();
    }
}
