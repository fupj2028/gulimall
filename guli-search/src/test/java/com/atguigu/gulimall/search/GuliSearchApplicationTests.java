package com.atguigu.gulimall.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.json.JsonData;
import java.util.Objects;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
@TestMethodOrder(MethodOrderer.MethodName.class)
public class GuliSearchApplicationTests {

    @Autowired
    private ElasticsearchClient client;

    @Test
    public void contextLoads() {
    }

    @Test
    public void step01_testCreateIndex() throws Exception {
        String index = "product";
        boolean exists = client.indices().exists(ExistsRequest.of(e -> e.index(index))).value();
        if (exists) {
            client.indices().delete(DeleteIndexRequest.of(d -> d.index(index)));
        }
        CreateIndexResponse response = client.indices().create(c -> c.index(index));
        log.info("创建索引：{}", response.acknowledged());
    }

    @Test
    public void step07_testDeleteIndex() throws Exception {
        DeleteIndexResponse response = client.indices().delete(DeleteIndexRequest.of(d -> d.index("product")));
        log.info("删除索引：{}", response.acknowledged());
    }

    @Test
    public void step02_testSaveDocument() throws Exception {
        User u1 = new User();
        u1.setId(1);
        u1.setName("张三");
        u1.setAge(25);
        User u2 = new User();
        u2.setId(2);
        u2.setName("李四");
        u2.setAge(30);
        User u3 = new User();
        u3.setId(3);
        u3.setName("王五");
        u3.setAge(35);
        log.info("保存文档：{}", client.index(i -> i.index("user").id("1").document(u1)).result());
        log.info("保存文档：{}", client.index(i -> i.index("user").id("2").document(u2)).result());
        log.info("保存文档：{}", client.index(i -> i.index("user").id("3").document(u3)).result());
    }

    @Test
    public void step03_testGetDocument() throws Exception {
        GetResponse<User> response = client.get(g -> g
                        .index("user")
                        .id("1"),
                User.class
        );
        if (response.found()) {
            User user = response.source();
            log.info("查询结果：{}", user);
        } else {
            log.info("文档不存在");
        }
    }

    @Test
    public void step04_testUpdateDocument() throws Exception {
        User user = new User();
        user.setName("张三三");
        user.setAge(26);
        UpdateResponse<User> response = client.update(u -> u
                        .index("user")
                        .id("1")
                        .doc(user),
                User.class
        );
        log.info("更新文档：{}", response.id());
    }

    @Test
    public void step05_testComplexSearch() throws Exception {
        client.indices().refresh(r -> r.index("user"));

        SearchResponse<User> response = client.search(s -> s
                        .index("user")
                        .query(q -> q
                                .bool(b -> b
                                        .should(sh -> sh.match(t -> t.field("name").query("张")))
                                        .should(sh -> sh.match(t -> t.field("name").query("李")))
                                        .minimumShouldMatch("1")
                                        .filter(f -> f.range(r -> r.field("age").gte(JsonData.of(20.0)).lte(JsonData.of(35.0))))
                                )
                        )
                        .sort(o -> o.field(f -> f.field("age").order(SortOrder.Desc)))
                        .from(0)
                        .size(10)
                        .aggregations("age_stats", a -> a.stats(st -> st.field("age")))
                        .aggregations("name_terms", a -> a.terms(t -> t.field("name.keyword").size(10))),
                User.class
        );

        long total = response.hits().total().value();
        log.info("总命中数：{}", total);
        response.hits().hits().forEach(hit -> {
            String scoreStr = hit.score() != null && !Double.isNaN(hit.score())
                    ? String.valueOf(hit.score()) : "N/A";
            log.info("命中文档：{} (score: {})", hit.source(), scoreStr);
        });

        var ageStats = response.aggregations().get("age_stats").stats();
        log.info("age_stats: count={}, min={}, max={}, avg={}, sum={}",
                ageStats.count(), ageStats.min(), ageStats.max(), ageStats.avg(), ageStats.sum());

        response.aggregations().get("name_terms").sterms().buckets().array().forEach(bucket ->
                log.info("name 聚合：{} → {} 个", bucket.key().stringValue(), bucket.docCount())
        );
    }

    @Test
    public void step06_testDeleteDocument() throws Exception {
        DeleteResponse response = client.delete(d -> d
                .index("user")
                .id("1")
        );
        log.info("删除文档：{}", response.result());
    }

    @Data
    static class User {
        private Integer id;
        private String name;
        private Integer age;
    }

}
