package com.atguigu.gulimall.third_party.controller;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import com.atguigu.gulimall.common.utils.R;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.http.HttpMethodName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cos")
public class CosController {

    @Autowired
    private COSClient cosClient;

    @Value("${cos.bucket}")
    private String bucket;

    @RequestMapping("/policy")
    public R policy(@RequestParam String fileName) {
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        String key = date + "/" + UUID.randomUUID().toString().replace("-", "") + suffix;

        Date expiration = new Date(System.currentTimeMillis() + 30 * 60 * 1000);
        URL url = cosClient.generatePresignedUrl(bucket, key, expiration, HttpMethodName.PUT);

        return R.ok().put("data", url.toString()).put("key", key);
    }

    @RequestMapping("/access")
    public R access(@RequestParam String key) {
        if (key.startsWith("http://") || key.startsWith("https://")) {
            try {
                key = new java.net.URL(key).getPath().substring(1);
            } catch (Exception e) {
                return R.error("无效的URL");
            }
        }
        Date expiration = new Date(System.currentTimeMillis() + 30 * 60 * 1000);
        URL url = cosClient.generatePresignedUrl(bucket, key, expiration, HttpMethodName.GET);
        return R.ok().put("data", url.toString());
    }

    @RequestMapping("/delete")
    public R delete(@RequestParam String key) {
        if (key.startsWith("http://") || key.startsWith("https://")) {
            try {
                key = new java.net.URL(key).getPath().substring(1);
            } catch (Exception e) {
                return R.error("无效的URL");
            }
        }
        Date expiration = new Date(System.currentTimeMillis() + 30 * 60 * 1000);
        URL url = cosClient.generatePresignedUrl(bucket, key, expiration, HttpMethodName.DELETE);
        return R.ok().put("data", url.toString());
    }
}
