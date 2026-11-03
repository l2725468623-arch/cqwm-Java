package com.sky.vo;
import java.time.LocalDateTime;
import lombok.Data;
@Data
public class EmployeeVO {
    private Long id;
    private String name;
    private String username;
    private String phone;
    private String sex;
    private Integer status;
    private LocalDateTime createTime;
}
