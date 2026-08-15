package com.atguigu.gulimall.auth.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.atguigu.gulimall.auth.feign.MemberFeignService;
import com.atguigu.gulimall.auth.feign.SmsFeignService;
import com.atguigu.gulimall.auth.vo.LoginVo;
import com.atguigu.gulimall.auth.vo.RegisterVo;
import com.atguigu.gulimall.common.constant.AuthConstant;
import com.atguigu.gulimall.common.utils.R;
import com.atguigu.gulimall.common.vo.MemberLoginVo;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class RegisterController {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private SmsFeignService smsFeignService;

    @Autowired
    private MemberFeignService memberFeignService;

    @ResponseBody
    @PostMapping("/sms/send")
    public R sendSms(@RequestParam("phone") String phone) {
        String limitKey = AuthConstant.SMS_CODE_LIMIT_PREFIX + phone;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(limitKey))) {
            long ttl = redisTemplate.getExpire(limitKey, TimeUnit.SECONDS);
            return R.error(429, "操作太频繁，请" + ttl + "秒后再试");
        }

        String code = String.format("%06d", new Random().nextInt(999999));
        redisTemplate.opsForValue().set(AuthConstant.SMS_CODE_CACHE_PREFIX + phone, code, 5, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(limitKey, "1", 60, TimeUnit.SECONDS);

        smsFeignService.sendSms(phone, code);

        return R.ok().put("data", code);
    }

    @ResponseBody
    @PostMapping("/register/save")
    public R register(@Valid @RequestBody RegisterVo vo, BindingResult result) {
        if (result.hasErrors()) {
            String msg = result.getFieldErrors().stream()
                    .map(e -> e.getDefaultMessage())
                    .collect(java.util.stream.Collectors.joining(", "));
            return R.error(400, msg);
        }

        String savedCode = redisTemplate.opsForValue().get(AuthConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
        if (savedCode == null) {
            return R.error("验证码已过期，请重新获取");
        }
        if (!savedCode.equals(vo.getCode())) {
            return R.error("验证码错误");
        }

        redisTemplate.delete(AuthConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());

        Map<String, Object> params = new HashMap<>();
        params.put("username", vo.getUsername());
        params.put("password", vo.getPassword());
        params.put("mobile", vo.getPhone());
        params.put("nickname", vo.getUsername());

        R r = memberFeignService.register(params);
        if (r.get("code") instanceof Integer && (Integer) r.get("code") != 0) {
            return R.error((String) r.get("msg"));
        }
        return R.ok();
    }

    @ResponseBody
    @PostMapping("/login/authenticate")
    public R login(@Valid @RequestBody LoginVo vo, BindingResult result, HttpSession session) {
        if (result.hasErrors()) {
            String msg = result.getFieldErrors().stream()
                    .map(e -> e.getDefaultMessage())
                    .collect(java.util.stream.Collectors.joining(", "));
            return R.error(400, msg);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("username", vo.getUsername());
        params.put("password", vo.getPassword());
        R r = memberFeignService.login(params);
        if (r.get("code") instanceof Integer && (Integer) r.get("code") != 0) {
            return R.error("用户名或密码错误");
        }

        Map<String, Object> member = (Map<String, Object>) r.get("member");

        MemberLoginVo loginVo = new MemberLoginVo();
        loginVo.setMemberId(member.get("id") != null ? Long.valueOf(member.get("id").toString()) : null);
        loginVo.setLevelId(member.get("levelId") != null ? Long.valueOf(member.get("levelId").toString()) : null);
        loginVo.setUsername((String) member.get("username"));
        loginVo.setNickname((String) member.get("nickname"));
        loginVo.setMobile((String) member.get("mobile"));
        loginVo.setEmail((String) member.get("email"));
        loginVo.setHeader((String) member.get("header"));
        loginVo.setGender(member.get("gender") != null ? Integer.valueOf(member.get("gender").toString()) : null);
        loginVo.setIntegration(member.get("integration") != null ? Integer.valueOf(member.get("integration").toString()) : null);
        loginVo.setGrowth(member.get("growth") != null ? Integer.valueOf(member.get("growth").toString()) : null);

        session.setAttribute("loginUser", loginVo);
        return R.ok();
    }
}
