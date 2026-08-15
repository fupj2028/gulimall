package com.atguigu.gulimall.auth.vo;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class LoginVo {

    @NotEmpty(message = "用户名不能为空")
    private String username;

    @NotEmpty(message = "密码不能为空")
    private String password;
}
