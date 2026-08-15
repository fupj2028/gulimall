package com.atguigu.gulimall.product.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

//import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.atguigu.gulimall.product.entity.AttrEntity;
import com.atguigu.gulimall.product.entity.ProductAttrValueEntity;
import com.atguigu.gulimall.product.service.AttrService;
import com.atguigu.gulimall.product.vo.AttrVo;
import com.atguigu.gulimall.common.utils.PageUtils;
import com.atguigu.gulimall.common.utils.R;



/**
 * @author fupengju
 * @email 3545485659@qq.com
 * @date 2026-07-05 17:59:52
 */
@RestController
@RequestMapping("product/attr")
public class AttrController {
    @Autowired
    private AttrService attrService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("product:attr:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = attrService.queryPage(params);

        return R.ok().put("page", page);
    }

    @RequestMapping("/{type}/list/{catelogId}")
    //@RequiresPermissions("product:attr:list")
    public R baseList(@PathVariable("type") String type,
                      @PathVariable("catelogId") Long catelogId,
                      @RequestParam Map<String, Object> params){
        PageUtils page = attrService.queryPageByType(params, catelogId, type);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{attrId}")
    //@RequiresPermissions("product:attr:info")
    public R info(@PathVariable("attrId") Long attrId){
		AttrVo attr = attrService.getAttrInfo(attrId);

        return R.ok().put("attr", attr);
    }

    /**
     * spu列表
     */
    @RequestMapping("/base/listforspu/{spuId}")
    //@RequiresPermissions("product:attr:save")
    public R listForSpu(@PathVariable("spuId") Long spuId){
        List<ProductAttrValueEntity> attrs = attrService.getProductAttrListBySpuId(spuId);
        return R.ok().put("data", attrs);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:attr:save")
    public R save(@RequestBody AttrVo attr){
		attrService.saveAttr(attr);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update/{spuId}")
    //@RequiresPermissions("product:attr:update")
    public R update(@RequestBody List<ProductAttrValueEntity> entitis,@PathVariable("spuId") Long spuId){
		attrService.updateAttrBySpuId(entitis,spuId);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:attr:delete")
    public R delete(@RequestBody Long[] attrIds){
		attrService.removeByIds(Arrays.asList(attrIds));

        return R.ok();
    }

}
