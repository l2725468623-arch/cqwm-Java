package com.sky.mapper;
import com.sky.entity.DishFlavor;
import java.util.List;
import org.apache.ibatis.annotations.Param;
public interface DishFlavorMapper {
    int insertBatch(@Param("flavors") List<DishFlavor> flavors);
    List<DishFlavor> getByDishId(Long dishId);
    int deleteByDishIds(@Param("ids") List<Long> ids);
}
