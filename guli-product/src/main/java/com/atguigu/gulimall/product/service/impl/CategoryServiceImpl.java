package com.atguigu.gulimall.product.service.impl;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gulimall.common.utils.PageUtils;
import com.atguigu.gulimall.common.utils.Query;

import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree(){
        List<CategoryEntity> all = baseMapper.selectList(null);
        List<CategoryEntity> level1 = all.stream()
        .filter((d)->d.getParentCid()==0)
        .map((d)->{
            d.setChildren(getChildrens(d,all));
            return d;
        })
        .sorted((menu1,menu2)->{
            return (menu1.getSort()==null?0:menu1.getSort()) - (menu2.getSort()==null?0:menu2.getSort());
        })
        .collect(Collectors.toList());

        return level1;
    }

    @Override
    public void removeMenuByIds(List<Long> catIds){
        List<Long> allIds = new ArrayList<>(catIds);
        // TODO: 检查当前删除的菜单，是否被别的地方引用
        // List<CategoryEntity> all = baseMapper.selectList(null);
        // for (Long catId : catIds) {
        //     collectDescendantIds(catId, all, allIds);
        // }
        this.removeByIds(allIds);
    }

    // private void collectDescendantIds(Long parentId, List<CategoryEntity> all, List<Long> result){
    //     for (CategoryEntity entity : all) {
    //         if (parentId.equals(entity.getParentCid())) {
    //             result.add(entity.getCatId());
    //             collectDescendantIds(entity.getCatId(), all, result);
    //         }
    //     }
    // }

    private List<CategoryEntity> getChildrens(CategoryEntity data,List<CategoryEntity> all){

        List<CategoryEntity> children = all.stream()
        .filter(menu->{
            return menu.getParentCid()==data.getCatId();
        })
        .map(menu->{
            menu.setChildren(getChildrens(menu, all));
            return menu;
        })
        .sorted((menu1,menu2)->{
            return (menu1.getSort()==null?0:menu1.getSort()) - (menu2.getSort()==null?0:menu2.getSort());
        })
        .collect(Collectors.toList());
        return children;
    }

    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<CategoryEntity> all = baseMapper.selectList(null);
        Map<Long, CategoryEntity> map = all.stream()
                .collect(Collectors.toMap(CategoryEntity::getCatId, e -> e));

        List<Long> path = new ArrayList<>();
        Long currentId = catelogId;
        while (currentId != null && currentId != 0) {
            path.add(currentId);
            CategoryEntity entity = map.get(currentId);
            if (entity == null) break;
            currentId = entity.getParentCid();
        }
        Collections.reverse(path);
        return path.toArray(new Long[0]);
    }

}