package com.sky.dto;
import javax.validation.constraints.*;
import lombok.Data;
@Data
public class CategoryPageQueryDTO {
    @Min(1) @Max(1000000) private int page=1;
    @Min(1) @Max(100) private int pageSize=10;
    @Size(max=32) private String name;
    @Min(1) @Max(2) private Integer type;
}
