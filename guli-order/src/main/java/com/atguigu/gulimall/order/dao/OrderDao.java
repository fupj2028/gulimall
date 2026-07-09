package com.atguigu.gulimall.order.dao;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author fupengju
 * @email 3545485659@qq.com
 * @date 2026-07-05 16:51:38
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
	
}
