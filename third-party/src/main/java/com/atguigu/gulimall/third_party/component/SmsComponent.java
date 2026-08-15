package com.atguigu.gulimall.third_party.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SmsComponent {

    private static final Logger log = LoggerFactory.getLogger(SmsComponent.class);

    public void sendCode(String phone, String code) {
        log.info("[SMS] 验证码 {} 已发送到手机 {}", code, phone);
    }
}
