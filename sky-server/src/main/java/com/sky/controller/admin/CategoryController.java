package com.sky.controller.admin;
import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@Api(tags="分类相关接口")
@RestController
@RequestMapping("/admin/category")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;
    @ApiOperation("新增分类") @PostMapping
    public Result<Void> save(@Valid @RequestBody CategoryDTO dto){categoryService.save(dto);return Result.success();}
    @ApiOperation("分类分页查询") @GetMapping("/page")
    public Result<PageResult> page(@Valid CategoryPageQueryDTO dto){return Result.success(categoryService.pageQuery(dto));}
    @ApiOperation("编辑分类") @PutMapping
    public Result<Void> update(@Valid @RequestBody CategoryDTO dto){categoryService.update(dto);return Result.success();}
    @ApiOperation("删除分类") @DeleteMapping
    public Result<Void> delete(@RequestParam Long id){categoryService.deleteById(id);return Result.success();}
    @ApiOperation("启用或禁用分类") @PostMapping("/status/{status}")
    public Result<Void> status(@PathVariable Integer status,@RequestParam Long id){categoryService.startOrStop(status,id);return Result.success();}
    @ApiOperation("按类型查询启用的分类") @GetMapping("/list")
    public Result<List<Category>> list(@RequestParam(required=false) Integer type){return Result.success(categoryService.list(type));}
    @ApiOperation("按ID查询分类") @GetMapping("/{id}")
    public Result<Category> get(@PathVariable Long id){return Result.success(categoryService.getById(id));}
}
