package com.atguigu.gulimall.product;

import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import java.io.File;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;



import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class GuliProductApplicationTests {

    @Autowired
    private CategoryDao categoryDao;

    @Test
    void testUpload() {
        
    }

    @Test
    void testCategoryCrud() {
        // insert
        CategoryEntity entity = new CategoryEntity();
        entity.setName("测试分类");
        entity.setCatId(0L);
        categoryDao.insert(entity);
        System.out.println("insert success, id: " + entity.getCatId());

        // select
        List<CategoryEntity> list = categoryDao.selectList(null);
        System.out.println("total categories: " + list.size());
        for (CategoryEntity c : list) {
            System.out.println("  -> " + c.getCatId() + ": " + c.getName());
        }

        // update
        CategoryEntity toUpdate = categoryDao.selectById(entity.getCatId());
        if (toUpdate != null) {
            toUpdate.setName("测试分类-已更新");
            categoryDao.updateById(toUpdate);
            System.out.println("update success");
        }

        // delete
        // categoryDao.deleteById(entity.getCatId());
        // System.out.println("delete success");
    }
}
