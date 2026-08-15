package com.atguigu.gulimall.search.vo;

import lombok.Data;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Data
public class SearchParam {

    private String keyword;

    private Long catelogId;

    private List<Long> brandId;

    private String price;

    private Integer hasStock;

    private List<String> attrs;

    private String sort;

    private Integer pageNum = 1;

    public String buildUrlExcluding(String excludeKey, String excludeValue) {
        var parts = new ArrayList<String>();

        append(parts, "keyword", keyword, "keyword", excludeKey, excludeValue);
        append(parts, "catelogId", catelogId, "catelogId", excludeKey, excludeValue);
        if (brandId != null) {
            for (Long id : brandId) {
                String val = id.toString();
                if (!("brandId".equals(excludeKey) && val.equals(excludeValue)))
                    parts.add("brandId=" + val);
            }
        }
        if (attrs != null) {
            for (String a : attrs) {
                if (!("attrs".equals(excludeKey) && a.equals(excludeValue)))
                    parts.add("attrs=" + urlEncode(a));
            }
        }
        append(parts, "price", price, "price", excludeKey, excludeValue);
        append(parts, "sort", sort, "sort", excludeKey, excludeValue);
        if (hasStock != null && !"hasStock".equals(excludeKey))
            parts.add("hasStock=" + hasStock);


        if (parts.isEmpty()) return "/search";
        return "/search?" + String.join("&", parts);
    }

    private void append(List<String> parts, String paramName, Object value, String key, String excludeKey, String excludeValue) {
        if (value == null) return;
        String str = value.toString();
        if (!StringUtils.hasText(str)) return;
        if (key.equals(excludeKey)) return;
        parts.add(paramName + "=" + urlEncode(str));
    }

    private static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }
}
