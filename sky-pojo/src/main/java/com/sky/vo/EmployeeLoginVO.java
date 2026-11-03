package com.sky.vo;
import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class EmployeeLoginVO {
    private Long id;
    private String username;
    private String name;
    private String token;
}
