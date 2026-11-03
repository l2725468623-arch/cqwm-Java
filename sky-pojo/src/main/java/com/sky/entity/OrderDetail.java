package com.sky.entity;
import java.io.Serializable;
import lombok.*;
import java.math.BigDecimal;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetail implements Serializable {
    private Long id;
    private String name;
    private String image;
    private Long orderId;
    private Long dishId;
    private Long setmealId;
    private String dishFlavor;
    private Integer number;
    private BigDecimal amount;
}
