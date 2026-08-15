package com.atguigu.gulimall.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import com.atguigu.gulimall.common.Exception.ErrorCode;
import com.atguigu.gulimall.common.to.es.SkuEsModel;
import com.atguigu.gulimall.common.utils.R;
import com.atguigu.gulimall.search.service.ProductUpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductUpServiceImpl implements ProductUpService {

    private final ElasticsearchClient client;

    @Override
    public R productUp(List<SkuEsModel> skuEsModels) {
        try {
            String index = "product";
            boolean exists = client.indices().exists(ExistsRequest.of(e -> e.index(index))).value();
            if (!exists) {
                CreateIndexResponse create = client.indices().create(c -> c
                        .index(index)
                        .settings(IndexSettings.of(s -> s
                                .numberOfShards("1")
                                .numberOfReplicas("0")
                        ))
                        .mappings(m -> m
                                .properties("skuId", p -> p.long_(l -> l))
                                .properties("spuId", p -> p.long_(l -> l))
                                .properties("skuTitle", p -> p.text(t -> t))
                                .properties("skuPrice", p -> p.double_(d -> d))
                                .properties("skuImage", p -> p.keyword(k -> k))
                                .properties("saleCount", p -> p.long_(l -> l))
                                .properties("hasStock", p -> p.boolean_(b -> b))
                                .properties("hotScore", p -> p.long_(l -> l))
                                .properties("brandId", p -> p.long_(l -> l))
                                .properties("catelogId", p -> p.long_(l -> l))
                                .properties("brandName", p -> p.keyword(k -> k))
                                .properties("brandImage", p -> p.keyword(k -> k))
                                .properties("catalogName", p -> p.keyword(k -> k))
                                .properties("attrs", p -> p.nested(n -> n
                                        .properties("attrId", p2 -> p2.long_(l -> l))
                                        .properties("attrName", p2 -> p2.keyword(k -> k))
                                        .properties("attrValue", p2 -> p2.keyword(k -> k))
                                ))
                        )
                );
                log.info("创建ES product索引：{}", create.acknowledged());
            }

            BulkResponse bulk = client.bulk(BulkRequest.of(b -> b
                    .index(index)
                    .operations(skuEsModels.stream()
                            .map(model -> co.elastic.clients.elasticsearch.core.bulk.BulkOperation.of(op -> op
                                    .index(idx -> idx
                                            .id(String.valueOf(model.getSkuId()))
                                            .document(model)
                                    )
                            ))
                            .toList()
                    )
            ));

            if (bulk.errors()) {
                bulk.items().stream()
                        .filter(item -> item.error() != null)
                        .forEach(item ->
                                log.error("索引失败: id={}, error={}", item.id(), item.error().reason())
                        );
                return R.error(ErrorCode.PRODUCT_UP_PARTIAL_ERROR.getCode(), "部分SKU索引失败");
            }

            log.info("上架成功，共 {} 个SKU", skuEsModels.size());
            return R.ok();
        } catch (Exception e) {
            log.error("上架到ES异常", e);
            return R.error(ErrorCode.PRODUCT_UP_ERROR.getCode(), "上架失败");
        }
    }
}
