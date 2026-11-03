package com.sky.mapper;
import com.sky.entity.Category;
import com.sky.dto.CategoryPageQueryDTO;
import java.util.List;
import org.apache.ibatis.annotations.Param;
public interface CategoryMapper {
    int insert(Category category);
    Category getById(Long id);
    int update(Category category);
    int updateStatus(Category category);
    int deleteById(Long id);
    long count(CategoryPageQueryDTO dto);
    List<Category> page(@Param("query") CategoryPageQueryDTO dto,@Param("offset") int offset);
    List<Category> list(Integer type);
    long dishCount(Long id);
    long setmealCount(Long id);
}
