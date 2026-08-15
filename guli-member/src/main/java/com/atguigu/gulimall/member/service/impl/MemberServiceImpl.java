package com.atguigu.gulimall.member.service.impl;

import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gulimall.common.utils.PageUtils;
import com.atguigu.gulimall.common.utils.Query;
import com.atguigu.gulimall.common.utils.R;
import com.atguigu.gulimall.member.dao.MemberDao;
import com.atguigu.gulimall.member.dao.MemberLevelDao;
import com.atguigu.gulimall.member.entity.MemberEntity;
import com.atguigu.gulimall.member.entity.MemberLevelEntity;
import com.atguigu.gulimall.member.service.MemberService;

@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    private MemberLevelDao memberLevelDao;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );
        return new PageUtils(page);
    }

    @Override
    public MemberEntity login(String username, String password) {
        MemberEntity member = this.getOne(new QueryWrapper<MemberEntity>()
                .and(w -> w.eq("username", username).or().eq("mobile", username)));
        if (member != null && encoder.matches(password, member.getPassword())) {
            return member;
        }
        return null;
    }

    @Override
    public R register(MemberEntity member) {
        long usernameCount = this.count(new QueryWrapper<MemberEntity>()
                .eq("username", member.getUsername()));
        if (usernameCount > 0) {
            return R.error("用户名已存在");
        }

        long mobileCount = this.count(new QueryWrapper<MemberEntity>()
                .eq("mobile", member.getMobile()));
        if (mobileCount > 0) {
            return R.error("手机号已被注册");
        }

        MemberLevelEntity level = memberLevelDao.selectOne(
                new QueryWrapper<MemberLevelEntity>().eq("default_status", 1));
        if (level != null) {
            member.setLevelId(level.getId());
        }

        member.setPassword(encoder.encode(member.getPassword()));
        member.setCreateTime(new Date());
        member.setStatus(1);
        member.setIntegration(0);
        member.setGrowth(0);

        this.save(member);
        return R.ok();
    }

    @Override
    public MemberEntity findByUsername(String username) {
        return this.getOne(new QueryWrapper<MemberEntity>().eq("username", username));
    }

}
