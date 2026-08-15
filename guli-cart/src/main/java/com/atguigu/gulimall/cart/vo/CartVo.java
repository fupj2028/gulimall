package com.atguigu.gulimall.cart.vo;

import java.math.BigDecimal;
import java.util.List;

public class CartVo {

    private Integer itemType; // 商品类型数量

    private Integer itemCount;

    private BigDecimal totalPrice;

    private List<CartItemVo> items;

    private BigDecimal reduce = new BigDecimal(0); // 折扣

    public Integer getItemType() {
        int types = 0;
        if (items != null && items.size() > 0) {
            types = items.size();
        }
        return types;
    }

    public Integer getItemCount() {

        int count = 0;
        if (items != null && items.size() > 0) {
            for (CartItemVo item : this.items) {
                count += item.getCount();
            }
        }
        return count;
    }

    public BigDecimal getTotalPrice() {

        BigDecimal total = new BigDecimal(0);
        if (items != null && items.size() > 0) {
            for (CartItemVo item : this.items) {
                total = total.add(item.getTotalPrice());
            }
        }
        total = total.subtract(reduce);

        return total;
    }

    public List<CartItemVo> getItems() {
        return items;
    }

    public void setItems(List<CartItemVo> items) {
        this.items = items;
    }

    public BigDecimal getReduce() {
        return reduce;
    }

    public void setReduce(BigDecimal reduce) {
        this.reduce = reduce;
    }

}
