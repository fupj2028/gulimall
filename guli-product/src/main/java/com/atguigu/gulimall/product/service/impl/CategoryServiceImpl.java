package com.atguigu.gulimall.product.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gulimall.common.utils.PageUtils;
import com.atguigu.gulimall.common.utils.Query;

import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.atguigu.gulimall.product.service.CategoryService;

import com.atguigu.gulimall.product.vo.Catalog2Vo;
import com.atguigu.gulimall.product.vo.Catalog3Vo;

import lombok.RequiredArgsConstructor;


@Service("categoryService")
@RequiredArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    private final CategoryBrandRelationService categoryBrandRelationService;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    @CacheEvict(value = "category", allEntries = true)
    @Override
    public boolean save(CategoryEntity entity) {
        return super.save(entity);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @CacheEvict(value = "category", allEntries = true)
    @Override
    public boolean updateById(CategoryEntity entity) {
        boolean result = super.updateById(entity);
        if (result && entity.getName() != null) {
            categoryBrandRelationService.updateCatelogName(entity.getCatId(), entity.getName());
        }
        return result;
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
    public List<CategoryEntity> listLevel1() {
        return baseMapper.selectList(new QueryWrapper<CategoryEntity>()
                .select("cat_id", "name")
                .eq("parent_cid", 0));
    }

    @CacheEvict(value = "category", allEntries = true)
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

    // @Cacheable(value = "category", key = "'catalogJson'", sync = true)
    @Override
    public Map<String, List<Catalog2Vo>> getCatalogJson() {
        String cacheKey = "category::catalogJson";

        String json = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.hasText(json)) {
            try {
                return objectMapper.readValue(json, new TypeReference<Map<String, List<Catalog2Vo>>>() {});
            } catch (JsonProcessingException ignored) {}
        }

        RLock lock = redissonClient.getLock("lock:category:catalogJson");
        lock.lock(30, TimeUnit.SECONDS);
        try {
            json = stringRedisTemplate.opsForValue().get(cacheKey);
            if (StringUtils.hasText(json)) {
                try {
                    return objectMapper.readValue(json, new TypeReference<Map<String, List<Catalog2Vo>>>() {});
                } catch (JsonProcessingException ignored) {}
            }

            Map<String, List<Catalog2Vo>> catalog = buildCatalogJsonFromDB();

            long ttl = 3600 + ThreadLocalRandom.current().nextLong(0, 1800);
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(catalog), ttl, TimeUnit.SECONDS);

            return catalog;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    private Map<String, List<Catalog2Vo>> buildCatalogJsonFromDB() {
        List<CategoryEntity> all = baseMapper.selectList(
                new QueryWrapper<CategoryEntity>().select("cat_id", "name", "parent_cid"));
        Map<Long, List<CategoryEntity>> childrenMap = all.stream()
                .collect(Collectors.groupingBy(CategoryEntity::getParentCid));

        Map<String, List<Catalog2Vo>> catalog = new HashMap<>();
        for (CategoryEntity level1 : childrenMap.getOrDefault(0L, Collections.emptyList())) {
            List<Catalog2Vo> level2List = new ArrayList<>();
            List<CategoryEntity> level2s = childrenMap.get(level1.getCatId());
            if (level2s != null) {
                for (CategoryEntity level2 : level2s) {
                    List<Catalog3Vo> level3List = new ArrayList<>();
                    List<CategoryEntity> level3s = childrenMap.get(level2.getCatId());
                    if (level3s != null) {
                        for (CategoryEntity level3 : level3s) {
                            level3List.add(new Catalog3Vo(level3.getCatId(), level3.getName()));
                        }
                    }
                    level2List.add(new Catalog2Vo(level2.getName(), level3List));
                }
            }
            catalog.put(String.valueOf(level1.getCatId()), level2List);
        }
        return catalog;
    }

}