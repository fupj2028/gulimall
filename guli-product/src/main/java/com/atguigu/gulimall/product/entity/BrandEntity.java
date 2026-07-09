package com.atguigu.gulimall.product.entity;

import com.atguigu.gulimall.common.valid.AddGroup;
import com.atguigu.gulimall.common.valid.ListValue;
import com.atguigu.gulimall.common.valid.UpdateGroup;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
// import java.util.Date;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 品牌
 * 
 * @author fupengju
 * @email 3545485659@qq.com
 * @date 2026-07-05 17:59:52
 */
@Data
@TableName("pms_brand")
public class BrandEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 品牌id
	 */
	@NotNull(message = "品牌id不能为空", groups = {UpdateGroup.class})
	@TableId
	private Long brandId;
	/**
	 * 品牌名
	 */
	@NotBlank(message = "品牌名不能为空", groups = {AddGroup.class,UpdateGroup.class})
	private String name;
	/**
	 * 品牌logo地址
	 */
	@NotBlank(message = "请上传品牌logo", groups = {AddGroup.class})
	@Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}/[a-f0-9]+\\.\\w+$", message = "logo格式不正确", groups = {AddGroup.class, UpdateGroup.class})
	private String logo;
	/**
	 * 介绍
	 */
	@NotBlank(message = "介绍不能为空", groups = {AddGroup.class})
	private String descript;
	/**
	 * 显示状态[0-不显示；1-显示]
	 */
	@NotNull(message = "请选择显示状态", groups = {AddGroup.class})
	@ListValue(vals = {0, 1}, message = "显示状态必须为0或1", groups = {AddGroup.class, UpdateGroup.class})
	private Integer showStatus;
	/**
	 * 检索首字母
	 */
	@NotBlank(message = "检索首字母不能为空", groups = {AddGroup.class})
	@Pattern(regexp = "^[A-Za-z]$", message = "检索首字母必须为英文字母", groups = {AddGroup.class, UpdateGroup.class})
	private String firstLetter;
	/**
	 * 排序
	 */
	@NotNull(message = "排序不能为空", groups = {AddGroup.class})
	@Min(value = 0, message = "排序必须为非负整数", groups = {AddGroup.class, UpdateGroup.class})
	private Integer sort;

}
