package com.sky.entity;
import java.io.Serializable;
import lombok.*;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DishFlavor implements Serializable {
    private Long id;
    private Long dishId;
    private String name;
    private String value;
}
