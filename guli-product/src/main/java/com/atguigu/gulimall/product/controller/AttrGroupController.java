package com.atguigu.gulimall.product.controller;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

//import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.atguigu.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.atguigu.gulimall.product.entity.AttrEntity;
import com.atguigu.gulimall.product.entity.AttrGroupEntity;
import com.atguigu.gulimall.product.service.AttrGroupService;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.AttrGroupWithAttrsVo;
import com.atguigu.gulimall.common.utils.PageUtils;
import com.atguigu.gulimall.common.utils.R;



/**
 * 属性分组
 *
 * @author fupengju
 * @email 3545485659@qq.com
 * @date 2026-07-05 17:59:52
 */
@RestController
@RequestMapping("product/attrgroup")
public class AttrGroupController {
    @Autowired
    private AttrGroupService attrGroupService;

    @Autowired
    private CategoryService categoryService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("product:attrgroup:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = attrGroupService.queryPage(params);

        return R.ok().put("page", page);
    }

    /**
     * 列表
     */
    @RequestMapping("/list/{catelogId}")
    //@RequiresPermissions("product:attrgroup:list")
    public R list(@PathVariable("catelogId") Long catelogId, @RequestParam Map<String, Object> params){
        // PageUtils page = attrGroupService.queryPage(params);

        PageUtils page = attrGroupService.queryPage(params,catelogId);


        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{attrGroupId}")
    //@RequiresPermissions("product:attrgroup:info")
    public R info(@PathVariable("attrGroupId") Long attrGroupId){
		AttrGroupEntity attrGroup = attrGroupService.getById(attrGroupId);
        attrGroup.setCatelogPath(categoryService.findCatelogPath(attrGroup.getCatelogId()));

        return R.ok().put("attrGroup", attrGroup);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:attrgroup:save")
    public R save(@RequestBody AttrGroupEntity attrGroup){
		attrGroupService.save(attrGroup);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("product:attrgroup:update")
    public R update(@RequestBody AttrGroupEntity attrGroup){
		attrGroupService.updateById(attrGroup);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:attrgroup:delete")
    public R delete(@RequestBody Long[] attrGroupIds){
		attrGroupService.removeByIds(Arrays.asList(attrGroupIds));

        return R.ok();
    }

    /**
     * 查询分组已关联的属性
     */
    @GetMapping("/{attrGroupId}/attr/relation")
    public R attrRelation(@PathVariable("attrGroupId") Long attrGroupId) {
        List<AttrEntity> list = attrGroupService.getRelationAttrs(attrGroupId);
        return R.ok().put("data", list);
    }

    /**
     * 查询分组未关联的属性（分页）
     */
    @GetMapping("/{attrGroupId}/noattr/relation")
    public R noAttrRelation(@RequestParam Map<String, Object> params,
                            @PathVariable("attrGroupId") Long attrGroupId) {
        PageUtils page = attrGroupService.getNoRelationPage(params, attrGroupId);
        return R.ok().put("page", page);
    }

    /**
     * 批量新增关联
     */
    @PostMapping("/attr/relation")
    public R addRelation(@RequestBody Collection<AttrAttrgroupRelationEntity> relations) {
        attrGroupService.saveRelations(relations);
        return R.ok();
    }

    /**
     * 批量删除关联
     */
    @PostMapping("/attr/relation/delete")
    public R deleteRelation(@RequestBody Collection<AttrAttrgroupRelationEntity> relations) {
        attrGroupService.removeRelations(relations);
        return R.ok();
    }

    /**
     * 获取分类下所有分组及关联的属性
     */
    @GetMapping("/{catelogId}/withattr")
    public R withAttr(@PathVariable("catelogId") Long catelogId) {
        List<AttrGroupWithAttrsVo> list = attrGroupService.getAttrGroupWithAttrsByCatelogId(catelogId);
        return R.ok().put("data", list);
    }

}
