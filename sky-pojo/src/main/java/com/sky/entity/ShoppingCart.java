package com.sky.entity;
import java.io.Serializable;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShoppingCart implements Serializable {
    private Long id;
    private String name;
    private String image;
    private Long userId;
    private Long dishId;
    private Long setmealId;
    private String dishFlavor;
    private Integer number;
    private BigDecimal amount;
    private LocalDateTime createTime;
}
