package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Api(tags = "菜品管理接口")
@RestController
@RequestMapping("/admin/dish")
@RequiredArgsConstructor
public class DishController {

    private final DishService service;

    @ApiOperation("新增菜品及口味")
    @PostMapping
    public Result<Void> save(@Valid @RequestBody DishDTO dto) {
        service.save(dto);
        return Result.success();
    }

    @ApiOperation("修改菜品及口味")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody DishDTO dto) {
        service.update(dto);
        return Result.success();
    }

    @ApiOperation("菜品详情与编辑回显")
    @GetMapping("/{id}")
    public Result<DishVO> get(@PathVariable Long id) {
        DishVO dish = service.getById(id);
        return Result.success(dish);
    }

    @ApiOperation("菜品分页查询")
    @GetMapping("/page")
    public Result<PageResult> page(@Valid DishPageQueryDTO query) {
        PageResult page = service.pageQuery(query);
        return Result.success(page);
    }

    @ApiOperation("删除或批量删除菜品及口味")
    @DeleteMapping
    public Result<Void> delete(@RequestParam List<Long> ids) {
        service.deleteByIds(ids);
        return Result.success();
    }

    @ApiOperation("菜品起售或停售")
    @PostMapping("/status/{status}")
    public Result<Void> status(@PathVariable Integer status, @RequestParam Long id) {
        service.startOrStop(status, id);
        return Result.success();
    }
}
