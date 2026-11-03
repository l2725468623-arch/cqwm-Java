package com.sky.service;
import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.result.PageResult;
import java.util.List;
public interface CategoryService {
    void save(CategoryDTO dto);
    void update(CategoryDTO dto);
    void deleteById(Long id);
    void startOrStop(Integer status,Long id);
    Category getById(Long id);
    PageResult pageQuery(CategoryPageQueryDTO dto);
    List<Category> list(Integer type);
}
