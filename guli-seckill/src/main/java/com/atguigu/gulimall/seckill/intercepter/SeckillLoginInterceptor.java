package com.atguigu.gulimall.seckill.intercepter;

import com.atguigu.gulimall.common.vo.MemberLoginVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SeckillLoginInterceptor implements HandlerInterceptor {

    public static final ThreadLocal<MemberLoginVo> LOGIN_USER = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        HttpSession session = request.getSession(false);
        MemberLoginVo loginUser = session == null ? null : (MemberLoginVo) session.getAttribute("loginUser");
        if (loginUser == null) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"请先登录\"}");
            return false;
        }
        LOGIN_USER.set(loginUser);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        LOGIN_USER.remove();
    }
}