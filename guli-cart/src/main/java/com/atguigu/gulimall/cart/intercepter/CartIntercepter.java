package com.atguigu.gulimall.cart.intercepter;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.atguigu.gulimall.cart.vo.UserInfoTo;
import com.atguigu.gulimall.common.constant.CartConstant;
import com.atguigu.gulimall.common.vo.MemberLoginVo;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class CartIntercepter implements HandlerInterceptor {

    public static ThreadLocal<UserInfoTo> threadLocal = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        HttpSession session = request.getSession();

        UserInfoTo userInfo = new UserInfoTo();
        MemberLoginVo info = (MemberLoginVo) session.getAttribute("loginUser");
        if (info != null) {
            userInfo.setUserId(info.getMemberId());
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                String name = cookie.getName();
                if (name.equals(CartConstant.TEMP_USER_KEY)) {
                    userInfo.setTempUserKey(cookie.getValue());
                    userInfo.setTempUser(true);
                    break;
                }
            }
        }

        if (!StringUtils.hasText(userInfo.getTempUserKey())) {
            String userKey = UUID.randomUUID().toString();
            userInfo.setTempUserKey(userKey);
            userInfo.setTempUser(false);
        }

        threadLocal.set(userInfo);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable ModelAndView modelAndView) throws Exception {

        UserInfoTo userInfo = threadLocal.get();
        if (!userInfo.getTempUser()) {
            Cookie cookie = new Cookie(CartConstant.TEMP_USER_KEY, userInfo.getTempUserKey());
            cookie.setMaxAge(CartConstant.TIME_TO_EXPIRE);
            response.addCookie(cookie);
        }

        // return true;
    }

}
