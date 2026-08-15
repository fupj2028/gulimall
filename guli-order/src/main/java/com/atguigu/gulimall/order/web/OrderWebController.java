package com.atguigu.gulimall.order.web;

import com.atguigu.gulimall.common.Exception.NoStockException;
import com.atguigu.gulimall.common.Exception.OrderExpireException;
import com.atguigu.gulimall.common.Exception.PriceMismatchException;
import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.vo.ConfirmItemVo;
import com.atguigu.gulimall.order.vo.FareVo;
import com.atguigu.gulimall.order.vo.OrderSubmitVo;
import com.atguigu.gulimall.order.vo.PayAsyncVo;
import com.atguigu.gulimall.order.vo.PayVo;
import com.atguigu.gulimall.order.vo.SubmitOrderResponseVo;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Controller
public class OrderWebController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/order/toTrade")
    public String toTrade(Model model) {
        ConfirmItemVo confirmVo = orderService.confirmOrder();
        model.addAttribute("confirmVo", confirmVo);
        if (confirmVo.getFareVo() != null && confirmVo.getFareVo().getAddress() != null) {
            model.addAttribute("memberId", confirmVo.getFareVo().getAddress().getMemberId());
        }
        return "confirm";
    }

    @GetMapping("/order/seckill/toTrade")
    public String seckillToTrade(@RequestParam Long sessionId, @RequestParam Long skuId,
                                 @RequestParam(required = false, defaultValue = "1") Integer num, Model model) {
        ConfirmItemVo confirmVo = orderService.confirmSeckillOrder(sessionId, skuId, num);
        model.addAttribute("confirmVo", confirmVo);
        if (confirmVo.getFareVo() != null && confirmVo.getFareVo().getAddress() != null) {
            model.addAttribute("memberId", confirmVo.getFareVo().getAddress().getMemberId());
        }
        //秒杀参数渲染进表单隐藏字段，提交时带回
        model.addAttribute("seckillSessionId", sessionId);
        model.addAttribute("seckillSkuId", skuId);
        model.addAttribute("seckillNum", num);
        return "confirm";
    }

    @ResponseBody
    @PostMapping("/order/fare")
    public FareVo getFare(@RequestParam Long addressId, @RequestParam Long memberId) {
        return orderService.getFare(addressId, memberId);
    }

    @PostMapping("/order/submit")
    public String submitOrder(OrderSubmitVo submitVo, Model model, RedirectAttributes redirectAttributes) {
        try {
            SubmitOrderResponseVo result = orderService.submitOrder(submitVo);
            if (result.getCode() != null && result.getCode() == 0) {
                model.addAttribute("result", result);
                return "pay";
            }
            redirectAttributes.addFlashAttribute("msg", failMessage(result.getCode()));
            return "redirect:/order/toTrade";
        } catch (OrderExpireException e) {
            log.warn("订单已过期: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("msg", failMessage(4));
            return "redirect:/order/toTrade";
        } catch (PriceMismatchException e) {
            log.warn("商品价格可能有更新: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("msg", failMessage(2));
            return "redirect:/order/toTrade";
        } catch (NoStockException e) {
            log.warn("库存不足: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("msg", failMessage(3));
            return "redirect:/order/toTrade";
        } catch (Exception e) {
            log.error("下单失败", e);
            redirectAttributes.addFlashAttribute("msg", failMessage(null));
            return "redirect:/order/toTrade";
        }
    }

    /**
     * 点击支付宝支付：拉起支付，跳转到模拟支付宝官方页
     */
    @GetMapping("/order/pay")
    public String toAlipay(@RequestParam String orderSn) {
        PayVo payVo = orderService.getPayVo(orderSn);
        if (payVo == null) {
            return "redirect:/order/toTrade";
        }
        return "redirect:/order/pay/alipay?outTradeNo=" + payVo.getOutTradeNo()
                + "&totalAmount=" + payVo.getTotalAmount()
                + "&subject=" + URLEncoder.encode(payVo.getSubject(), StandardCharsets.UTF_8);
    }

    /**
     * 同步回跳：支付完成后跳转到会员订单页
     */
    @GetMapping("/order/pay/return")
    public String payReturn(@RequestParam String orderSn) {
        log.info("支付同步回跳，orderSn={}", orderSn);
        return "redirect:/api/member/order/list";
    }

    /**
     * 支付宝异步通知端点：TRADE_SUCCESS 支付成功改订单；TRADE_CLOSED 超时收单发关单消息
     */
    @ResponseBody
    @PostMapping("/pay/alipay/notify")
    public String alipayNotify(PayAsyncVo vo) {
        log.info("收到支付宝异步通知: {}", vo);
        if ("TRADE_SUCCESS".equals(vo.getTradeStatus())) {
            orderService.handlePayNotify(vo);
        } else if ("TRADE_CLOSED".equals(vo.getTradeStatus())) {
            orderService.closeOrderByAlipay(vo.getOutTradeNo());
        }
        return "success";
    }

    private String failMessage(Integer code) {
        if (code == null) {
            return "下单失败，请重试";
        }
        switch (code) {
            case 1:
                return "订单已提交或防重令牌失效，请重新确认订单";
            case 2:
                return "商品价格可能有更新，请确认后重新提交";
            case 3:
                return "商品库存不足，无法下单";
            case 4:
                return "订单已过期，请重新提交";
            default:
                return "下单失败，请重试";
        }
    }
}