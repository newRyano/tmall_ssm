package com.ryan.tmall.service;

import com.ryan.tmall.pojo.Category;

import java.util.List;

/**
 * 分类管理功能 接口
 */
public interface CategoryService{

    //查询分类信息
    List<Category> list();

    //增加分类商品
    void add(Category category);

    //删除分类商品
    void delete(int id);

    //通过 ID 获取 Category 对象
    Category get(int id);

    //修改商品信息
    void update(Category category);
}
