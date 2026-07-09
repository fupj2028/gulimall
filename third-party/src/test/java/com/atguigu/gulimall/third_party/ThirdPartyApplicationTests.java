package com.atguigu.gulimall.third_party;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.http.HttpMethodName;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ThirdPartyApplicationTests {

    @Autowired
    private COSClient cosClient;

    @Value("${cos.bucket}")
    private String bucket;

    @Test
    void contextLoads() {
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String key = date + "/test.txt";

        Date expiration = new Date(System.currentTimeMillis() + 30 * 60 * 1000);
        URL url = cosClient.generatePresignedUrl(bucket, key, expiration, HttpMethodName.GET);

        System.out.println(url);
    }
}
