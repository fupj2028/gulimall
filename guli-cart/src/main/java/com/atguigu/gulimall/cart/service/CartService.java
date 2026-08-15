package com.atguigu.gulimall.cart.service;

import com.atguigu.gulimall.cart.vo.CartItemVo;
import com.atguigu.gulimall.cart.vo.CartVo;

public interface CartService {

    CartItemVo addToCart(Long skuId, Integer num);

    CartItemVo getCartItem(Long skuId);

    CartVo getCart();

    void checkItem(Long skuId, Integer isChecked);

    void changeItemCount(Long skuId, Integer num);

    void deleteItem(Long skuId);

    void clearTempCart(String key);

}
