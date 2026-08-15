package com.atguigu.gulimall.order.web;

import com.atguigu.gulimall.order.vo.PayAsyncVo;
import com.atguigu.gulimall.order.vo.PayVo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 模拟支付宝官方收银台。
 * 只负责支付宝侧的收银台展示与向商家发送异步通知，不感知项目订单服务。
 */
@Slf4j
@Controller
public class MockAlipayController {

    @Value("${gulimall.alipay.pay-timeout-seconds:1800}")
    private int payTimeoutSeconds;

    @Value("${gulimall.alipay.notify-url:http://localhost:14000/pay/alipay/notify}")
    private String notifyUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 模拟支付宝官方收银台页面
     */
    @GetMapping("/order/pay/alipay")
    public String alipayPage(@RequestParam String outTradeNo,
                             @RequestParam String totalAmount,
                             @RequestParam(required = false) String subject,
                             Model model) {
        PayVo payVo = new PayVo();
        payVo.setOutTradeNo(outTradeNo);
        payVo.setTotalAmount(totalAmount);
        payVo.setSubject(subject);
        //模拟沙箱环境登录的买家账号
        model.addAttribute("buyer", "sandbox_buyer_xxxx@alipay.com");
        model.addAttribute("closed", false);
        model.addAttribute("payVo", payVo);
        model.addAttribute("expireSeconds", payTimeoutSeconds);
        return "alipay";
    }

    /**
     * 模拟支付宝支付成功：向商家发送 TRADE_SUCCESS 异步通知，然后回跳商家
     */
    @PostMapping("/order/pay/alipay/confirm")
    public String alipayConfirm(@RequestParam String outTradeNo,
                                @RequestParam String totalAmount) {
        log.info("模拟支付宝支付成功，outTradeNo={}, totalAmount={}", outTradeNo, totalAmount);
        sendNotify(outTradeNo, totalAmount, "TRADE_SUCCESS");
        //同步回跳商家成功页
        return "redirect:/order/pay/return?orderSn=" + outTradeNo;
    }

    /**
     * 模拟支付宝自动超时收单：只向商家发送 TRADE_CLOSED 异步通知，然后展示交易关闭页面
     */
    @PostMapping("/order/pay/alipay/close")
    public String alipayClose(@RequestParam String outTradeNo, Model model) {
        log.info("模拟支付宝超时收单，交易已关闭，outTradeNo={}", outTradeNo);
        sendNotify(outTradeNo, null, "TRADE_CLOSED");
        model.addAttribute("closed", true);
        model.addAttribute("msg", "超时未支付，交易已关闭");
        return "alipay";
    }

    /**
     * 模拟支付宝向商家异步通知端点发送表单通知
     */
    private void sendNotify(String outTradeNo, String totalAmount, String tradeStatus) {
        PayAsyncVo vo = new PayAsyncVo();
        vo.setOutTradeNo(outTradeNo);
        vo.setTotalAmount(totalAmount);
        vo.setTradeStatus(tradeStatus);
        vo.setTradeNo("MOCK" + System.currentTimeMillis());
        vo.setAppId("MOCK_ALIPAY_APPID");
        vo.setNotifyType("trade_status_sync");
        vo.setGmtPayment(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("outTradeNo", vo.getOutTradeNo());
        form.add("totalAmount", vo.getTotalAmount());
        form.add("tradeStatus", vo.getTradeStatus());
        form.add("tradeNo", vo.getTradeNo());
        form.add("appId", vo.getAppId());
        form.add("notifyType", vo.getNotifyType());
        form.add("gmtPayment", vo.getGmtPayment());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
        try {
            String resp = restTemplate.postForObject(notifyUrl, request, String.class);
            log.info("模拟支付宝异步通知已发送，tradeStatus={}, 商家返回={}", tradeStatus, resp);
        } catch (Exception e) {
            log.error("模拟支付宝异步通知发送失败，outTradeNo={}, tradeStatus={}", outTradeNo, tradeStatus, e);
        }
    }
}
