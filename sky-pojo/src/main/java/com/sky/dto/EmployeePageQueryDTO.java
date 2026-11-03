package com.sky.dto;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import javax.validation.constraints.Size;
import lombok.Data;
@Data
public class EmployeePageQueryDTO {
    @Min(1) @Max(1000000) private int page = 1;
    @Min(1) @Max(100) private int pageSize = 10;
    @Size(max = 32) private String name;
}
