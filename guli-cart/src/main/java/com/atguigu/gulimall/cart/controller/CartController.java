package com.atguigu.gulimall.cart.controller;

import com.atguigu.gulimall.cart.service.CartService;
import com.atguigu.gulimall.cart.vo.CartItemVo;
import com.atguigu.gulimall.cart.vo.CartVo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired
    CartService cartService;

    @GetMapping("/list")
    public String cartList(Model model) {
        CartVo cart = cartService.getCart();
        model.addAttribute("cartList", cart.getItems());
        return "cartList";
    }

    @GetMapping("/addToCart")
    public String addToCart(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num) {
        cartService.addToCart(skuId, num);
        return "redirect:/cart/addToCartSuccess?skuId=" + skuId + "&num=" + num;
    }

    @GetMapping("/addToCartSuccess")
    public String addToCartSuccess(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num, Model model) {
        CartItemVo cartItem = cartService.getCartItem(skuId);
        model.addAttribute("cartItem", cartItem);
        model.addAttribute("skuNum", num);
        return "success";
    }

    @ResponseBody
    @PostMapping("/checkCart")
    public String checkCart(@RequestParam("skuId") Long skuId, @RequestParam("isChecked") Integer isChecked) {
        cartService.checkItem(skuId, isChecked);
        return "ok";
    }

    @ResponseBody
    @PostMapping("/changeCount")
    public String changeCount(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num) {
        cartService.changeItemCount(skuId, num);
        return "ok";
    }

    @ResponseBody
    @PostMapping("/deleteItem")
    public String deleteItem(@RequestParam("skuId") Long skuId) {
        cartService.deleteItem(skuId);
        return "ok";
    }

    @ResponseBody
    @GetMapping("/checkedItems")
    public List<CartItemVo> checkedItems() {
        CartVo cart = cartService.getCart();
        return cart.getItems().stream()
                .filter(CartItemVo::getCheck)
                .collect(Collectors.toList());
    }
}
