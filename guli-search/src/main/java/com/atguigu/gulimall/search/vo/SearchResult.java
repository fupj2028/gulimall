package com.atguigu.gulimall.search.vo;

import com.atguigu.gulimall.common.to.es.SkuEsModel;
import lombok.Data;

import java.util.List;

@Data
public class SearchResult {

    private List<SkuEsModel> products;

    private List<BrandVo> brands;

    private List<CatalogVo> catalogs;

    private List<AttrVo> attrs;

    private Integer pageNum;
    private Long total;
    private Integer totalPages;
    private List<Integer> pageNavs;

    private String selectedKeyword;
    private Long selectedCatalogId;
    private String selectedCatalogName;
    private List<BrandVo> selectedBrands;
    private List<SelectedChip> selectedChips;
    private Integer priceMin;
    private Integer priceMax;
    private List<SelectedAttrVo> selectedAttrs;

    public boolean containsBrandId(Long brandId) {
        return selectedBrands != null && selectedBrands.stream().anyMatch(b -> b.getBrandId().equals(brandId));
    }

    @Data
    public static class BrandVo {
        private Long brandId;
        private String brandName;
        private String brandImage;
    }

    @Data
    public static class CatalogVo {
        private Long catalogId;
        private String catalogName;
    }

    @Data
    public static class AttrVo {
        private Long attrId;
        private String attrName;
        private List<String> attrValues;
    }

    @Data
    public static class SelectedAttrVo {
        private Long attrId;
        private String attrName;
        private String attrValue;
    }

    @Data
    public static class SelectedChip {
        private String display;
        private String removeUrl;
        private String type;
    }
}
