package com.sky.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.BaseException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DishServiceImpl implements DishService {

    private final DishMapper dishMapper;
    private final DishFlavorMapper flavorMapper;
    private final CategoryMapper categoryMapper;
    private final ObjectMapper json;

    @Transactional
    public void save(DishDTO dto) {
        validateCategory(dto.getCategoryId());
        List<DishFlavor> flavors = normalizeFlavors(dto.getFlavors());

        Dish dish = new Dish();
        BeanUtils.copyProperties(dto, dish);
        dish.setId(null);
        dish.setName(dto.getName().trim());
        dish.setStatus(dto.getStatus() == null ? 0 : dto.getStatus());

        // The mapper insert also triggers AutoFillAspect before executing SQL.
        dishMapper.insert(dish);
        saveFlavors(dish.getId(), flavors);
    }

    @Transactional
    public void update(DishDTO dto) {
        Dish original = requireLocked(dto.getId());
        validateCategory(dto.getCategoryId());
        List<DishFlavor> flavors = normalizeFlavors(dto.getFlavors());

        Dish dish = new Dish();
        BeanUtils.copyProperties(dto, dish);
        dish.setName(dto.getName().trim());
        if (dto.getStatus() == null) {
            dish.setStatus(original.getStatus());
        }

        dishMapper.update(dish);
        flavorMapper.deleteByDishIds(Collections.singletonList(dish.getId()));
        saveFlavors(dish.getId(), flavors);
    }

    @Transactional(readOnly = true)
    public DishVO getById(Long id) {
        if (id == null || id < 1) {
            throw new BaseException("请选择有效菜品");
        }
        DishVO dish = dishMapper.getById(id);
        if (dish == null) {
            throw new BaseException("菜品不存在");
        }
        dish.setFlavors(flavorMapper.getByDishId(id));
        return dish;
    }

    public PageResult pageQuery(DishPageQueryDTO query) {
        long total = dishMapper.count(query);
        int offset = (query.getPage() - 1) * query.getPageSize();
        List<DishVO> records = dishMapper.page(query, offset);
        return new PageResult(total, records);
    }

    @Transactional
    public void deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty() || ids.size() > 100
                || ids.stream().anyMatch(id -> id == null || id < 1)) {
            throw new BaseException("请选择1至100个有效菜品");
        }
        List<Long> unique = new ArrayList<>(new TreeSet<>(ids));
        List<Dish> dishes = dishMapper.lockByIds(unique);
        if (dishes.size() != unique.size()) {
            throw new BaseException("部分菜品不存在，请刷新后重试");
        }
        if (dishes.stream().anyMatch(d -> Integer.valueOf(1).equals(d.getStatus()))) {
            throw new BaseException("起售中的菜品不能删除，请先停售");
        }
        if (dishMapper.countSetmealLinks(unique) > 0) {
            throw new BaseException("菜品关联了套餐，不能删除");
        }

        flavorMapper.deleteByDishIds(unique);
        dishMapper.deleteByIds(unique);
    }

    @Transactional
    public void startOrStop(Integer status, Long id) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BaseException("状态只能为0或1");
        }
        Dish original = requireLocked(id);
        if (status == 1) {
            validateCategory(original.getCategoryId());
        }
        Dish dish = Dish.builder().id(id).status(status).build();
        dishMapper.updateStatus(dish);
    }

    private Dish requireLocked(Long id) {
        if (id == null || id < 1) {
            throw new BaseException("请选择有效菜品");
        }
        Dish dish = dishMapper.getForUpdate(id);
        if (dish == null) {
            throw new BaseException("菜品不存在");
        }
        return dish;
    }

    private void validateCategory(Long id) {
        Category category = id == null ? null : categoryMapper.getById(id);
        if (category == null || !Integer.valueOf(1).equals(category.getType())) {
            throw new BaseException("请选择菜品分类");
        }
        if (!Integer.valueOf(1).equals(category.getStatus())) {
            throw new BaseException("所选分类已禁用，请先启用分类");
        }
    }

    private List<DishFlavor> normalizeFlavors(List<DishFlavorDTO> input) {
        List<DishFlavor> result = new ArrayList<>();
        if (input == null) {
            return result;
        }
        Set<String> names = new HashSet<>();
        for (DishFlavorDTO dto : input) {
            String name = dto.getName().trim();
            if (!names.add(name)) {
                throw new BaseException("口味名称不能重复");
            }
            try {
                JsonNode array = json.readTree(dto.getValue());
                if (array == null || !array.isArray() || array.size() < 1 || array.size() > 20) {
                    throw new IllegalArgumentException();
                }
                List<String> options = new ArrayList<>();
                Set<String> seen = new HashSet<>();
                for (JsonNode value : array) {
                    if (!value.isTextual() || value.asText().trim().isEmpty()
                            || value.asText().trim().length() > 32) {
                        throw new IllegalArgumentException();
                    }
                    String option = value.asText().trim();
                    if (!seen.add(option)) {
                        throw new IllegalArgumentException();
                    }
                    options.add(option);
                }
                String canonical = json.writeValueAsString(options);
                if (canonical.length() > 255) {
                    throw new IllegalArgumentException();
                }
                DishFlavor flavor = DishFlavor.builder().name(name).value(canonical).build();
                result.add(flavor);
            } catch (Exception ex) {
                throw new BaseException("口味选项应为不重复的文字数组，每组最多20项");
            }
        }
        return result;
    }

    private void saveFlavors(Long id, List<DishFlavor> flavors) {
        if (flavors.isEmpty()) {
            return;
        }
        flavors.forEach(flavor -> flavor.setDishId(id));
        flavorMapper.insertBatch(flavors);
    }
}
