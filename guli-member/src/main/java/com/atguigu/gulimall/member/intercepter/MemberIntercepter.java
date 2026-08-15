package com.atguigu.gulimall.member.intercepter;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.atguigu.gulimall.common.vo.MemberLoginVo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class MemberIntercepter implements HandlerInterceptor {

    public static ThreadLocal<MemberLoginVo> threadLocal = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        HttpSession session = request.getSession();

        // UserInfoTo userInfo = new UserInfoTo();
        MemberLoginVo info = (MemberLoginVo) session.getAttribute("loginUser");
        if (info != null) {
            threadLocal.set(info);
            return true;
        }else{
            request.getSession().setAttribute("msg","请先进行登录");
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.setHeader("Location", "/login");

            return false; 
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception ex) throws Exception {
        threadLocal.remove();
    }

}
