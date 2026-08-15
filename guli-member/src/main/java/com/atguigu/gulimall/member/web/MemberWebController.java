package com.atguigu.gulimall.member.web;

import com.atguigu.gulimall.common.vo.MemberLoginVo;
import com.atguigu.gulimall.common.vo.MemberOrderVo;
import com.atguigu.gulimall.member.feign.OrderFeignService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;

import java.util.List;


@Controller
public class MemberWebController {

    @Autowired
    private OrderFeignService orderFeignService;

    @GetMapping("/member/order/list")
    public String memberOrderList(Model model, HttpSession session) {
        MemberLoginVo loginUser = (MemberLoginVo) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }
        List<MemberOrderVo> orderList = orderFeignService.listByMember(loginUser.getMemberId());
        model.addAttribute("orders", orderList);
        return "list";
    }
}
