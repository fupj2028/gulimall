package com.atguigu.gulimall.common.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class MemberLoginVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long memberId;
    private Long levelId;
    private String username;
    private String nickname;
    private String mobile;
    private String email;
    private String header;
    private Integer gender;
    private Integer integration;
    private Integer growth;
    private Date createTime;
}
