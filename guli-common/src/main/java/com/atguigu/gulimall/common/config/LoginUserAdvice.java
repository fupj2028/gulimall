package com.atguigu.gulimall.common.config;

import com.atguigu.gulimall.common.vo.MemberLoginVo;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class LoginUserAdvice {

    @ModelAttribute("loginUser")
    public MemberLoginVo loginUser(HttpSession session) {
        return (MemberLoginVo) session.getAttribute("loginUser");
    }
}
