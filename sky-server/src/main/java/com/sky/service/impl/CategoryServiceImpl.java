package com.sky.service.impl;
import com.sky.constant.MessageConstant;
import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.exception.BaseException;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.CategoryMapper;
import com.sky.result.PageResult;
import com.sky.service.CategoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryMapper categoryMapper;
    public void save(CategoryDTO dto) {
        Category category=new Category(); BeanUtils.copyProperties(dto,category);
        category.setId(null); category.setStatus(0);
        categoryMapper.insert(category);
    }
    public void update(CategoryDTO dto) {
        getById(dto.getId());
        Category category=new Category();BeanUtils.copyProperties(dto,category);
        categoryMapper.update(category);
    }
    @Transactional
    public void deleteById(Long id) {
        getById(id);
        if(categoryMapper.dishCount(id)>0) throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_DISH);
        if(categoryMapper.setmealCount(id)>0) throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_SETMEAL);
        categoryMapper.deleteById(id);
    }
    public void startOrStop(Integer status,Long id) {
        if(status==null || (status!=0 && status!=1)) throw new BaseException("状态只能为0或1");
        getById(id);
        Category category=Category.builder().id(id).status(status).build();
        categoryMapper.updateStatus(category);
    }
    public Category getById(Long id) {
        if(id==null || id<1) throw new BaseException("请选择有效分类");
        Category category=categoryMapper.getById(id);
        if(category==null) throw new BaseException("分类不存在");
        return category;
    }
    public PageResult pageQuery(CategoryPageQueryDTO dto) {
        return new PageResult(categoryMapper.count(dto),categoryMapper.page(dto,(dto.getPage()-1)*dto.getPageSize()));
    }
    public List<Category> list(Integer type) {
        if(type!=null && type!=1 && type!=2) throw new BaseException("分类类型只能为1或2");
        return categoryMapper.list(type);
    }
}
