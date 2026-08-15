package com.atguigu.gulimall.cart.service.impl;

// import static org.junit.jupiter.api.DynamicTest.stream;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.atguigu.gulimall.cart.feign.ProductFeignService;
import com.atguigu.gulimall.cart.intercepter.CartIntercepter;
import com.atguigu.gulimall.cart.service.CartService;
import com.atguigu.gulimall.cart.vo.CartItemVo;
import com.atguigu.gulimall.cart.vo.CartVo;
import com.atguigu.gulimall.cart.vo.SkuInfoVo;
import com.atguigu.gulimall.cart.vo.UserInfoTo;
import com.atguigu.gulimall.common.utils.R;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    @Qualifier("cartExecutor")
    ThreadPoolTaskExecutor executor;

    static final String CART_PREFIX = "gulimall:cart";

    @Override
    public CartItemVo addToCart(Long skuId, Integer num) {

        BoundHashOperations<String, Object, Object> operations = getCartTask();

        CartItemVo cartItem;
        Object o = operations.get(skuId.toString());

        if (o != null) {
            cartItem = objectMapper.convertValue(o, CartItemVo.class);
            cartItem.setCount(cartItem.getCount() + num);
        } else {
            CompletableFuture<SkuInfoVo> skuFuture = CompletableFuture.supplyAsync(
                    () -> {
                        R r = productFeignService.info(skuId);
                        return objectMapper.convertValue(r.get("skuInfo"), SkuInfoVo.class);
                    }, executor);

            CompletableFuture<List<String>> attrsFuture = CompletableFuture.supplyAsync(
                    () -> productFeignService.stringList(skuId), executor);

            CompletableFuture.allOf(skuFuture, attrsFuture).join();

            SkuInfoVo skuInfo = skuFuture.join();
            List<String> attrs = attrsFuture.join();

            cartItem = new CartItemVo();
            cartItem.setSkuId(skuId);
            cartItem.setTitle(skuInfo.getSkuTitle());
            cartItem.setImage(skuInfo.getSkuDefaultImg());
            cartItem.setPrice(skuInfo.getPrice());
            cartItem.setCount(num);
            cartItem.setAttrs(attrs);
            cartItem.setCheck(true);
        }

        operations.put(skuId.toString(), cartItem);

        return cartItem;
    }

    @Override
    public CartItemVo getCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> operations = getCartTask();
        Object o = operations.get(skuId.toString());
        if (o == null) {
            return null;
        }
        return objectMapper.convertValue(o, CartItemVo.class);
    }

    private BoundHashOperations<String, Object, Object> getCartTask() {
        UserInfoTo userInfo = CartIntercepter.threadLocal.get();
        String key;
        if (userInfo.getUserId() != null) {
            key = CART_PREFIX + userInfo.getUserId();
        } else {
            key = CART_PREFIX + userInfo.getTempUserKey();
        }
        return redisTemplate.boundHashOps(key);
    }

    @Override
    public CartVo getCart() {
        UserInfoTo userInfo = CartIntercepter.threadLocal.get();
        CartVo cart = new CartVo();

        if (userInfo.getUserId() != null) {
            String userKey = CART_PREFIX + userInfo.getUserId();
            String tempKey = CART_PREFIX + userInfo.getTempUserKey();

            List<CartItemVo> tempCartItems = getCartItemList(tempKey);
            if (!tempCartItems.isEmpty()) {
                BoundHashOperations<String, Object, Object> userOps = redisTemplate.boundHashOps(userKey);
                for (CartItemVo item : tempCartItems) {
                    Object o = userOps.get(item.getSkuId().toString());
                    if (o != null) {
                        CartItemVo existing = objectMapper.convertValue(o, CartItemVo.class);
                        existing.setCount(existing.getCount() + item.getCount());
                        userOps.put(item.getSkuId().toString(), existing);
                    } else {
                        userOps.put(item.getSkuId().toString(), item);
                    }
                }
                clearTempCart(tempKey);
            }

            cart.setItems(getCartItemList(userKey));
            return cart;

        } else {
            String key = CART_PREFIX + userInfo.getTempUserKey();
            cart.setItems(getCartItemList(key));
            return cart;
        }
    }

    private List<CartItemVo> getCartItemList(String cartKey) {
        BoundHashOperations<String, Object, Object> operation = redisTemplate.boundHashOps(cartKey);
        List<Object> list = operation.values();
        if (list != null && list.size() > 0) {
            return list.stream().map(l -> objectMapper.convertValue(l, CartItemVo.class)).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @Override
    public void checkItem(Long skuId, Integer isChecked) {
        BoundHashOperations<String, Object, Object> operations = getCartTask();
        Object o = operations.get(skuId.toString());
        if (o != null) {
            CartItemVo cartItem = objectMapper.convertValue(o, CartItemVo.class);
            cartItem.setCheck(isChecked == 1);
            operations.put(skuId.toString(), cartItem);
        }
    }

    @Override
    public void changeItemCount(Long skuId, Integer num) {
        BoundHashOperations<String, Object, Object> operations = getCartTask();
        Object o = operations.get(skuId.toString());
        if (o != null) {
            CartItemVo cartItem = objectMapper.convertValue(o, CartItemVo.class);
            cartItem.setCount(num);
            operations.put(skuId.toString(), cartItem);
        }
    }

    @Override
    public void deleteItem(Long skuId) {
        BoundHashOperations<String, Object, Object> operations = getCartTask();
        operations.delete(skuId.toString());
    }

    @Override
    public void clearTempCart(String key) {
        redisTemplate.delete(key);
    }

}
