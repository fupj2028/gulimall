package com.atguigu.gulimall.auth.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import com.atguigu.gulimall.auth.feign.MemberFeignService;
import com.atguigu.gulimall.common.utils.R;
import com.atguigu.gulimall.common.vo.MemberLoginVo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Controller
public class OAuthController {

    @Value("${github.client.client-id}")
    private String clientId;

    @Value("${github.client.client-secret}")
    private String clientSecret;

    @Value("${github.redirect.uri}")
    private String redirectUri;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final MemberFeignService memberFeignService;

    @GetMapping("/login/oauth2/github")
    public String githubLogin() {
        return "redirect:https://github.com/login/oauth/authorize?client_id=" + clientId
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
    }

    @GetMapping("/login/oauth/authorize")
    public String callback(@RequestParam("code") String code, HttpSession session) throws Exception {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("code", code);
        body.add("redirect_uri", redirectUri);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Accept", "application/json");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> tokenResp = restTemplate.postForEntity(
                "https://github.com/login/oauth/access_token", request, String.class);
        JsonNode tokenNode = objectMapper.readTree(tokenResp.getBody());
        if (tokenNode.has("error")) {
            return "redirect:http://124.222.125.141/login?error=github_" + tokenNode.get("error").asText();
        }
        String accessToken = tokenNode.get("access_token").asText();

        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(accessToken);
        HttpEntity<Void> userRequest = new HttpEntity<>(userHeaders);
        ResponseEntity<String> userResp = restTemplate.exchange(
                "https://api.github.com/user", org.springframework.http.HttpMethod.GET,
                userRequest, String.class);
        JsonNode userNode = objectMapper.readTree(userResp.getBody());
        long githubId = userNode.get("id").asLong();
        String name = userNode.has("name") && !userNode.get("name").isNull()
                ? userNode.get("name").asText() : userNode.get("login").asText();

        String username = "github_" + githubId;
        R findResult = memberFeignService.findByUsername(username);
        Map<String, Object> memberInfo;
        if (findResult.get("code") instanceof Integer && (Integer) findResult.get("code") == 0) {
            memberInfo = (Map<String, Object>) findResult.get("member");
        } else {
            Map<String, Object> params = new HashMap<>();
            params.put("username", username);
            params.put("password", UUID.randomUUID().toString().replace("-", ""));
            params.put("mobile", "");
            params.put("nickname", name);

            R r = memberFeignService.register(params);
            if (r.get("code") instanceof Integer && (Integer) r.get("code") != 0) {
                return "redirect:http://124.222.125.141/login?error=" + r.get("msg");
            }
            memberInfo = (Map<String, Object>) r.get("member");
        }

        MemberLoginVo loginVo = buildMemberLoginVo(memberInfo);
        session.setAttribute("loginUser", loginVo);

        return "redirect:http://124.222.125.141/";
    }

    private MemberLoginVo buildMemberLoginVo(Map<String, Object> map) {
        MemberLoginVo vo = new MemberLoginVo();
        vo.setMemberId(map.get("id") != null ? Long.valueOf(map.get("id").toString()) : null);
        vo.setLevelId(map.get("levelId") != null ? Long.valueOf(map.get("levelId").toString()) : null);
        vo.setUsername((String) map.get("username"));
        vo.setNickname((String) map.get("nickname"));
        vo.setMobile((String) map.get("mobile"));
        vo.setEmail((String) map.get("email"));
        vo.setHeader((String) map.get("header"));
        vo.setGender(map.get("gender") != null ? Integer.valueOf(map.get("gender").toString()) : null);
        vo.setIntegration(map.get("integration") != null ? Integer.valueOf(map.get("integration").toString()) : null);
        vo.setGrowth(map.get("growth") != null ? Integer.valueOf(map.get("growth").toString()) : null);
        return vo;
    }

}
