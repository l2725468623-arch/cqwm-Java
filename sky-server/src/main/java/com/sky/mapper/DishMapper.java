package com.sky.mapper;
import com.sky.annotation.AutoFill;
import com.sky.enumeration.OperationType;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.vo.DishVO;
import java.util.List;
import org.apache.ibatis.annotations.Param;
public interface DishMapper {
    @AutoFill(OperationType.INSERT) int insert(Dish dish);
    @AutoFill(OperationType.UPDATE) int update(Dish dish);
    @AutoFill(OperationType.UPDATE) int updateStatus(Dish dish);
    DishVO getById(Long id);
    Dish getForUpdate(Long id);
    long count(DishPageQueryDTO query);
    List<DishVO> page(@Param("query") DishPageQueryDTO query,@Param("offset") int offset);
    List<Dish> lockByIds(@Param("ids") List<Long> ids);
    long countSetmealLinks(@Param("ids") List<Long> ids);
    int deleteByIds(@Param("ids") List<Long> ids);
}
