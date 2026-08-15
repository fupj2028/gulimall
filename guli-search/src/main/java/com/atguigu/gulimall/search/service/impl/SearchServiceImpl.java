package com.atguigu.gulimall.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.NestedAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.atguigu.gulimall.common.to.es.SkuEsModel;
import com.atguigu.gulimall.common.utils.R;
import com.atguigu.gulimall.search.feign.ThirdPartyFeignService;
import com.atguigu.gulimall.search.service.SearchService;
import com.atguigu.gulimall.search.vo.SearchParam;
import com.atguigu.gulimall.search.vo.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final ElasticsearchClient client;
    private final ThirdPartyFeignService thirdPartyFeignService;

    private static final int PAGE_SIZE = 20;

    @Override
    public SearchResult search(SearchParam param) {
        try {
            int pageNum = Math.max(1, param.getPageNum() == null ? 1 : param.getPageNum());

            List<Query> filterQueries = buildFilters(param);

            SearchResponse<SkuEsModel> response = client.search(s -> {
                s.index("product")
                        .query(q -> q.bool(b -> {
                            if (StringUtils.hasText(param.getKeyword())) {
                                b.must(m -> m.matchPhrase(t -> t.field("skuTitle").query(param.getKeyword())));
                            }
                            if (!filterQueries.isEmpty()) {
                                for (Query filter : filterQueries) {
                                    b.filter(filter);
                                }
                            }
                            return b;
                        }))
                        .from((pageNum - 1) * PAGE_SIZE)
                        .size(PAGE_SIZE);

                String sort = param.getSort();
                if (StringUtils.hasText(sort)) {
                    String[] parts = sort.split("_");
                    if (parts.length == 2) {
                        String sortField = switch (parts[0]) {
                            case "saleCount" -> "saleCount";
                            case "skuPrice" -> "skuPrice";
                            case "hotScore" -> "hotScore";
                            default -> null;
                        };
                        if (sortField != null) {
                            s.sort(o -> o.field(f -> f
                                    .field(sortField)
                                    .order("asc".equals(parts[1]) ? SortOrder.Asc : SortOrder.Desc)
                            ));
                        }
                    }
                }

                s.aggregations("brand_agg", a -> a
                        .terms(t -> t.field("brandId").size(50))
                        .aggregations("brand_info", a2 -> a2
                                .topHits(th -> th.size(1).source(sc -> sc.filter(f -> f.includes("brandName", "brandImage"))))
                        )
                );
                s.aggregations("catalog_agg", a -> a
                        .terms(t -> t.field("catelogId").size(50))
                        .aggregations("catalog_info", a2 -> a2
                                .topHits(th -> th.size(1).source(sc -> sc.filter(f -> f.includes("catalogName"))))
                        )
                );
                s.aggregations("attrs_agg", a -> a
                        .nested(n -> n.path("attrs"))
                        .aggregations("attr_id_agg", a2 -> a2
                                .terms(t -> t.field("attrs.attrId").size(50))
                                .aggregations("attr_name", a3 -> a3
                                        .terms(t -> t.field("attrs.attrName").size(1))
                                )
                                .aggregations("attr_value", a3 -> a3
                                        .terms(t -> t.field("attrs.attrValue").size(50))
                                )
                        )
                );

                return s;
            }, SkuEsModel.class);

            return buildResult(response, pageNum, param);
        } catch (Exception e) {
            log.error("搜索异常", e);
            return new SearchResult();
        }
    }

    private List<Query> buildFilters(SearchParam param) {
        List<Query> filters = new ArrayList<>();

        if (param.getCatelogId() != null) {
            filters.add(Query.of(q -> q.term(t -> t.field("catelogId").value(param.getCatelogId()))));
        }

        if (param.getBrandId() != null && !param.getBrandId().isEmpty()) {
            List<FieldValue> values = param.getBrandId().stream()
                    .map(FieldValue::of)
                    .toList();
            filters.add(Query.of(q -> q.terms(t -> t.field("brandId").terms(tm -> tm.value(values)))));
        }

        if (param.getAttrs() != null && !param.getAttrs().isEmpty()) {
            Map<Long, List<String>> attrMap = param.getAttrs().stream()
                    .map(this::parseAttrEntry)
                    .filter(e -> e != null)
                    .collect(Collectors.groupingBy(
                            Map.Entry::getKey,
                            Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                    ));

            for (Map.Entry<Long, List<String>> entry : attrMap.entrySet()) {
                Long attrId = entry.getKey();
                List<FieldValue> attrValues = entry.getValue().stream()
                        .map(FieldValue::of)
                        .toList();

                filters.add(Query.of(q -> q.nested(n -> n
                        .path("attrs")
                        .query(nq -> nq.bool(b -> {
                            b.must(m -> m.term(t -> t.field("attrs.attrId").value(attrId)));
                            b.must(m -> m.terms(t -> t.field("attrs.attrValue").terms(tm -> tm.value(attrValues))));
                            return b;
                        }))
                )));
            }
        }

        if (StringUtils.hasText(param.getPrice())) {
            String[] range = param.getPrice().split("_");
            if (range.length == 2) {
                String minStr = range[0];
                String maxStr = range[1];
                if (StringUtils.hasText(minStr) || StringUtils.hasText(maxStr)) {
                    filters.add(Query.of(q -> q
                            .range(r -> {
                                r.field("skuPrice");
                                if (StringUtils.hasText(minStr)) {
                                    r.gte(JsonData.of(Double.parseDouble(minStr)));
                                }
                                if (StringUtils.hasText(maxStr)) {
                                    r.lte(JsonData.of(Double.parseDouble(maxStr)));
                                }
                                return r;
                            })
                    ));
                }
            }
        }

        if (param.getHasStock() != null && param.getHasStock() == 1) {
            filters.add(Query.of(q -> q.term(t -> t.field("hasStock").value(true))));
        }

        return filters;
    }

    private Map.Entry<Long, String> parseAttrEntry(String attr) {
        String[] firstSplit = attr.split(":");
        if (firstSplit.length != 2) return null;
        String[] secondSplit = firstSplit[0].split("_");
        if (secondSplit.length != 2) return null;
        try {
            Long attrId = Long.parseLong(secondSplit[0]);
            return new AbstractMap.SimpleEntry<>(attrId, firstSplit[1]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private SearchResult buildResult(SearchResponse<SkuEsModel> response, int pageNum, SearchParam param) {
        SearchResult result = new SearchResult();

        List<SkuEsModel> products = response.hits().hits().stream()
                .map(Hit::source)
                .collect(Collectors.toList());
        resolveImages(products);
        result.setProducts(products);

        Map<String, Aggregate> aggs = response.aggregations();
        if (aggs != null) {
            result.setBrands(parseBrandAgg(aggs.get("brand_agg")));
            result.setCatalogs(parseCatalogAgg(aggs.get("catalog_agg")));
            result.setAttrs(parseAttrAgg(aggs.get("attrs_agg")));
        }

        long total = response.hits().total() != null ? response.hits().total().value() : 0;
        result.setTotal(total);
        result.setPageNum(pageNum);
        int totalPages = (int) Math.ceil((double) total / PAGE_SIZE);
        result.setTotalPages(totalPages);

        List<Integer> pageNavs = new ArrayList<>();
        for (int i = Math.max(1, pageNum - 2); i <= Math.min(totalPages, pageNum + 2); i++) {
            pageNavs.add(i);
        }
        result.setPageNavs(pageNavs);

        result.setSelectedKeyword(param.getKeyword() != null ? param.getKeyword() : "");
        result.setSelectedCatalogId(param.getCatelogId() != null ? param.getCatelogId() : 0L);
        List<Long> rawBrandIds = param.getBrandId();
        List<SearchResult.BrandVo> selectedBrands = rawBrandIds != null && result.getBrands() != null ?
            result.getBrands().stream()
                .filter(b -> rawBrandIds.contains(b.getBrandId()))
                .toList()
            : new ArrayList<>();
        result.setSelectedBrands(selectedBrands);
        if (StringUtils.hasText(param.getPrice())) {
            String[] range = param.getPrice().split("_");
            if (range.length == 2) {
                try {
                    if (StringUtils.hasText(range[0]))
                        result.setPriceMin(Integer.parseInt(range[0]));
                    if (StringUtils.hasText(range[1]))
                        result.setPriceMax(Integer.parseInt(range[1]));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (result.getPriceMin() == null) result.setPriceMin(-1);
        if (result.getPriceMax() == null) result.setPriceMax(-1);
        result.setSelectedAttrs(parseSelectedAttrs(param.getAttrs()));
        result.setSelectedCatalogName(
            result.getCatalogs() != null ?
                result.getCatalogs().stream()
                    .filter(c -> c.getCatalogId().equals(result.getSelectedCatalogId()))
                    .map(SearchResult.CatalogVo::getCatalogName)
                    .findFirst()
                    .orElse("")
                : ""
        );

        List<SearchResult.SelectedChip> chips = new ArrayList<>();

        if (StringUtils.hasText(result.getSelectedKeyword())) {
            SearchResult.SelectedChip chip = new SearchResult.SelectedChip();
            chip.setDisplay(result.getSelectedKeyword());
            chip.setRemoveUrl(param.buildUrlExcluding("keyword", result.getSelectedKeyword()));
            chip.setType("keyword");
            chips.add(chip);
        }

        if (result.getSelectedCatalogId() > 0) {
            SearchResult.SelectedChip chip = new SearchResult.SelectedChip();
            chip.setDisplay(result.getSelectedCatalogName());
            chip.setRemoveUrl(param.buildUrlExcluding("catelogId", null));
            chip.setType("catalog");
            chips.add(chip);
        }

        if (result.getSelectedBrands() != null) {
            for (SearchResult.BrandVo brand : result.getSelectedBrands()) {
                SearchResult.SelectedChip chip = new SearchResult.SelectedChip();
                chip.setDisplay(brand.getBrandName());
                chip.setRemoveUrl(param.buildUrlExcluding("brandId", brand.getBrandId().toString()));
                chip.setType("brand");
                chips.add(chip);
            }
        }

        if (result.getPriceMin() >= 0 || result.getPriceMax() >= 0) {
            SearchResult.SelectedChip chip = new SearchResult.SelectedChip();
            chip.setDisplay("价格 " + (result.getPriceMin() >= 0 ? result.getPriceMin() : "0")
                + " - " + (result.getPriceMax() >= 0 ? result.getPriceMax() : "不限"));
            chip.setRemoveUrl(param.buildUrlExcluding("price", null));
            chip.setType("price");
            chips.add(chip);
        }

        if (result.getSelectedAttrs() != null) {
            for (SearchResult.SelectedAttrVo attr : result.getSelectedAttrs()) {
                SearchResult.SelectedChip chip = new SearchResult.SelectedChip();
                chip.setDisplay(attr.getAttrName() + ": " + attr.getAttrValue());
                chip.setRemoveUrl(param.buildUrlExcluding("attrs",
                    attr.getAttrId() + "_" + attr.getAttrName() + ":" + attr.getAttrValue()));
                chip.setType("attr");
                chips.add(chip);
            }
        }

        result.setSelectedChips(chips);

        return result;
    }

    private void resolveImages(List<SkuEsModel> products) {
        for (SkuEsModel product : products) {
            product.setSkuImage(resolveImageUrl(product.getSkuImage()));
            product.setBrandImage(resolveImageUrl(product.getBrandImage()));
        }
    }

    private String resolveImageUrl(String image) {
        if (!StringUtils.hasText(image)) return image;
        if (image.startsWith("http://") || image.startsWith("https://")) return image;
        try {
            R r = thirdPartyFeignService.access(image);
            Object code = r.get("code");
            if (code instanceof Integer && (Integer) code == 0) {
                Object data = r.get("data");
                if (data instanceof String) return (String) data;
            }
        } catch (Exception e) {
            log.warn("图片URL解析失败: key={}", image, e);
        }
        return image;
    }

    private List<SearchResult.BrandVo> parseBrandAgg(Aggregate agg) {
        List<SearchResult.BrandVo> brands = new ArrayList<>();
        if (agg == null) return brands;
        LongTermsAggregate brandAgg = agg.lterms();
        for (LongTermsBucket bucket : brandAgg.buckets().array()) {
            SearchResult.BrandVo vo = new SearchResult.BrandVo();
            vo.setBrandId(bucket.key());
            Aggregate infoAgg = bucket.aggregations().get("brand_info");
            if (infoAgg != null && infoAgg.topHits() != null && !infoAgg.topHits().hits().hits().isEmpty()) {
                var hit = infoAgg.topHits().hits().hits().get(0);
                try {
                    Map<String, Object> map = hit.source().to(Map.class);
                    vo.setBrandName((String) map.get("brandName"));
                    vo.setBrandImage(resolveImageUrl((String) map.get("brandImage")));
                } catch (Exception e) {
                    log.warn("解析品牌聚合失败", e);
                }
            }
            brands.add(vo);
        }
        return brands;
    }

    private List<SearchResult.CatalogVo> parseCatalogAgg(Aggregate agg) {
        List<SearchResult.CatalogVo> catalogs = new ArrayList<>();
        if (agg == null) return catalogs;
        LongTermsAggregate catalogAgg = agg.lterms();
        for (LongTermsBucket bucket : catalogAgg.buckets().array()) {
            SearchResult.CatalogVo vo = new SearchResult.CatalogVo();
            vo.setCatalogId(bucket.key());
            Aggregate infoAgg = bucket.aggregations().get("catalog_info");
            if (infoAgg != null && infoAgg.topHits() != null && !infoAgg.topHits().hits().hits().isEmpty()) {
                var hit = infoAgg.topHits().hits().hits().get(0);
                try {
                    Map<String, Object> map = hit.source().to(Map.class);
                    vo.setCatalogName((String) map.get("catalogName"));
                } catch (Exception e) {
                    log.warn("解析分类聚合失败", e);
                }
            }
            catalogs.add(vo);
        }
        return catalogs;
    }

    private List<SearchResult.AttrVo> parseAttrAgg(Aggregate agg) {
        List<SearchResult.AttrVo> attrs = new ArrayList<>();
        if (agg == null) return attrs;
        NestedAggregate nestedAgg = agg.nested();
        Aggregate attrIdAgg = nestedAgg.aggregations().get("attr_id_agg");
        if (attrIdAgg == null) return attrs;
        LongTermsAggregate termsAgg = attrIdAgg.lterms();
        for (LongTermsBucket bucket : termsAgg.buckets().array()) {
            SearchResult.AttrVo vo = new SearchResult.AttrVo();
            vo.setAttrId(bucket.key());
            Aggregate nameAgg = bucket.aggregations().get("attr_name");
            if (nameAgg != null && nameAgg.sterms() != null) {
                StringTermsAggregate nameTerms = nameAgg.sterms();
                if (!nameTerms.buckets().array().isEmpty()) {
                    vo.setAttrName(nameTerms.buckets().array().get(0).key().stringValue());
                }
            }
            Aggregate valueAgg = bucket.aggregations().get("attr_value");
            if (valueAgg != null) {
                StringTermsAggregate valueTerms = valueAgg.sterms();
                List<String> values = valueTerms.buckets().array().stream()
                        .map(StringTermsBucket::key)
                        .map(FieldValue::stringValue)
                        .toList();
                vo.setAttrValues(values);
            }
            attrs.add(vo);
        }
        return attrs;
    }

    private List<SearchResult.SelectedAttrVo> parseSelectedAttrs(List<String> attrs) {
        List<SearchResult.SelectedAttrVo> selected = new ArrayList<>();
        if (attrs == null) return selected;
        for (String attr : attrs) {
            String[] firstSplit = attr.split(":");
            if (firstSplit.length != 2) continue;
            String[] secondSplit = firstSplit[0].split("_");
            if (secondSplit.length != 2) continue;
            try {
                SearchResult.SelectedAttrVo vo = new SearchResult.SelectedAttrVo();
                vo.setAttrId(Long.parseLong(secondSplit[0]));
                vo.setAttrName(secondSplit[1]);
                vo.setAttrValue(firstSplit[1]);
                selected.add(vo);
            } catch (NumberFormatException ignored) {
            }
        }
        return selected;
    }
}
