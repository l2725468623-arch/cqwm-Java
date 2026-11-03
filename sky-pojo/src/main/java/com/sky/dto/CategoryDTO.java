package com.sky.dto;
import javax.validation.constraints.*;
import lombok.Data;
@Data
public class CategoryDTO {
    @Positive private Long id;
    @NotNull(message="请选择分类类型") @Min(1) @Max(2) private Integer type;
    @NotBlank(message="请输入分类名称") @Size(max=32) private String name;
    @NotNull(message="请输入排序") @Min(0) @Max(99999) private Integer sort;
}
