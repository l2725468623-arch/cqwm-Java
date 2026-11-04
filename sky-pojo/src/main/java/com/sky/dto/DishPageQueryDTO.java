package com.sky.dto;
import javax.validation.constraints.*;
import lombok.Data;
@Data
public class DishPageQueryDTO {
    @Min(1) @Max(1000000) private int page=1;
    @Min(1) @Max(100) private int pageSize=10;
    @Size(max=32) private String name;
    @Positive private Long categoryId;
    @Min(0) @Max(1) private Integer status;
}
