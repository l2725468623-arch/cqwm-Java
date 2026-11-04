package com.sky.service;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.vo.DishVO;
import com.sky.result.PageResult;
import java.util.List;
public interface DishService {
    void save(DishDTO dto);
    void update(DishDTO dto);
    DishVO getById(Long id);
    PageResult pageQuery(DishPageQueryDTO query);
    void deleteByIds(List<Long> ids);
    void startOrStop(Integer status,Long id);
}
