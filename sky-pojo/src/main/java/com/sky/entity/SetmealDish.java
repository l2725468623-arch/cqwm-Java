package com.sky.entity;
import java.io.Serializable;
import lombok.*;
import java.math.BigDecimal;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetmealDish implements Serializable {
    private Long id;
    private Long setmealId;
    private Long dishId;
    private String name;
    private BigDecimal price;
    private Integer copies;
}
