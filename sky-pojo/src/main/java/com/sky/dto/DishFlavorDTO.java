package com.sky.dto;
import javax.validation.constraints.*;
import lombok.Data;
@Data
public class DishFlavorDTO {
    @NotBlank(message="请输入口味名称") @Size(max=32) private String name;
    @NotBlank(message="请输入口味选项") @Size(max=255) private String value;
}
