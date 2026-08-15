package com.atguigu.gulimall.product.dao;

import com.atguigu.gulimall.product.entity.SkuSaleAttrValueEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SkuSaleAttrValueDao extends BaseMapper<SkuSaleAttrValueEntity> {

    @Select("SELECT ssav.* FROM pms_sku_sale_attr_value ssav " +
            "LEFT JOIN pms_sku_info si ON ssav.sku_id = si.sku_id " +
            "WHERE si.spu_id = #{spuId} " +
            "ORDER BY ssav.attr_id, ssav.attr_sort")
    List<SkuSaleAttrValueEntity> selectSaleAttrValuesBySpuId(@Param("spuId") Long spuId);
}
