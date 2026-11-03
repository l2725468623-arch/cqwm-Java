package com.sky.entity;
import java.io.Serializable;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Setmeal implements Serializable {
    private Long id;
    private Long categoryId;
    private String name;
    private BigDecimal price;
    private Integer status;
    private String description;
    private String image;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long createUser;
    private Long updateUser;
}
